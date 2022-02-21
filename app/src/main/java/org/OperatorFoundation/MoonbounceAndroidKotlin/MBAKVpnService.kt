package org.OperatorFoundation.MoonbounceAndroidKotlin

import android.content.Intent
import android.net.VpnService
import android.net.ipsec.ike.TunnelModeChildSessionParams
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


class MBAKVpnService: VpnService() {
    //private var mThread: Thread? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    val coroutineContext: CoroutineContext = EmptyCoroutineContext
    val externalScope: CoroutineScope = CoroutineScope(coroutineContext)
    val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
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
            var socket = Socket()

            // Call VpnService.protect() to keep your app's tunnel socket outside of the system VPN and avoid a circular connection.
            protect(socket)

            // Call DatagramSocket.connect() to connect your app's tunnel socket to the VPN gateway.
            val socketAddress = InetSocketAddress(transportServerIP, transportServerPort)
            socket.connect(socketAddress)

            // Read from the socket to get our handshake information from the server as bytes (this is actually in Flower format)
            // Use this information for builder
            // 2 bytes (length of message in NetworkByteOrder),
            // 1 byte (type of message - should be a 6 here which indicates IPV4 assignment message),
            // 4 bytes (the actual IPV4 assignment)

            // If we want to do IPV6 read the first two bytes and that will be the size of the byte array
            // at that point, that's when we'll use messageType to create an if statement depending of
            // if the byte is 6 or not indicating IPV4
            var handshakeInformation = ByteArray(7)
            socket.getInputStream().read(handshakeInformation)
            var messageLength = handshakeInformation.sliceArray(0..1) // First two bytes

            var messageType =
                handshakeInformation[2] // 3rd Byte (which should be a 6 to indicate IPV4)
            var ipv4Assignment = handshakeInformation.sliceArray(3 until 7)

            var inetAddress = InetAddress.getByAddress(ipv4Assignment)
            var ipv4AssignmentString = inetAddress.toString()

            // Call VpnService.Builder methods to configure a new local TUN interface on the device for VPN traffic.
            prepareBuilder(ipv4AssignmentString)

            // ParcelFileDescriptor will read and write here (builder)

        } catch (error: Exception) {
            print("Error creating socket")
            print(error)
        }
    } .join()
    }
//    // Services interface
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
//    {
//        var maybeIP: String?
//        val maybePort: Int
//
//        if (intent != null)
//        {
//            maybeIP = intent.getStringExtra(SERVER_IP)
//            maybePort = intent.getIntExtra(SERVER_PORT, 0)
//        }
//        else
//        {
//            print("MBAKVpnService intent was null")
//            return Service.START_NOT_STICKY
//        }
//
//        if (maybeIP != null)
//        {
//            transportServerIP = maybeIP
//        }
//        else
//        {
//            print("Tried to connect without a valid IP")
//            return Service.START_NOT_STICKY
//        }
//
//        if (maybePort != 0)
//        {
//            transportServerPort = maybePort
//        }
//        else
//        {
//            print("Tried to connect without a valid port")
//            return Service.START_NOT_STICKY
//        }
//
//        // Start a new session by creating a new thread.
//        mThread = Thread({
//            try {
//                println("Entered try block of onStartCommand function")
//
//                //a. Configure the TUN and get the interface.
//                prepareBuilder()
//
//                //b. Packets to be sent are queued in this input stream
//                val input = FileInputStream(
//                    mInterface!!.fileDescriptor
//                )
//                println("created input stream")
//                //b. Packets received need to be written to this output stream.
//                val output = FileOutputStream(
//                    mInterface!!.fileDescriptor
//                )
//                println("created output stream")
//
//                //c. The UDP channel can be used to pass/get ip package to/from server
//                val tunnel: DatagramChannel = DatagramChannel.open()
//                println("called DatagramChannel.open()")
//                // Connect to the server, localhost is used for demonstration only.
//                val socketAddress = InetSocketAddress(transportServerIP, transportServerPort)
//                tunnel.connect(socketAddress)
//                println("called tunnel.connect()")
//
//                //d. Protect this socket, so package send by it will not be feedback to the vpn service.
//                protect(tunnel.socket())
//                println("called protect(), starting loop")
//
//                //e. Use a loop to pass packets.
//                while (true)
//                {
//                    // this is where we will add code to handle the packets.
//                    //get packet within
//                    //put packet to tunnel
//                    //get packet from tunnel
//                    //return packet with out
//                    //sleep is a must
//                    // Can replace input.available() with an int for testing purposes
//                    //val arrayBuffer = ByteArray(input.available())
//                    val arrayBuffer = ByteArray(10)
//                    val bytesRead = input.read(arrayBuffer)
////                    if (bytesRead != input.available())
////                    {
////                        println("Bytes read not equal to bytes available")
////                    }
//                    println("bytes read: $bytesRead")
//                    // This is intended just to receive the packet we just put in the tunnel
//                    val byteBuffer = ByteBuffer.allocate(arrayBuffer.size)
//                    // put and send may not be in the correct order...
//                    byteBuffer.put(arrayBuffer)
//                    tunnel.receive(byteBuffer)
//                    val sendBuffer = ByteBuffer.allocate(arrayBuffer.size)
//                    tunnel.send(sendBuffer, socketAddress)
//                    // this is where we will need to return the packet with output...
//                    //output.write()
//                    Thread.sleep(100)
//                }
//                println("exited loop")
//            }
//            catch (e: Exception)
//            {
//                // Catch any exception
//                e.printStackTrace()
//            }
//            finally
//            {
//                try
//                {
//                    if (mInterface != null)
//                    {
//                        mInterface!!.close()
//                        mInterface = null
//                    }
//                }
//                catch (e: Exception)
//                {
//                    println(e)
//                }
//            }
//        }, "MyVpnRunnable")
//
//        //start the service
//        mThread!!.start()
//        println("start() called")
//        return START_STICKY
//    }

    override fun onDestroy()
    {
        // TODO Auto-generated method stub
//        if (mThread != null) {
//            mThread!!.interrupt()
//        }
        super.onDestroy()
    }

    fun prepareBuilder(ipv4AssignmentString: String)
    {
        // Create a local TUN interface using predetermined addresses.
        //  You typically use values returned from the VPN gateway during handshaking.
        parcelFileDescriptor = builder
            .setSession("MoonbounceAndroidKotlinVpnService")
            .addAddress(ipv4AssignmentString, subnetMask) // Local IP Assigned by server on handshake
            .addDnsServer(dnsServerIP)
            .addRoute(route, 0)
            .establish() // Call VpnService.Builder.establish() so that the system establishes the local TUN interface and begins routing traffic through the interface.

        println("finished setting up builder")
    }
}