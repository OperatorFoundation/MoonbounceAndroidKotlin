package org.operatorfoundation.moonbouncevpnservice

import android.content.Intent
import android.net.InetAddresses
import android.net.IpPrefix
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.view.View
import androidx.annotation.RequiresApi
import org.operatorfoundation.flower.*
import org.operatorfoundation.transmission.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.Inet4Address
import java.net.InetAddress
import kotlin.concurrent.thread

val SERVER_PORT = "ServerPort"
val SERVER_IP = "ServerIP"
val DISALLOWED_APP = "DisallowedApp"
const val EXCLUDE_ROUTE = "ExcludeRoute"

class MBAKVpnService: VpnService()
{
    val broadcastAction = "org.operatorfoundation.moonbounceAndroidKotlin.status"
    val isConnected = "moonbounceVpnConnected"
    private var builder: Builder = Builder()
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var flowerConnection: FlowerConnection? = null
    private var inputStream: FileInputStream? = null
    private var outputStream: FileOutputStream? = null
    private val dnsServerIP = "8.8.8.8"
    private val route = "0.0.0.0"
    private val subnetMask = 8
    private var disallowedApp: String? = null
    private var excludeRoute: String? = null
    var transportServerIP = ""
    var transportServerPort = 1234

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        getConnectInfoFromIntent(intent)
        connect()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder?
    {
        // TODO: https://developer.android.com/reference/android/app/Service#onBind(android.content.Intent)
        return super.onBind(intent)
    }

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
                    broadcastStatus(false)
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

                    // We have successfully created a VPN tunnel
                    broadcastStatus(true)
                }
            } catch (error: Exception) {
                println("🌙 MBAKVpnService: Error using ip $transportServerIP and port $transportServerPort. Error message: " + error.message)

                // Failed to create VPN tunnel
                broadcastStatus(false)
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
            println("MoonbounceAndroid.serverToVPN: Received a null response from our call to readMessage() closing the connection.")
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

        // TODO: These need to be options the user decides.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            disallowedApp?.let {
                println("Add disallowed application: $it")
                builder.addDisallowedApplication(it)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            {
                excludeRoute?.let {
                    val excludeRouteInetAddress = InetAddress.getByName(it)
                    println("Get the excludeRouteInetAddress: $excludeRouteInetAddress")
                    val excludeRouteIpPrefix = IpPrefix(excludeRouteInetAddress, 32)
                    println("Get the excludeRouteIpPrefix: $excludeRouteIpPrefix")

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

        if (intent != null)
        {
            maybeIP = intent.getStringExtra(SERVER_IP)
            maybePort = intent.getIntExtra(SERVER_PORT, 0)
            disallowedApp = intent.getStringExtra(DISALLOWED_APP)
            excludeRoute = intent.getStringExtra(EXCLUDE_ROUTE)
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

        return true
    }

    fun broadcastStatus(connected: Boolean)
    {
        val intent = Intent()
        intent.putExtra(isConnected, connected)
        intent.action = broadcastAction
        sendBroadcast(intent)
    }

    fun stopVPN()
    {
        println("✋ Stopping VPN  ✋")
        cleanUp()
        stopSelf()
    }

    fun cleanUp()
    {
        parcelFileDescriptor?.close()
        flowerConnection?.connection?.close()
        outputStream?.close()
        inputStream?.close()
    }

    override fun onDestroy()
    {
        println("✋ onDestroy called ✋")
        super.onDestroy()

        cleanUp()
    }

    override fun onRevoke()
    {
        println("✋ onRevoke called ✋")
        super.onRevoke()

        cleanUp()
    }
}