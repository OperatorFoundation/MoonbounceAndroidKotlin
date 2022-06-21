package org.operatorfoundation.moonbouncevpnservice

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import org.operatorfoundation.flower.*
import org.operatorfoundation.transmission.*
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread

val SERVER_PORT = "ServerPort"
val SERVER_IP = "ServerIP"

class MBAKVpnService: VpnService()
{
    private var builder: Builder = Builder()
    var transportServerIP = ""
    var transportServerPort = 1234
    val dnsServerIP = "8.8.8.8"
    val route = "0.0.0.0"
    val subnetMask = 8

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        getConnectInfoFromIntent(intent)

        thread(start = true) {
            connect()
        }

        return START_STICKY
    }

    fun connect()
    {
        try
        {
            thread(start = true)
            {
                val transmissionConnection = TransmissionConnection(transportServerIP, transportServerPort, ConnectionType.TCP, null)
                protect(transmissionConnection.tcpConnection!!)

                val flowerConnection = FlowerConnection(transmissionConnection, null)
                val parcelFileDescriptor = handshake(flowerConnection)

                if (parcelFileDescriptor == null)
                {
                    println("🌙 MBAKVpnService: Failed to prepare the builder. Closing the connection.")
                    flowerConnection.connection.close()
                    return@thread
                }

                val outputStream = FileOutputStream(parcelFileDescriptor.fileDescriptor)
                val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)

                println("🌙 MBAKVpnService: starting ServerToVPN loop")
                thread(start = true)
                {
                    runServerToVPN(flowerConnection, inputStream, outputStream)
                }

                println("🌙 MBAKVpnService: starting VPNtoServer loop")
                thread(start = true)
                {
                    runVPNtoServer(flowerConnection, inputStream, outputStream)
                }
            }
        }
        catch (error: Exception)
        {
            println("🌙 MBAKVpnService: Error using ip $transportServerIP and port $transportServerPort. Error message: " + error.message)
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

    fun runVPNtoServer(flowerConnection: FlowerConnection, inputStream: FileInputStream, outputStream: FileOutputStream)
    {
        while (true)
        {
            // Leave the loop if the socket is closed
            if (flowerConnection.connection.tcpConnection != null && flowerConnection.connection.tcpConnection!!.isClosed)
            {
                break
            }
            else if (flowerConnection.connection.udpConnection != null && flowerConnection.connection.udpConnection!!.isClosed)
            {
                break
            }
            else
            {
                try
                {
                    vpnToServer(inputStream, flowerConnection)
                }
                catch (vpnToServerError: Exception)
                {
                    println("🌙 MBAKVpnService.runVPNtoServer: Error: $vpnToServerError")
                    flowerConnection.connection.close()
                    outputStream.close()
                    inputStream.close()
                    break
                }
            }
        }
    }

    fun vpnToServer(inputStream: FileInputStream, connection: FlowerConnection)
    {
        try {
            var readBuffer = ByteArray(2048)
            val numberOfBytesReceived = inputStream.read(readBuffer)
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

    fun runServerToVPN(flowerConnection: FlowerConnection, inputStream: FileInputStream, outputStream: FileOutputStream)
    {
        while(true)
        {
            // Leave loop if the socket is closed
            if (flowerConnection.connection.tcpConnection != null && flowerConnection.connection.tcpConnection!!.isClosed)
            {
                break
            }
            else if (flowerConnection.connection.udpConnection != null && flowerConnection.connection.udpConnection!!.isClosed)
            {
                break
            }
            else
            {
                try
                {
                    serverToVPN(outputStream, flowerConnection)
                }
                catch (serverToVPNError: Exception)
                {
                    println("MoonbounceAndroid.serverToVPN Error: $serverToVPNError")
                    flowerConnection.connection.close()
                    outputStream.close()
                    inputStream.close()
                    break
                }
            }
        }
    }

    fun serverToVPN(outputStream: FileOutputStream, connection: FlowerConnection)
    {
        val messageReceived = connection.readMessage()

        if (messageReceived == null)
        {
            println("MoonbounceAndroid.serverToVPN: Received a null response from our call to readMessage() closing the connection.")
            outputStream.close()
            connection.connection.close()
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
                    outputStream.write(messageData)
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
        val parcelFileDescriptor = builder
            .setSession("MoonbounceAndroidKotlinVpnService")
            .addAddress(ipv4AssignmentString, subnetMask) // Local IP Assigned by server on handshake
            .addDnsServer(dnsServerIP)
            .addRoute(route, 0)
            .establish() // Call VpnService.Builder.establish() so that the system establishes the local TUN interface and begins routing traffic through the interface.

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

    override fun onDestroy()
    {
        super.onDestroy()
    }
}