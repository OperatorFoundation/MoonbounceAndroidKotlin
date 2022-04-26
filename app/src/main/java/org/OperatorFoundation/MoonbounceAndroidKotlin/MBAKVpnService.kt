package org.OperatorFoundation.MoonbounceAndroidKotlin

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.*
import org.operatorfoundation.flower.*
import org.operatorfoundation.transmission.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class MBAKVpnService: VpnService()
{
    var vpnToServerCouroutineScope = CoroutineScope(Dispatchers.Default + Job())
    var serverToVPNCoroutineScope = CoroutineScope(Dispatchers.Default + Job())
    var udpCoroutineScope = CoroutineScope(Dispatchers.Default + Job())
    var tcpCouroutineScope = CoroutineScope(Dispatchers.Default + Job())


    private var builder: Builder = Builder()
    var transportServerIP = ""
    var transportServerPort = 2277
    val dnsServerIP = "8.8.8.8"
    val route = "0.0.0.0"
    val subnetMask = 8

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        connect(intent)
        //externalScope.launch{connect(intent)}
        return START_STICKY
    }

    fun connect(intent: Intent?)
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
            println("🌙 MBAKVpnService intent was null")
            return
        }

        if (maybeIP != null)
        {
            transportServerIP = maybeIP
        }
        else
        {
            println("🌙 Tried to connect without a valid IP")
            return
        }

        if (maybePort != 0)
        {
            transportServerPort = maybePort
        }
        else
        {
            println("🌙 Tried to connect without a valid port")
            return
        }

        try
        {
            val transmissionConnection = TransmissionConnection(transportServerIP, transportServerPort, ConnectionType.TCP, null)
            protect(transmissionConnection.tcpConnection!!)

            val flowerConnection = FlowerConnection(transmissionConnection, null)
            val messageData = IPRequestV4().data
            val ipRequest = Message(messageData)

            println("\n🌙 is attempting to write a 🌻 message...")
            flowerConnection.writeMessage(ipRequest)

            println("\n🌙 is attempting to read a 🌻 message...")
            val flowerResponse = flowerConnection.readMessage()

            when(flowerResponse.messageType)
            {
                MessageType.IPAssignV4Type ->
                {
                    println("\n🌙 Got a 🌻 IPV4 Assignment!!")
                    val messageContent = flowerResponse.content as IPAssignV4
                    val inet4AddressData = messageContent.inet4Address.address

                    val inetAddress = InetAddress.getByAddress(inet4AddressData)
                    val ipv4AssignmentString = inetAddress.toString()

                    // Call VpnService.Builder methods to configure a new local TUN interface on the device for VPN traffic.
                    val parcelFileDescriptor = prepareBuilder(ipv4AssignmentString)

                    // ParcelFileDescriptor will read and write here (builder)
                    if (parcelFileDescriptor != null)
                    {
                        val outputStream = FileOutputStream(parcelFileDescriptor.fileDescriptor)
                        val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)

                        println("🌙 Launching read coroutine")
                        vpnToServerCouroutineScope.launch {
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
                                    serverToVPN(outputStream, flowerConnection)
                                }
                            }
                        }

                        println("🌙 Launching write coroutine")
                        serverToVPNCoroutineScope.launch {
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
                                    vpnToServer(inputStream, flowerConnection)
                                }
                            }
                        }

                        println("🌙 Launching UDP Test")
                        udpCoroutineScope.launch {
                            udpTest()
                        }

                        println("🌙 Launching TCP Test")
                        tcpCouroutineScope.launch {
                            tcpTest()
                        }
                    }
                }
                else ->
                {
                    println("🌙 Our first response from the server was not an ipv4 assignment.")
                }
            }
        }
        catch (error: Exception)
        {
            println("🌙 Error creating socket: " + error.message)
        }

//        externalScope.launch(defaultDispatcher)
//        {
//
//        } .join()
    }

    fun udpTest()
    {
        udpCoroutineScope.async(Dispatchers.IO)
        {
            val transmissionConnection = TransmissionConnection(transportServerIP, transportServerPort, ConnectionType.UDP, null)
            transmissionConnection.write("Catbus is UDP tops!")

            val result = transmissionConnection.readMaxSize(10)

            if (result == null)
            {
                println("🌙 UDP test tried to read, but got no response")
            }
            else
            {
                val resultString = String(result)
                println("🌙 UDP test got a response: " + resultString)
            }
        }
    }

    fun tcpTest()
    {
        tcpCouroutineScope.async(Dispatchers.IO)
        {
            val transmissionConnection = TransmissionConnection(transportServerIP, 7777, ConnectionType.TCP, null)
            transmissionConnection.write("Catbus is TCP tops!")

            val result = transmissionConnection.readMaxSize(10)

            if (result == null)
            {
                println("🌙 TCP test tried to read, but got no response")
            }
            else
            {
                val resultString = String(result)
                println("🌙 TCP test got a response: " + resultString)
            }
        }
    }

    fun vpnToServer(inputStream: FileInputStream, connection: FlowerConnection)
    {
        vpnToServerCouroutineScope.async(Dispatchers.IO)
        {
            val bytesReceived = inputStream.readBytes()

            if (bytesReceived.isEmpty())
            {
                return@async
            }
            else
            {
                val messageContent = IPDataV4(bytesReceived)
                val message = Message(messageContent.data)
                connection.writeMessage(message)
            }
        }
    }

    fun serverToVPN(outputStream: FileOutputStream, connection: FlowerConnection)
    {
        serverToVPNCoroutineScope.async(Dispatchers.IO)
        {
            val messageReceived = connection.readMessage()

            when(messageReceived.messageType)
            {
                MessageType.IPDataV4Type ->
                {
                    val messageContent = messageReceived.content as IPDataV4
                    val messageData = messageContent.bytes

                    outputStream.write(messageData)
                }

                else -> {
                    return@async
                }
            }
        }
    }

    override fun onDestroy()
    {
        // TODO Auto-generated method stub
//        if (mThread != null) {
//            mThread!!.interrupt()
//        }
        super.onDestroy()
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
}