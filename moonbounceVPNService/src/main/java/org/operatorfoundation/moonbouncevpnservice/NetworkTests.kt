package org.operatorfoundation.moonbouncevpnservice

import org.operatorfoundation.transmission.ConnectionType
import org.operatorfoundation.transmission.TransmissionConnection
import kotlin.NullPointerException
import kotlin.concurrent.thread

class NetworkTests
{
    var host: String = "0.0.0.0"
    var port: Int = 2233

    fun udpTest()
    {
        println("🌙 MBAKVpnService: Launching UDP Test")

        thread(start = true)
        {
            try
            {
                val transmissionConnection =
                    TransmissionConnection(host, port, ConnectionType.UDP, null)
                transmissionConnection.write("ᓚᘏᗢ Catbus is UDP tops! ᓚᘏᗢ")

                val result = transmissionConnection.read(22)

                if (result == null)
                {
                    println("🌙 NetworkTests: UDP test tried to read, but got no response")
                }
                else
                {
                    val resultString = String(result)
                    println("🌙 NetworkTests: UDP test got a response: $resultString")
                }
            }
            catch(error: NullPointerException)
            {
                println("🌙 NetworkTests: UDP test failed to make a connection. $error")
            }
        }
    }

    fun tcpTest()
    {
        println("🌙 Launching TCP Test")
//        println("host and port: $host / $port")

        thread(start = true)
        {
            try
            {
                val transmissionConnection = TransmissionConnection(host, port, ConnectionType.TCP, null)
                transmissionConnection.write("ᓚᘏᗢ Catbus is TCP tops! ᓚᘏᗢ")

                val result = transmissionConnection.read(5)

                if (result == null)
                {
                    println("🌙 TCP test tried to read, but got no response")
                }
                else
                {
                    val resultString = String(result)
                    println("🌙 NetworkTests: TCP test got a response: $resultString")
                }
            }
            catch(error: NullPointerException)
            {
                println("🌙 NetworkTests: TCP test failed to make a connection. $error")
            }
        }
    }
}