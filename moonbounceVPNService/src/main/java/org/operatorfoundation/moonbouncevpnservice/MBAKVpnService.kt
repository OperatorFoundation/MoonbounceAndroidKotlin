package org.operatorfoundation.moonbouncevpnservice

import android.annotation.TargetApi
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
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import org.operatorfoundation.flower.*
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

class MBAKVpnService : VpnService()
{
    val TAG = "MBAKVpnService"
    var parcelFileDescriptor: ParcelFileDescriptor? = null
    var flowerConnection: FlowerConnection? = null
    var inputStream: FileInputStream? = null
    var outputStream: FileOutputStream? = null
    var transportServerIP = ""
    var transportServerPort = 1234

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

        const val VPN_CONNECTED_STATUS = "VpnConnected"
        const val TCP_TEST_STATUS = "TCPTestPassed"
        const val UDP_TEST_STATUS = "UDPTestPassed"
    }

    @Override public fun onBind(vpnService: VpnService): IBinder? {
        Log.d(TAG, "onBind Called")
        return null
    }

    @TargetApi(Build.VERSION_CODES.O) // O = Oreo = 8.0 = LVL 26
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN) // JELLY_BEAN = 4.1 = LVL 16
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        print("****** onStartCommand called *******")
        super.onStartCommand(intent, flags, startId)

        getConnectInfoFromIntent(intent)
        connect()
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
        startForeground(1337, notification)
        //TODO: Revisit the following if statement.
//        if (intent != null) {
//            if (intent.getAction().equals("StopService")) {
//                stopForeground(true)
//                stopSelf()
//            }
//        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun connect()
    {
        thread(start = true)
        {
            try
            {
                val transmissionConnection = TransmissionConnection(
                    transportServerIP,
                    transportServerPort,
                    ConnectionType.TCP,
                    null
                )

                // Data sent through this socket will go directly to the underlying network, so its traffic will not be forwarded through the VPN
                // A VPN tunnel should protect itself if its destination is covered by VPN routes.
                // Otherwise its outgoing packets will be sent back to the VPN interface and cause an infinite loop.
                protect(transmissionConnection.tcpConnection!!)

                flowerConnection = FlowerConnection(transmissionConnection, null, true, true)
                parcelFileDescriptor = handshake(flowerConnection!!)

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

    fun handshake(flowerConnection: FlowerConnection): ParcelFileDescriptor?
    {
        val messageData = IPRequestV4().data
        val ipRequest = Message(messageData)

        println("\n🌙 MBAKVpnService.handshake: is attempting to write a 🌻 message...")
        flowerConnection.writeMessage(ipRequest)

        println("\n🌙 MBAKVpnService.handshake: is attempting to read a 🌻 message...")
        val flowerResponse = flowerConnection.readMessage()

        if (flowerResponse == null)
        {
            println("🌙 MBAKVpnService: Received a null response from our call to readMessage.")
            return null
        }
        else
        {
            when(flowerResponse.messageType)
            {
                MessageType.IPAssignV4Type ->
                {
                    println("\n🌙 MBAKVpnService.handshake: Got a 🌻 IPV4 Assignment!!")
                    val messageContent = flowerResponse.content as IPAssignV4
                    val ipv4AssignmentString = messageContent.inet4Address.hostAddress

                    if (ipv4AssignmentString == null)
                    {
                        println("🌙 MBAKVpnService.handshake: Failed to get the ipv4Assignment String")
                        return null
                    }
                    else
                    {
                        println("🌙 MBAKVpnService.handshake: ipv4AssignmentString - $ipv4AssignmentString")
                        // TODO: Go up the chain until I find a function that can access user input.
                        return prepareBuilder(ipv4AssignmentString)
                    }
                }
                else ->
                {
                    println("🌙 MBAKVpnService.handshake: Our first response from the server was not an ipv4 assignment.")
                    return null
                }
            }
        }
    }

    fun runVPNtoServer()
    {

        while (true)
        {
            // Leave the loop if the socket is closed
            if (flowerConnection?.connection?.tcpConnection != null && flowerConnection!!.connection.tcpConnection!!.isClosed)
            {
                break
            }
            else if (flowerConnection?.connection?.udpConnection != null && flowerConnection!!.connection.udpConnection!!.isClosed)
            {
                break
            }
            else if (flowerConnection?.connection != null && inputStream != null)
            {
                try
                {
                    vpnToServer(inputStream!!, flowerConnection!!)
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

    fun vpnToServer(vpnInputStream: FileInputStream, connection: FlowerConnection)
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
                val messageContent = IPDataV4(readBuffer)
                val message = Message(messageContent.data)

                try
                {
                    println("🌖 MoonbounceAndroid.vpnToServer: calling connection.writeMessage()")
                    connection.writeMessage(message)
                    println("🌖 MoonbounceAndroid.vpnToServer: returned from connection.writeMessage()")
                }
                catch (vpnToServerWriteError: Exception)
                {
                    println("\uD83C\uDF16 MoonbounceAndroid.vpnToServer: vpnToServerWriteError")
                    throw vpnToServerWriteError
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
            if (flowerConnection?.connection?.tcpConnection != null && flowerConnection!!.connection.tcpConnection!!.isClosed)
            {
                break
            }
            else if (flowerConnection?.connection?.udpConnection != null && flowerConnection!!.connection.udpConnection!!.isClosed)
            {
                break
            }
            else if (flowerConnection?.connection != null && outputStream != null)
            {
                try
                {
                    serverToVPN(outputStream!!, flowerConnection!!)
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

    fun serverToVPN(vpnOutputStream: FileOutputStream, connection: FlowerConnection)
    {
        val messageReceived = connection.readMessage()

        if (messageReceived == null)
        {
            println("\uD83C\uDF16 MoonbounceAndroid.serverToVPN: Received a null response from our call to readMessage() closing the connection.")
            stopVPN()
            return
        }
        else
        {
            println("🌖 MoonbounceAndroid.serverToVPN: inputStream.readBytes() received ${messageReceived.data.size} bytes")

            when(messageReceived.messageType)
            {
                MessageType.IPDataV4Type ->
                {
                    val messageContent = messageReceived.content as IPDataV4
                    val messageData = messageContent.bytes
                    println("\uD83C\uDF16 MoonbounceAndroid.serverToVPN: writing ${messageData.size} bytes to outputStream.")
                    vpnOutputStream.write(messageData)
                    println("\uD83C\uDF16 MoonbounceAndroid.serverToVPN: finished writing ${messageData.size} bytes to outputStream.")
                }

                else ->
                {
                    println("Received an unsupported Flower message type: ${messageReceived.messageType}")
                    return
                }
            }
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)  // Lollipop = API 21 = Version 5
            {
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

        if (intent != null)
        {
            maybeIP = intent.getStringExtra(SERVER_IP)
            maybePort = intent.getIntExtra(SERVER_PORT, 0)
            maybeDisallowedApp = intent.getStringExtra(DISALLOWED_APP)
            maybeExcludeRoute = intent.getStringExtra(EXCLUDE_ROUTE)
            println("MBAKVpnService Server IP is: $maybeIP")
            println("MBAKVpnService Server Port is: $maybePort")
            println("MBAKVpnService Disallowed App is: $maybeDisallowedApp")
            println("MBAKVpnService Exclude Route is: $maybeExcludeRoute")
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

    fun broadcastStatus(action: String, statusDescription: String, status: Boolean)
    {
        val intent = Intent()
        intent.putExtra(statusDescription, status)
        intent.action = action
        sendBroadcast(intent)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun stopVPN()
    {
        println("✋ Stopping VPN  ✋")
        println("****** stopVPN called ******")
        Log.d(TAG, "onStopVPN Called")
        cleanUp()
        STOP_FOREGROUND_DETACH
        println("*****Reached the line after STOP_FOREGROUND_DETACH*******")
        stopSelf()
        println("**********Reached the line past stopSelf() in MBAKVpnService.kt")
    }

    fun cleanUp()
    {
        println("****** cleanUp called *******")
        Log.d(TAG, "cleanUp Called")
        try {
            parcelFileDescriptor?.close()
        } catch (ex: IOException) {
            Log.e(TAG, "parcelFileDescriptor.close()", ex)
        }
        try {
            flowerConnection?.connection?.close()
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
        STOP_FOREGROUND_DETACH
        stopSelf()
        println("✋ left cleanUp() function ✋")
    }

    @RequiresApi(api = Build.VERSION_CODES.N) // N = Nougat = 7.0 = LVL 24
    override fun onDestroy()
    {
        println("✋ onDestroy called ✋")
        Log.d(TAG, "onDestroy Called")
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_DETACH)
        cleanUp()
        stopVPN()
        stopSelf()
        Toast.makeText(this, "Notification Service Service destroyed by user.", Toast.LENGTH_LONG).show()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onRevoke()
    {
        println("✋ onRevoke called ✋")
        Log.d(TAG, "onRevoke Called")
        super.onRevoke()

        stopVPN()
        cleanUp()
    }
}
