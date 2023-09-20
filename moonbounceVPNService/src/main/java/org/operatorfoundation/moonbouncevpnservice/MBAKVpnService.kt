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
import android.widget.Toast
import org.operatorfoundation.transmission.ConnectionType
import org.operatorfoundation.transmission.TransmissionConnection
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress
import kotlin.concurrent.thread

val SERVER_PORT = "ServerPort"
val SERVER_IP = "ServerIP"
val DISALLOWED_APP = "DisallowedApp"
const val EXCLUDE_ROUTE = "ExcludeRoute"
val USE_PLUGGABLE_TRANSPORTS = "UsePluggableTransports"
val STOP_VPN_ACTION = "StopMoonbounce"
val START_VPN_ACTION = "StartMoonbounce"

class MBAKVpnService : VpnService()
{
    val sizeInBits = 32
    val TAG = "MBAKVpnService"
    var parcelFileDescriptor: ParcelFileDescriptor? = null
    var transmissionConnection: TransmissionConnection? = null
    var inputStream: FileInputStream? = null
    var outputStream: FileOutputStream? = null
    var transportServerIP = ""
    var transportServerPort = 1234
    var usePluggableTransport = false
    var useTransport = false
    val foregroundID = 5678

    private val dnsServerIP = "8.8.8.8"
    private val route = "0.0.0.0"
    private val subnetMask = 8
    private var builder: Builder = Builder()
    private var disallowedApp = ""
    private var excludeRoute: String? = null

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
        print("****** onStartCommand called *******")
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
                    throw Error("Failed to create a connection to the server.")
                }

                protect(transmissionConnection!!.tcpConnection!!)
                parcelFileDescriptor = handshake()

                if (parcelFileDescriptor == null)
                {
                    println("🌙 MBAKVpnService: Failed to prepare the builder. Closing the connection.")
                    stopVPN()

                    // Failed to create VPN tunnel
                    broadcastStatus(vpnStatusNotification, VPN_CONNECTED_STATUS, false)
                    return@thread
                }
                else
                {
                    outputStream = FileOutputStream(parcelFileDescriptor!!.fileDescriptor)
                    inputStream = FileInputStream(parcelFileDescriptor!!.fileDescriptor)

                    println("ð MBAKVpnService: starting ServerToVPN loop")
                    thread(start = true)
                    {
                        runServerToVPN()
                    }

                    println("🌙 MBAKVpnService: starting VPNtoServer loop")
                    thread(start = true)
                    {
                        runVPNtoServer()
                    }
                    println("We have successfully created a VPN tunnel.")
                    // We have successfully created a VPN tunnel
                    broadcastStatus(vpnStatusNotification, VPN_CONNECTED_STATUS, true)
                }
            } catch (error: Exception) {
                println("🌙 MBAKVpnService: Error using ip $transportServerIP and port $transportServerPort. Error message: " + error.message)

                // Failed to create VPN tunnel
                broadcastStatus(vpnStatusNotification, VPN_CONNECTED_STATUS, false)
            }
        }
    }

    fun handshake(): ParcelFileDescriptor?
    {
        return prepareBuilder("10.0.0.1")
    }

    fun runVPNtoServer()
    {
        while (true)
        {
            // Leave the loop if the socket is closed
            if (transmissionConnection?.tcpConnection != null && transmissionConnection!!.tcpConnection!!.isClosed)
            {
                break
            }
            else if (transmissionConnection != null && inputStream != null)
            {
                try
                {
                    vpnToServer(inputStream!!)
                }
                catch (vpnToServerError: Exception)
                {
                    println("🌙 MBAKVpnService.runVPNtoServer: Error: $vpnToServerError")
                    stopVPN()
                    break
                }
            }
            else
            {
                break
            }
        }
    }

    fun vpnToServer(vpnInputStream: FileInputStream)
    {
        try {
            var readBuffer = ByteArray(2048)
            val numberOfBytesReceived = vpnInputStream.read(readBuffer)
            readBuffer = readBuffer.dropLast(readBuffer.size - numberOfBytesReceived).toByteArray()

            if (numberOfBytesReceived < 1)
            {
                return
            }
            else
            {
                println("🌖 MoonbounceAndroid.vpnToServer: inputStream.readBytes() received ${numberOfBytesReceived} bytes")
                if (transmissionConnection == null)
                {
                    println("vpnToServer(): Transmission connection is closed")
                    return
                }
                else
                {
                    try
                    {
                        println("🌖 MoonbounceAndroid.vpnToServer: calling connection.writeMessage()")
                        transmissionConnection!!.writeWithLengthPrefix(readBuffer, sizeInBits)
                        println("🌖 MoonbounceAndroid.vpnToServer: returned from connection.writeMessage()")
                    }
                    catch (vpnToServerWriteError: Exception)
                    {
                        println("\uD83C\uDF16 MoonbounceAndroid.vpnToServer: vpnToServerWriteError")
                        throw vpnToServerWriteError
                    }
                }
            }
        }
        catch (vpnToServerReadError: Exception)
        {
            println("MoonbounceAndroid.vpnToServer: vpnToServerReadError")
            throw vpnToServerReadError
        }
    }

    fun runServerToVPN()
    {
        while(true)
        {
            // Leave loop if the socket is closed
            if (transmissionConnection?.tcpConnection != null && transmissionConnection!!.tcpConnection!!.isClosed)
            {
                break
            }
            else if (transmissionConnection != null && outputStream != null)
            {
                try
                {
                    serverToVPN(outputStream!!, transmissionConnection!!)
                }
                catch (serverToVPNError: Exception)
                {
                    println("MoonbounceAndroid.serverToVPN Error: $serverToVPNError")
                    stopVPN()
                    break
                }
            }
        }
    }

    fun serverToVPN(vpnOutputStream: FileOutputStream, connection: TransmissionConnection)
    {
        val messageData = connection.readWithLengthPrefix(sizeInBits)

        if (messageData == null)
        {
            println("\uD83C\uDF16 MoonbounceAndroid.serverToVPN: Received a null response from our call to readMessage() closing the connection.")
            stopVPN()
            return
        }
        else
        {
            println("🌖 MoonbounceAndroid.serverToVPN: inputStream.readBytes() received ${messageData.size} bytes")
            vpnOutputStream.write(messageData)
            println("\uD83C\uDF16 MoonbounceAndroid.serverToVPN: finished writing ${messageData.size} bytes to outputStream.")
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

            println("********* Add disallowed application: $disallowedApp ********")
            builder.addDisallowedApplication(disallowedApp)

            // TODO: Test excludeRoute
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)  // TIRAMISU = API 33 = Version 13
            {
                excludeRoute?.let {
                    val excludeRouteInetAddress = InetAddress.getByName(it)
                    println("Get the excludeRouteInetAddress: $excludeRouteInetAddress")
                    val excludeRouteIpPrefix = IpPrefix(excludeRouteInetAddress, 32)
                    println("********* Get the excludeRouteIpPrefix: $excludeRouteIpPrefix *******")

                    builder.excludeRoute(excludeRouteIpPrefix)
                }
            }

        val parcelFileDescriptor = builder.establish()

        println("🌙 finished setting up the VPNService builder")

        return parcelFileDescriptor
    }

    fun getConnectInfoFromIntent(intent: Intent?): Boolean
    {
        val maybeIP: String?
        val maybePort: Int
        val maybeDisallowedApp: String?
        val maybeExcludeRoute: String?
        val maybeUsePluggableTransports: Boolean

        if (intent != null)
        {
            maybeIP = intent.getStringExtra(SERVER_IP)
            maybePort = intent.getIntExtra(SERVER_PORT, 0)
            maybeDisallowedApp = intent.getStringExtra(DISALLOWED_APP)
            maybeExcludeRoute = intent.getStringExtra(EXCLUDE_ROUTE)
            maybeUsePluggableTransports = intent.getBooleanExtra(USE_PLUGGABLE_TRANSPORTS, false)

            println("MBAKVpnService Server IP is: $maybeIP")
            println("MBAKVpnService Server Port is: $maybePort")
            println("MBAKVpnService Disallowed App is: $maybeDisallowedApp")
            println("MBAKVpnService Exclude Route is: $maybeExcludeRoute")
            println("MBAKVpnServer Use Pluggable Transports is: $maybeUsePluggableTransports")
        }
        else
        {
            println("🌙 MBAKVpnService: MBAKVpnService intent was null")
            return false
        }

        if (maybeIP != null)
        {
            transportServerIP = maybeIP
        }
        else
        {
            println("🌙 MBAKVpnService: Tried to connect without a valid IP")
            return false
        }

        if (maybePort != 0)
        {
            transportServerPort = maybePort
        }
        else
        {
            println("🌙 MBAKVpnService: Tried to connect without a valid port")
            return false
        }

        if (maybeDisallowedApp != null)
        {
            disallowedApp = maybeDisallowedApp
        }
        else
        {
            println("MBAKVpnService: No Disallowed App was requested.")
        }
        if (maybeExcludeRoute != null)
        {
            excludeRoute = maybeExcludeRoute
        }
        else
        {
            println("MBAKVpnService: No Exclude Route was requested.")
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
            .setContentText("VPN Tunnel is ON. Navigate to the MBAK App to turn it off.")
            .setContentIntent(pendingIntent)
            .build()

        startForeground(foregroundID, notification)
    }

    fun broadcastStatus(action: String, statusDescription: String, status: Boolean)
    {
        val intent = Intent()
        intent.putExtra(statusDescription, status)
        intent.action = action
        sendBroadcast(intent)
    }

    public fun stopVPN()
    {
        cleanUp()
        stopSelf()
    }

    fun cleanUp()
    {
        try {
            parcelFileDescriptor?.close()
        } catch (ex: IOException) {
            Log.e(TAG, "parcelFileDescriptor.close()", ex)
        }

        try {
            transmissionConnection?.close()
        } catch (ex:IOException) {
            Log.e(TAG, "flowerConnection.close()", ex)
        }

        try {
            outputStream?.close()
        } catch (ex:IOException) {
            Log.e(TAG, "outputStream.close()", ex)
        }

        try {
            inputStream?.close()
        } catch (ex:IOException) {
            Log.e(TAG, "inputStream.close()", ex)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy()
    {
        cleanUp()
        super.onDestroy()
    }

    override fun onRevoke()
    {
        cleanUp()
        super.onRevoke()
    }
}
