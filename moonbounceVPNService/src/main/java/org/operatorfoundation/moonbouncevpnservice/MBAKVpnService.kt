package org.operatorfoundation.moonbouncevpnservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.IpPrefix
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import org.operatorfoundation.shadow.ShadowConfig
import org.operatorfoundation.shadow.ShadowConnection
import org.operatorfoundation.transmission.Connection
import org.operatorfoundation.transmission.ConnectionType
import org.operatorfoundation.transmission.TransmissionConnection
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.time.TimeSource

const val SERVER_PORT = "ServerPort"
const val SERVER_IP = "ServerIP"
const val SERVER_PUBLIC_KEY = "ServerPublicKey"
const val DISALLOWED_APPS = "DisallowedApp"
const val EXCLUDE_ROUTES = "ExcludeRoute"
const val USE_PLUGGABLE_TRANSPORTS = "UsePluggableTransports"
const val STOP_VPN_ACTION = "StopMoonbounce"
const val START_VPN_ACTION = "StartMoonbounce"
const val APP_PACKAGE = "CallingActivityClass"

class MBAKVpnService() : VpnService()
{
    val sizeInBits = 32
    val maxBatchSize =  250 // bytes
    val maxPacketSize = 2048
    val timeoutDuration = 250 // milliseconds
    var batchBuffer = byteArrayOf()
    var lastPacketSentTime = TimeSource.Monotonic.markNow()

    val TAG = "MBAKVpnService"
    var parcelFileDescriptor: ParcelFileDescriptor? = null
    var transmissionConnection: TransmissionConnection? = null
    var shadowConnection: ShadowConnection? = null
    var transportServerIP = ""
    var transportServerPort = 1234
    var transportServerPublicKey: String? = null
    var usePluggableTransport = false
    val foregroundID = 5678

    private val dnsServerIP = "8.8.8.8"
    private val route = "0.0.0.0"
    private val subnetMask = 8
    private var builder: Builder = Builder()
    private var disallowedApps: Array<String>? = null
    private var excludeRoutes: Array<String>? = null

    // Needed to create an explicit intent for broadcasting status to an explicit application package name
    // Defaults to the example app, pass your package name in as an extra in the VPN intent to override this
    private var applicationPackageName: String = "org.operatorfoundation.moonbounceAndroidKotlin"

    companion object
    {
        const val vpnStatusNotification = "org.operatorfoundation.moonbounceAndroidKotlin.VPNStatusNotification"
        const val tcpTestNotification = "org.operatorfoundation.moonbounceAndroidKotlin.tcptestnotification"
        const val udpTestNotification = "org.operatorfoundation.moonbounceAndroidKotlin.udptestnotification"
        const val httpTestNotification = "org.operatorfoundation.moonbounceAndroidKotlin.httptestnotification"
        const val dnsTestNotification = "org.operatorfoundation.moonbounceAndroidKotlin.dnstestnotification"

        const val VPN_CONNECTED_STATUS = "VpnConnected"
        const val TCP_TEST_STATUS = "TCPTestPassed"
        const val UDP_TEST_STATUS = "UDPTestPassed"
        const val DNS_TEST_STATUS = "DNSTestPassed"
        const val HTTP_TEST_STATUS = "HTTPTestPassed"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        super.onStartCommand(intent, flags, startId)

        if (intent != null)
        {
            if (intent.action == STOP_VPN_ACTION)
            {
                stopVPN()
            }
            else if (intent.action == START_VPN_ACTION)
            {
                getConnectInfoFromIntent(intent)
                connect()
                createForegroundNotification()
            }
        }

        return START_STICKY
    }

    fun connect()
    {
        thread(start = true)
        {
            try
            {
                this.transmissionConnection = TransmissionConnection(
                    transportServerIP,
                    transportServerPort,
                    ConnectionType.TCP,
                    null
                )

                // Data sent through this socket will go directly to the underlying network, so its traffic will not be forwarded through the VPN
                // A VPN tunnel should protect itself if its destination is covered by VPN routes.
                // Otherwise its outgoing packets will be sent back to the VPN interface and cause an infinite loop.
                if (this.transmissionConnection == null)
                {
                    throw Error("🌙 Failed to create a connection to the server.")
                }

                protect(transmissionConnection!!.tcpConnection!!)
                parcelFileDescriptor = handshake()

                if (parcelFileDescriptor == null)
                {
                    stopVPN()

                    // Failed to create VPN tunnel
                    broadcastStatus(vpnStatusNotification, VPN_CONNECTED_STATUS, false)
                    return@thread
                }
                else
                {
                    val outputStream = FileOutputStream(parcelFileDescriptor!!.fileDescriptor)
                    val inputStream = FileInputStream(parcelFileDescriptor!!.fileDescriptor)

                    if (this.usePluggableTransport)
                    {
                        val serverAddress = "$transportServerIP:$transportServerPort"
                        val config = ShadowConfig(transportServerPublicKey!!, "Darkstar", serverAddress)
                        val context = applicationContext
                        this.shadowConnection = ShadowConnection(config, context, Logger.getLogger("MoonbounceShadowLogger"), this.transmissionConnection!!)
                    }

                    thread(start = true)
                    {
                        if (this.usePluggableTransport)
                        {
                            runServerToVPN(outputStream, this.shadowConnection!!)
                        }
                        else
                        {
                            runServerToVPN(outputStream, this.transmissionConnection!!)
                        }
                    }

                    thread(start = true)
                    {
                        if (this.usePluggableTransport)
                        {
                            runVPNtoServer(inputStream, this.shadowConnection!!)
                        }
                        else
                        {
                            runVPNtoServer(inputStream, this.transmissionConnection!!)
                        }
                    }

                    // We have successfully created a VPN tunnel
                    broadcastStatus(vpnStatusNotification, VPN_CONNECTED_STATUS, true)
                }
            } catch (error: Exception) {
                // Failed to create VPN tunnel
                broadcastStatus(vpnStatusNotification, VPN_CONNECTED_STATUS, false)
            }
        }
    }

    fun handshake(): ParcelFileDescriptor?
    {
        return prepareBuilder("10.0.0.1")
    }

    fun runVPNtoServer(vpnInputStream: FileInputStream, serverConnection: Connection)
    {
        while (true)
        {
            // Leave the loop if the socket is closed
            try
            {
                vpnToServer(vpnInputStream, serverConnection)
            }
            catch (vpnToServerError: Exception)
            {
                stopVPN()
                break
            }
        }
    }

    fun vpnToServer(vpnInputStream: FileInputStream, serverConnection: Connection)
    {
        val now = TimeSource.Monotonic.markNow()
        val packetLengthSize = sizeInBits/8 // In bytes
        val lengthBuffer = ByteBuffer.allocate(packetLengthSize)
        var readBuffer = ByteArray(maxPacketSize)

        val numberOfBytesReceived = vpnInputStream.read(readBuffer)

        if (numberOfBytesReceived == -1)
        {
            throw IOException()
        }

        if (numberOfBytesReceived == 0)
        {
            return
        }

        readBuffer = readBuffer.dropLast(readBuffer.size - numberOfBytesReceived).toByteArray()

        /// Batching ///
        // Create the packet size prefix
        lengthBuffer.putInt(numberOfBytesReceived)
        // Add the packet size prefix
        batchBuffer += lengthBuffer.array()
        // Add the packet payload
        batchBuffer += readBuffer

        // If we have enough data, send it
        if (batchBuffer.size >= maxBatchSize)
        {
            serverConnection.write(batchBuffer)
            batchBuffer = byteArrayOf()
        }
        else if ((now - lastPacketSentTime).inWholeMilliseconds >= timeoutDuration)
        {
            serverConnection.write(batchBuffer)
            batchBuffer = byteArrayOf()
        }

        return
    }

    fun runServerToVPN(vpnOutputStream: FileOutputStream, serverConnection: Connection)
    {
        while(true)
        {
            // Leave loop if the socket is closed
            try
            {
                serverToVPN(vpnOutputStream, serverConnection)
            }
            catch (serverToVPNError: Exception)
            {
                stopVPN()
                break
            }
        }
    }

    fun serverToVPN(vpnOutputStream: FileOutputStream, serverConnection: Connection)
    {
        val messageData = serverConnection.readWithLengthPrefix(sizeInBits)

        if (messageData == null)
        {
            stopVPN()
            return
        }
        else
        {
            vpnOutputStream.write(messageData)
        }
    }

    fun prepareBuilder(ipv4AssignmentString: String): ParcelFileDescriptor?
    {
        // Create a local TUN interface using predetermined addresses.
        //  You typically use values returned from the VPN gateway during handshaking.
        builder
            .setSession("MoonbounceAndroidKotlinVpnService")
            .addAddress(ipv4AssignmentString, subnetMask) // Local IP Assigned by server on handshake
            .addDnsServer(dnsServerIP)
            .addRoute(route, 0)
            .setBlocking(true)

        disallowedApps?.let { requestedAppsToExclude ->

            for (requestedApp in requestedAppsToExclude)
            {
                builder.addDisallowedApplication(requestedApp)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)  // TIRAMISU = API 33 = Version 13
        {
            excludeRoutes?.let { requestedRoutesToExclude ->

                for (requestedRoute in requestedRoutesToExclude)
                {
                    val excludeRouteInetAddress = InetAddress.getByName(requestedRoute)
                    val excludeRouteIpPrefix = IpPrefix(excludeRouteInetAddress, 32)

                    builder.excludeRoute(excludeRouteIpPrefix)
                }
            }
        }
        else
        {
            throw Exception("Attempted to use the exclude route feature with an android API less than 33. Ignoring.")
        }

        val parcelFileDescriptor = builder.establish()

        return parcelFileDescriptor
    }

    fun getConnectInfoFromIntent(intent: Intent?): Boolean
    {
        val maybeIP: String?
        val maybePort: Int
        val maybeServerPublicKey: String?

        val maybeDisallowedApps: Array<String>?
        val maybeExcludeRoutes: Array<String>?
        val maybeUsePluggableTransports: Boolean
        val maybeApplicationPackage: String?

        if (intent != null)
        {
            maybeIP = intent.getStringExtra(SERVER_IP)
            maybePort = intent.getIntExtra(SERVER_PORT, 0)
            maybeServerPublicKey = intent.getStringExtra(SERVER_PUBLIC_KEY)
            maybeDisallowedApps = intent.getStringArrayExtra(DISALLOWED_APPS)
            maybeExcludeRoutes = intent.getStringArrayExtra(EXCLUDE_ROUTES)
            maybeUsePluggableTransports = intent.getBooleanExtra(USE_PLUGGABLE_TRANSPORTS, false)
            maybeApplicationPackage = intent.getStringExtra(APP_PACKAGE)

            this.usePluggableTransport = maybeUsePluggableTransports
        }
        else
        {
            return false
        }

        if (maybeIP != null)
        {
            transportServerIP = maybeIP
        }
        else
        {
            return false
        }

        transportServerPublicKey = maybeServerPublicKey

        if (maybePort != 0)
        {
            transportServerPort = maybePort
        }
        else
        {
            return false
        }

        if (maybeUsePluggableTransports != null)
        {
            usePluggableTransport = maybeUsePluggableTransports
        }

        if (maybeDisallowedApps != null)
        {
            disallowedApps = maybeDisallowedApps
        }

        if (maybeExcludeRoutes != null)
        {
            excludeRoutes = maybeExcludeRoutes
        }

        if (maybeApplicationPackage != null)
        {
            applicationPackageName = maybeApplicationPackage
        }

        return true
    }

    fun createForegroundNotification()
    {
        val notificationChannelId = "VPN Service Channel"
        val channelName = "VPN Service Channel"
        val chan = NotificationChannel(notificationChannelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager: NotificationManager =
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)
        val notificationIntent = Intent(this, MBAKVpnService::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = Notification.Builder(this, notificationChannelId)
            .setSmallIcon(R.drawable.crecent_moon)
            .setContentTitle("VPN Service Channel")
            .setContentText("The VPN Tunnel is ON. Navigate to the VPN App to turn it off.")
            .setContentIntent(pendingIntent)
            .build()

        startForeground(foregroundID, notification)
    }

    fun broadcastStatus(action: String, statusDescription: String, status: Boolean)
    {
        val intent = Intent()
        intent.setPackage(applicationPackageName)
        intent.putExtra(statusDescription, status)
        intent.action = action
        sendBroadcast(intent)
    }

    fun stopVPN()
    {
        cleanup()
        stopSelf()
    }

    fun cleanup()
    {
        try {
            parcelFileDescriptor?.close()
        } catch (ex: IOException) {
            Log.e(TAG, "Calling parcelFileDescriptor.close()", ex)
        }

        try {
            transmissionConnection?.close()
        } catch (ex:IOException) {
            Log.e(TAG, "Calling TransmissionConnection.close()", ex)
        }

        try {
            shadowConnection?.close()
        } catch (ex:IOException) {
            Log.e(TAG, "Calling ShadowConnection.close()", ex)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy()
    {
        cleanup()
        super.onDestroy()
    }

    override fun onRevoke()
    {
        cleanup()
        super.onRevoke()
    }
}
