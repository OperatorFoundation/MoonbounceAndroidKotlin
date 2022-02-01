package org.OperatorFoundation.MoonbounceAndroidKotlin

import android.net.VpnService
import android.content.Intent
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class VpnService: VpnService() {
    private var mThread: Thread? = null
    private var mInterface: ParcelFileDescriptor? = null

    // configure a builder for the interface.
    private var builder: Builder = Builder()

    // Services interface
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start a new session by creating a new thread.
        mThread = Thread({
            try {
                //a. Configure the TUN and get the interface.
                mInterface = builder.setSession("MoonbounceAndroidKotlinVpnService")
                    //trans¸ort server - 159.203.158.90
                    //let shadow socks ServerPort: UInt16 = 2345
                    //let replicantServerPort: UInt16 = 2277
                    // Uncertain if I have the correct IP address in .addAddress
                    .addAddress("159.203.158.90", 24)
                    .addDnsServer("8.8.8.8")
                    .addRoute("0.0.0.0", 0).establish()
                //b. Packets to be sent are queued in this input stream
                val input = FileInputStream(
                    mInterface!!.fileDescriptor
                )
                //b. Packets received need to be written to this output stream.
                val output = FileOutputStream(
                    mInterface!!.fileDescriptor
                )
                //c. The UDP channel can be used to pass/get ip package to/from server
                val tunnel: DatagramChannel = DatagramChannel.open()
                // Connect to the server, localhost is used for demonstration only.
                //transport server - 159.203.158.90
                //let shadow socks ServerPort: UInt16 = 2345
                //let replicantServerPort: UInt16 = 2277
                // Uncertain if we have the correct hostname IP address or port number
                val socketAddress = InetSocketAddress("159.203.158.90", 2345)
                tunnel.connect(socketAddress)
                //d. Protect this socket, so package send by it will not be feedback to the vpn service.
                // Does this protect() ensure that the VPN Service itself cannot intercept the package either.
                protect(tunnel.socket())
                //e. Use a loop to pass packets.
                while (true) {
                    // this is where we will add code to handle the packets.
                    //get packet with in
                    //put packet to tunnel
                    //get packet form tunnel
                    //return packet with out
                    //sleep is a must
                    // Can replace input.available() with an int for testing purposes
                    val arrayBuffer = ByteArray(input.available())
                    val bytesRead = input.read(arrayBuffer)
                    if (bytesRead != input.available())
                    {
                        println("Bytes read not equal to bytes available")
                    }
                    // This is intended just to receive the packet we just put in the tunnel
                    val byteBuffer = ByteBuffer.allocate(arrayBuffer.size)
                    // put and send may not be in the correct order...
                    byteBuffer.put(arrayBuffer)
                    tunnel.receive(byteBuffer)
                    val sendBuffer = ByteBuffer.allocate(arrayBuffer.size)
                    tunnel.send(sendBuffer, socketAddress)
                    // this is where we will need to return the packet with output...
                    //output.write()
                    Thread.sleep(100)
                }
            } catch (e: Exception) {
                // Catch any exception
                e.printStackTrace()
            } finally {
                try {
                    if (mInterface != null) {
                        mInterface!!.close()
                        mInterface = null
                    }
                } catch (e: Exception) {
                }
            }
        }, "MyVpnRunnable")

        //start the service
        mThread!!.start()
        return START_STICKY
    }

    override fun onDestroy() {
        // TODO Auto-generated method stub
        if (mThread != null) {
            mThread!!.interrupt()
        }
        super.onDestroy()
    }
}