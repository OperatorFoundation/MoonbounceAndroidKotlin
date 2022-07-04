package org.operatorfoundation.moonbouncevpnservice

import org.operatorfoundation.transmission.ConnectionType
import org.operatorfoundation.transmission.TransmissionConnection
import kotlin.concurrent.thread

class NetworkTests
{
    fun udpTest(host: String, port: Int)
    {
        println("🌙 MBAKVpnService: Launching UDP Test")

        thread(start = true)
        {
            val transmissionConnection =
                TransmissionConnection(host, port, ConnectionType.UDP, null)
            transmissionConnection.write("Catbus is UDP tops!")

            val result = transmissionConnection.read(22)

            if (result == null)
            {
                println("🌙 NetworkTests: UDP test tried to read, but got no response")
            }
            else
            {
                val resultString = String(result)
                println("🌙 NetworkTests: UDP test got a response: " + resultString)
            }
        }
    }

    fun tcpTest(host: String, port: Int)
    {
        println("🌙 Launching TCP Test")

        thread(start = true)
        {
            val transmissionConnection = TransmissionConnection(host, port, ConnectionType.TCP, null)
            transmissionConnection.write("Catbus is TCP tops!")

            val result = transmissionConnection.read(5)

            if (result == null)
            {
                println("🌙 TCP test tried to read, but got no response")
            }
            else
            {
                val resultString = String(result)
                println("🌙 NetworkTests: TCP test got a response: " + resultString)
            }
        }
    }
}