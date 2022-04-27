package org.operatorfoundation.moonbounceAndroidKotlin

import org.operatorfoundation.transmission.ConnectionType
import org.operatorfoundation.transmission.TransmissionConnection

class NetworkTests {

    fun udpTest(host: String, port: Int)
    {
        val transmissionConnection = TransmissionConnection(host, port, ConnectionType.UDP, null)
        transmissionConnection.write("Catbus is UDP tops!")

        val result = transmissionConnection.readMaxSize(10)

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

    fun tcpTest(host: String, port: Int)
    {
        val transmissionConnection = TransmissionConnection(host, port, ConnectionType.TCP, null)
        transmissionConnection.write("Catbus is TCP tops!")

        val result = transmissionConnection.readMaxSize(10)

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