package org.OperatorFoundation.MoonbounceAndroidKotlin

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.net.InetSocketAddress
import java.net.Socket


class MBAKVpnService: VpnService() {
    private var mThread: Thread? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    private var builder: Builder = Builder()
    var transportServerIP = ""
    var transportServerPort = 2277
    val dnsServerIP = "8.8.8.8"
    val route = "0.0.0.0"
    val subnetMask = 8

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        connect(intent)
        return START_STICKY
    }

    fun connect(intent: Intent?)
    {

        var maybeIP: String?
        val maybePort: Int

        if (intent != null)
        {
            maybeIP = intent.getStringExtra(SERVER_IP)
            maybePort = intent.getIntExtra(SERVER_PORT, 0)
        }
        else
        {
            print("MBAKVpnService intent was null")
            return
        }

        if (maybeIP != null)
        {
            transportServerIP = maybeIP
        }
        else
        {
            print("Tried to connect without a valid IP")
            return
        }

        if (maybePort != 0)
        {
            transportServerPort = maybePort
        }
        else
        {
            print("Tried to connect without a valid port")
            return
        }

        try
        {
            var socket = Socket()
            // Call VpnService.protect() to keep your app's tunnel socket outside of the system VPN and avoid a circular connection.

            protect(socket)

            // Call DatagramSocket.connect() to connect your app's tunnel socket to the VPN gateway.
            val socketAddress = InetSocketAddress(transportServerIP, transportServerPort)
            socket.connect(socketAddress)

            // Read from the socket to get our handshake information from the server as bytes (this is actually in Flower format)
            // Use this information for builder

            // Call VpnService.Builder methods to configure a new local TUN interface on the device for VPN traffic.
            prepareBuilder()

            // ParcelFileDescriptor will read and write here
            // Thread for reading
            // Thread for writing
        }
        catch (error: Exception)
        {
            print("Error creating socket")
            print(error)
            return
        }
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

    fun prepareBuilder()
    {
        // Create a local TUN interface using predetermined addresses.
        //  You typically use values returned from the VPN gateway during handshaking.
        parcelFileDescriptor = builder.setSession("MoonbounceAndroidKotlinVpnService")
            .addAddress("10.0.0.3", subnetMask) // Local IP Assigned by server on handshake
            .addDnsServer(dnsServerIP)
            .addRoute(route, 0)
            .establish() // Call VpnService.Builder.establish() so that the system establishes the local TUN interface and begins routing traffic through the interface.

        println("finished setting up builder")
    }
}