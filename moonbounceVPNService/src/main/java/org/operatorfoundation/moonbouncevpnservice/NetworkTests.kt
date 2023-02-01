package org.operatorfoundation.moonbouncevpnservice

import android.content.Intent
import org.operatorfoundation.transmission.ConnectionType
import org.operatorfoundation.transmission.TransmissionConnection
import kotlin.concurrent.thread
import android.content.Context
import org.operatorfoundation.moonbouncevpnservice.MBAKVpnService.Companion.TCP_TEST_STATUS
import org.operatorfoundation.moonbouncevpnservice.MBAKVpnService.Companion.UDP_TEST_STATUS
import org.operatorfoundation.moonbouncevpnservice.MBAKVpnService.Companion.tcpTestNotification
import org.operatorfoundation.moonbouncevpnservice.MBAKVpnService.Companion.udpTestNotification


class NetworkTests (val context: Context)
{
    var host: String = "0.0.0.0"
    var udpEchoPort = 2233
    var tcpEchoPort = 2234

    fun udpTest()
    {
        println("🌙 MBAKVpnService: Launching UDP Test")

        thread(start = true)
        {
            try
            {
                val transmissionConnection =
                    TransmissionConnection(host, udpEchoPort, ConnectionType.UDP, null)
                transmissionConnection.write("ᓚᘏᗢ Catbus is UDP tops! ᓚᘏᗢ")

                val result = transmissionConnection.read(22)

                if (result == null)
                {
                    println("🌙 NetworkTests: UDP test tried to read, but got no response")
                    broadcastStatus(udpTestNotification, UDP_TEST_STATUS, false)
                }
                else
                {
                    val resultString = String(result)
                    println("🌙 NetworkTests: UDP test got a response: $resultString")
                    broadcastStatus(udpTestNotification, UDP_TEST_STATUS, true)
                }
            }
            catch(error: Exception)
            {
                println("🌙 NetworkTests: UDP test failed to make a connection. $error")
                broadcastStatus(udpTestNotification, UDP_TEST_STATUS, false)
            }
        }
    }

    fun tcpTest()
    {
        println("🌙 Launching TCP Test")
        println("host and port: $host: $tcpEchoPort")

        thread(start = true)
        {
            try
            {
                val transmissionConnection = TransmissionConnection(host, tcpEchoPort, ConnectionType.TCP, null)
                println("🌙 TCP test: Transmission Connection created.")
                transmissionConnection.write("ᓚᘏᗢ Catbus is TCP tops! ᓚᘏᗢ")
                println("🌙 TCP test: Wrote some data...")

                val result = transmissionConnection.read(5)

                if (result == null)
                {
                    println("🌙 TCP test tried to read, but got no response")
                    broadcastStatus(tcpTestNotification, TCP_TEST_STATUS, false)
                }
                else
                {
                    val resultString = String(result)
                    println("🌙 NetworkTests: TCP test got a response: $resultString")

                    broadcastStatus(tcpTestNotification, TCP_TEST_STATUS, true)
                }
            }
            catch(error: Exception)
            {
                println("🌙 NetworkTests: TCP test failed to make a connection. $error")
                broadcastStatus(tcpTestNotification, TCP_TEST_STATUS, false)
            }
        }
    }

    fun broadcastStatus(action: String, statusDescription: String, status: Boolean)
    {
        val intent = Intent()
        intent.putExtra(statusDescription, status)
        intent.action = action
        context.sendBroadcast(intent)
    }

//    fun tcpBroadcastMessage(success: Boolean)
//    {
//        println("*******BROADCASTING TCP MESSAGE")
//        Intent().also { intent ->
//            intent.action = broadcastTCPAction
//            intent.putExtra(isSuccessful, success)
//            context.sendBroadcast(intent)
//        }
//    }
//
//    fun udpBroadcastMessage(success: Boolean)
//    {
//        println("*******BROADCASTING UDP MESSAGE")
//
//        Intent().also { intent ->
//            intent.action = broadcastUDPAction
//            intent.putExtra(udpIsSuccessful, success)
//            context.sendBroadcast(intent)
//        }
//    }
}
