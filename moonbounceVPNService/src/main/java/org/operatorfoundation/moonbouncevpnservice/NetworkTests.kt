package org.operatorfoundation.moonbouncevpnservice

import android.content.Intent

import org.operatorfoundation.transmission.ConnectionType


import org.operatorfoundation.transmission.TransmissionConnection
import kotlin.concurrent.thread
import android.content.Context


class NetworkTests (val context: Context)
{
    private val broadcastTCPAction = "org.operatorfoundation.moonbounceAndroidKotlin.tcp.status"
    val isSuccessful = "TCP Tests Successful!"

    private val broadcastUDPAction = "org.operatorfoundation.moonbounceAndroidKotlin.udp.status"
    val udpIsSuccessful = "UDP Tests Successful!"
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
                    udpBroadcastMessage(false)
                }
                else
                {
                    val resultString = String(result)
                    println("🌙 NetworkTests: UDP test got a response: $resultString")
                    udpBroadcastMessage(true)
                }
            }
            catch(error: Exception)
            {
                println("🌙 NetworkTests: UDP test failed to make a connection. $error")
                udpBroadcastMessage(false)
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
                tcpBroadcastMessage(true)

                val result = transmissionConnection.read(5)

                if (result == null)
                {
                    println("🌙 TCP test tried to read, but got no response")
                    tcpBroadcastMessage(false)
                }
                else
                {
                    val resultString = String(result)
                    println("🌙 NetworkTests: TCP test got a response: $resultString")
                    tcpBroadcastMessage(true)
                }
            }
            catch(error: Exception)
            {
                println("🌙 NetworkTests: TCP test failed to make a connection. $error")
                tcpBroadcastMessage(false)
            }
        }
    }

    fun tcpBroadcastMessage(success: Boolean)
    {
        println("*******BROADCASTING TCP MESSAGE")
        Intent().also { intent ->
            intent.action = broadcastTCPAction
            intent.putExtra(isSuccessful, success)
            context.sendBroadcast(intent)
        }
    }

    fun udpBroadcastMessage(success: Boolean)
    {
        println("*******BROADCASTING UDP MESSAGE")

        Intent().also { intent ->
            intent.action = broadcastUDPAction
            intent.putExtra(udpIsSuccessful, success)
            context.sendBroadcast(intent)
        }
    }
}
