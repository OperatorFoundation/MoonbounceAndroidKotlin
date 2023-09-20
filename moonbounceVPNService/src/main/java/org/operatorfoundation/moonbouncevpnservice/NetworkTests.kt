package org.operatorfoundation.moonbouncevpnservice

import android.content.Context
import android.content.Intent
import org.operatorfoundation.moonbouncevpnservice.MBAKVpnService.Companion.TCP_TEST_STATUS
import org.operatorfoundation.moonbouncevpnservice.MBAKVpnService.Companion.UDP_TEST_STATUS
import org.operatorfoundation.moonbouncevpnservice.MBAKVpnService.Companion.DNS_TEST_STATUS
import org.operatorfoundation.moonbouncevpnservice.MBAKVpnService.Companion.HTTP_TEST_STATUS
import org.operatorfoundation.moonbouncevpnservice.MBAKVpnService.Companion.tcpTestNotification
import org.operatorfoundation.moonbouncevpnservice.MBAKVpnService.Companion.udpTestNotification
import org.operatorfoundation.moonbouncevpnservice.MBAKVpnService.Companion.dnsTestNotification
import org.operatorfoundation.moonbouncevpnservice.MBAKVpnService.Companion.httpTestNotification
import org.operatorfoundation.transmission.ConnectionType
import org.operatorfoundation.transmission.Transmission.Companion.toHexString
import org.operatorfoundation.transmission.TransmissionConnection
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import kotlin.concurrent.thread


class NetworkTests (val context: Context)
{
    var host: String = "0.0.0.0"
    var udpEchoPort = 7
    var tcpEchoPort = 7

    fun udpTest()
    {
        println("🌙 MBAKVpnService: Launching UDP Test")

        thread(start = true)
        {
            try
            {
                val udpTestString = "ᓚᘏᗢ Catbus is UDP tops! ᓚᘏᗢ"
                println("🌙 Sending UDP test data (${udpTestString.toByteArray().size} bytes): ${udpTestString.toByteArray().toHexString()}")
                val transmissionConnection = TransmissionConnection(host, udpEchoPort, ConnectionType.UDP, null)
                transmissionConnection.write(udpTestString)
                println("🌙 NetworkTests: UDP returned from write.")

                val result = transmissionConnection.read(39)
                println("🌙 NetworkTests: UDP returned from read.")

                if (result == null)
                {
                    println("🌙 NetworkTests: UDP test tried to read, but got no response")
                    broadcastStatus(udpTestNotification, UDP_TEST_STATUS, false)
                }
                else
                {
                    val resultString = String(result)
                    println("🌙 NetworkTests: UDP test got a response: $resultString")

                    if (resultString == udpTestString)
                    {
                        println("🌙 NetworkTests: UDP test Success!!")
                    }
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

    fun testMultiPacketOut()
    {
        val exceedMTUSize = 2500
        val someBytes = ByteArray(exceedMTUSize)

        // TODO: Make tcpTest take a byte array to send
    }

    fun tcpTest()
    {
        println("🌙 Launching TCP Test")
        println("\uD83C\uDF19 host and port: $host: $tcpEchoPort")

        thread(start = true)
        {
            try
            {
                val testString = "Catbus is TCP tops!"

                val transmissionConnection = TransmissionConnection(host, tcpEchoPort, ConnectionType.TCP, null)
                println("🌙 TCP test: Transmission Connection created.")
                transmissionConnection.write(testString)
                println("🌙 TCP test: Wrote some data...")

                val result = transmissionConnection.read(testString.count())
                transmissionConnection.close()

                if (result == null)
                {
                    println("🌙 TCP test tried to read, but got no response")
                    broadcastStatus(tcpTestNotification, TCP_TEST_STATUS, false)
                }
                else
                {
                    val resultString = String(result)
                    println("🌙 NetworkTests: TCP test got a response (${result.size} bytes)): $resultString")

                    if (testString == resultString)
                    {
                        broadcastStatus(tcpTestNotification, TCP_TEST_STATUS, true)
                    }
                    else
                    {
                        broadcastStatus(tcpTestNotification, TCP_TEST_STATUS, false)
                    }
                }
            }
            catch(error: Exception)
            {
                println("🌙 NetworkTests: TCP test failed to make a connection. $error")
                broadcastStatus(tcpTestNotification, TCP_TEST_STATUS, false)
            }
        }
    }

    fun tcpConnectTest()
    {
        println("🌙 Launching TCP connect Test")
        println("\uD83C\uDF19 host and port: $host: $tcpEchoPort")

        thread(start = true)
        {
            try
            {
                val testString = "Catbus is TCP tops!"

                val testConnection = TransmissionConnection(host, tcpEchoPort, ConnectionType.TCP, null)
                println("🌙 TCP Connect test: Transmission Connection created. TCP Connect test succeeded!")
                testConnection.close()
                broadcastStatus(tcpTestNotification, TCP_TEST_STATUS, true)
            }
            catch(error: Exception)
            {
                println("🌙 TCP Connect test: TCP test failed to make a connection. $error")
                broadcastStatus(tcpTestNotification, TCP_TEST_STATUS, false)
            }
        }
    }

    fun tcpTest2k()
    {
        println("🌙 Launching TCP Test 2k")
        println("\uD83C\uDF19 host and port: $host: $tcpEchoPort")

        thread(start = true)
        {
            try
            {
                val testData = ByteArray(2000) { 'A'.code.toByte() }

                val transmissionConnection = TransmissionConnection(host, tcpEchoPort, ConnectionType.TCP, null)
                println("🌙 TCP test: Transmission Connection created.")

                transmissionConnection.write(testData)
                println("🌙 TCP test: Wrote some data...")

                val result = transmissionConnection.read(testData.count())
                transmissionConnection.close()

                if (result == null)
                {
                    println("🌙 TCP test tried to read, but got no response")
                    broadcastStatus(tcpTestNotification, TCP_TEST_STATUS, false)
                }
                else
                {
                    println("🌙 NetworkTests: TCP test got a response (${result.size} bytes)): $result")

                    if (testData.contentEquals(result))
                    {
                        broadcastStatus(tcpTestNotification, TCP_TEST_STATUS, true)
                    }
                    else
                    {
                        broadcastStatus(tcpTestNotification, TCP_TEST_STATUS, false)
                    }
                }
            }
            catch(error: Exception)
            {
                println("🌙 NetworkTests: TCP test failed to make a connection. $error")
                broadcastStatus(tcpTestNotification, TCP_TEST_STATUS, false)
            }
        }
    }

    fun testHTTP()
    {
//        // http://operatorfoundation.org/images/logo.jpg
//        //GET / HTTP/1.0
//        //\r\n\]r\n
//
//        URL("http://operatorfoundation.org/images/logo.jpg").run {
//            openConnection().run {
//                this as HttpURLConnection
//                val response = inputStream.bufferedReader().readText()
//                println("testHTTP received a response: $response")
//            }
//        }

        println("🌙 Launching HTTP Test")
        println("\uD83C\uDF19 host and port: 185.199.109.153: 80")

        thread(start = true)
        {
            try
            {
                val transmissionConnection = TransmissionConnection("185.199.109.153", 80, ConnectionType.TCP, null)
                println("🌙 HTTP test: Transmission Connection created.")

                transmissionConnection.write("GET / HTTP/1.0\r\n\r\n".toByteArray())
                println("🌙 HTTP test: Wrote some data...")

                val result = transmissionConnection.read(10)
                transmissionConnection.close()

                if (result == null)
                {
                    println("🌙 HTTP test tried to read, but got no response")
                    broadcastStatus(httpTestNotification, HTTP_TEST_STATUS, false)
                }
                else
                {
                    println("🌙 NetworkTests: HTTP test got a response (${result.size} bytes)): $result")

                    broadcastStatus(httpTestNotification, HTTP_TEST_STATUS, true)
                }
            }
            catch(error: Exception)
            {
                println("🌙 NetworkTests: HTTP test failed to make a connection. $error")
                broadcastStatus(tcpTestNotification, HTTP_TEST_STATUS, false)
            }
        }
    }

    fun testResolveDNS()
    {
        println("🌙 testResolveDNS called")
        thread(start = true)
        {
            val address = InetAddress.getByName("operatorfoundation.org")
            println("🌙 testResolveDNS: got an address from operatorfoundation.org: ${address.hostAddress}")

            if (address.hostAddress == "185.199.110.153")
            {
                println("🌙 testResolveDNS succeeded! ✨")
                broadcastStatus(dnsTestNotification, DNS_TEST_STATUS, true)
            }
            else
            {
                println("🌙 testResolveDNS returned an unexpected host address: ${address.hostAddress}")
                broadcastStatus(dnsTestNotification, DNS_TEST_STATUS, false)
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
