package org.OperatorFoundation.MoonbounceAndroidKotlin

import android.R
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.widget.Button
import android.widget.TextView
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class MBAKVpnService: VpnService() {
    private var mThread: Thread? = null
    private var mInterface: ParcelFileDescriptor? = null

    // configure a builder for the interface.
    private var builder: Builder = Builder()
    val transportServerIP = ""
    val localHost = "127.0.0.1"
    val replicantServerPort = 2277
    val shadowSocksServerPort = 2345
    val dnsServerIP = "8.8.8.8"
    val route = "0.0.0.0"
    var subnetMask = 32

    // Services interface
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start a new session by creating a new thread.
        mThread = Thread({
            try {
                println("Entered try block of onStartCommand function")
                //a. Configure the TUN and get the interface.
                prepareBuilder()

                //b. Packets to be sent are queued in this input stream
                val input = FileInputStream(
                    mInterface!!.fileDescriptor
                )
                println("created input stream")
                //b. Packets received need to be written to this output stream.
                val output = FileOutputStream(
                    mInterface!!.fileDescriptor
                )
                println("created output stream")

                //c. The UDP channel can be used to pass/get ip package to/from server
                val tunnel: DatagramChannel = DatagramChannel.open()
                println("called DatagramChannel.open()")
                // Connect to the server, localhost is used for demonstration only.
                val socketAddress = InetSocketAddress(localHost, shadowSocksServerPort)
                tunnel.connect(socketAddress)
                println("called tunnel.connect()")

                //d. Protect this socket, so package send by it will not be feedback to the vpn service.
                protect(tunnel.socket())
                println("called protect(), starting loop")

                //e. Use a loop to pass packets.
                while (true)
                {
                    // this is where we will add code to handle the packets.
                    //get packet within
                    //put packet to tunnel
                    //get packet from tunnel
                    //return packet with out
                    //sleep is a must
                    // Can replace input.available() with an int for testing purposes
                    //val arrayBuffer = ByteArray(input.available())
                    val arrayBuffer = ByteArray(10)
                    val bytesRead = input.read(arrayBuffer)
//                    if (bytesRead != input.available())
//                    {
//                        println("Bytes read not equal to bytes available")
//                    }
                    println("bytes read: $bytesRead")
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
                println("exited loop")
            }
            catch (e: Exception)
            {
                // Catch any exception
                e.printStackTrace()
            }
            finally
            {
                try
                {
                    if (mInterface != null)
                    {
                        mInterface!!.close()
                        mInterface = null
                    }
                }
                catch (e: Exception)
                {
                    println(e)
                }
            }
        }, "MyVpnRunnable")

        //start the service
        mThread!!.start()
        println("start() called")
        return START_STICKY
    }

    override fun onDestroy() {
        // TODO Auto-generated method stub
        if (mThread != null) {
            mThread!!.interrupt()
        }
        super.onDestroy()
    }

    fun prepareBuilder() {

        mInterface = builder.setSession("MoonbounceAndroidKotlinVpnService")
            .addAddress(localHost, subnetMask)
            .addDnsServer(dnsServerIP)
            .addRoute(route, 0).establish()

        println("finished setting up builder")
    }
}