package org.OperatorFoundation.MoonbounceAndroidKotlin

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.*
import org.junit.Assert
import org.operatorfoundation.flower.*
import org.operatorfoundation.transmission.ConnectionType
import org.operatorfoundation.transmission.TransmissionConnection
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class MBAKVpnService: VpnService()
{
    //private var mThread: Thread? = null
    //private var parcelFileDescriptor: ParcelFileDescriptor? = null
    val coroutineContext: CoroutineContext = EmptyCoroutineContext
    val externalScope: CoroutineScope = CoroutineScope(coroutineContext)
    val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

    val parentJob = Job()
    var vpnToServerCouroutineScope = CoroutineScope(Dispatchers.Default + parentJob)
    var serverToVPNCoroutineScope = CoroutineScope(Dispatchers.Default + parentJob)
    var udpCoroutineScope = CoroutineScope(Dispatchers.Default + parentJob)
    var tcpCouroutineScope = CoroutineScope(Dispatchers.Default + parentJob)


    private var builder: Builder = Builder()
    var transportServerIP = ""
    var transportServerPort = 2277
    val dnsServerIP = "8.8.8.8"
    val route = "0.0.0.0"
    val subnetMask = 8

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        externalScope.launch{connect(intent)}
        return START_STICKY
    }

    suspend fun connect(intent: Intent?) {

        var maybeIP: String?
        val maybePort: Int

        if (intent != null) {
            maybeIP = intent.getStringExtra(SERVER_IP)
            maybePort = intent.getIntExtra(SERVER_PORT, 0)
        } else {
            print("MBAKVpnService intent was null")
            return
        }

        if (maybeIP != null) {
            transportServerIP = maybeIP
        } else {
            print("Tried to connect without a valid IP")
            return
        }

        if (maybePort != 0) {
            transportServerPort = maybePort
        } else {
            print("Tried to connect without a valid port")
            return
        }

        externalScope.launch(defaultDispatcher)
        {

        try {
            val transmissionConnection = TransmissionConnection(transportServerIP, transportServerPort, ConnectionType.TCP, null)
            protect(transmissionConnection.tcpConnection!!)

            val flowerConnection = FlowerConnection(transmissionConnection, null)
            val socketAddress = InetSocketAddress(transportServerIP, transportServerPort)
            val messageData = IPRequestV4().data
            val ipRequest = org.operatorfoundation.flower.Message(messageData)
            flowerConnection.writeMessage(ipRequest)
            val flowerResponse = flowerConnection.readMessage()

            if (flowerResponse == null)
            {
                println("🥀 Moonbounce did not receive a Flower response from the server. 164🥀")
            }
            else
            {

                println("@->-- Got a flower response!!")
                when(flowerResponse.messageType)
                {
                    MessageType.IPAssignV4Type ->
                    {
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

                            vpnToServerCouroutineScope.launch {
                                println("^^^^^^ Launching read coroutine")
                                while(true)
                                {
                                    // FIXME: Leave loop if the socket is closed
                                    serverToVPN(outputStream, flowerConnection)
                                }
                            }


                            serverToVPNCoroutineScope.launch {
                                println("^^^^^ Launching write coroutine")
                                while (true)
                                {
                                    // FIXME: Leave loop if the socket is closed
                                    vpnToServer(inputStream, flowerConnection)
                                }
                            }

                            udpCoroutineScope.launch {
                                println("~~~~ Launching UDP Test")
                                udpTest()
                            }

                            tcpCouroutineScope.launch {
                                println("**** Launchinf TCP Test")
                                tcpTest()
                            }
                        }

                    }
                    else ->
                    {
                        print("Our first response from the server was not an ipv4 assignment.")
                    }
                }

                //val message = Message(IPDataV4(pingPacket).data)
                //flowerConnection.writeMessage(message)
            }
        }
        catch (error: Exception)
        {
            print("Error creating socket")
            print(error)
        }
    } .join()
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
                print("UDP test tried to read, but got no response")
            }
            else
            {
                val resultString = String(result)
                print("UDP test got a response: ")
                print(resultString)
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
                print("TCP test tried to read, but got no response")
            }
            else
            {
                val resultString = String(result)
                print("TCP test got a response: ")
                print(resultString)
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

            if (messageReceived == null)
            {
                return@async
            }
            else
            {
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

        println("finished setting up builder")
        return parcelFileDescriptor
    }
}