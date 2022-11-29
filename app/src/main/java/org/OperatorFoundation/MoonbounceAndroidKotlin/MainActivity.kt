package org.operatorfoundation.moonbounceAndroidKotlin

import android.app.Activity
import android.content.BroadcastReceiver
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.widget.Button
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import org.OperatorFoundation.MoonbounceAndroidKotlin.StatusReceiver
import org.OperatorFoundation.MoonbounceAndroidKotlin.TCPStatusReceiver
import org.OperatorFoundation.MoonbounceAndroidKotlin.UDPStatusReceiver
import java.lang.Exception
import org.operatorfoundation.moonbouncevpnservice.*

class MainActivity : AppCompatActivity()
{
    val broadcastAction = "org.operatorfoundation.moonbounceAndroidKotlin.status"
    val broadcastTCPAction = "org.operatorfoundation.moonbounceAndroidKotlin.tcp.status"
    val broadcastUDPAction = "org.operatorfoundation.moonbounceAndroidKotlin.udp.status"
    val networkTests = NetworkTests(this)
    var vpnServiceIntent: Intent? = null
    var ipAddress = "0.0.0.0"
    var serverPort = 1234
    var disallowedApp: String? = null
    var excludeRoute: String? = null
    var vpnStatusReceiver: BroadcastReceiver? = null
    var tcpStatusReceiver: BroadcastReceiver? = null
    var udpStatusReceiver: BroadcastReceiver? = null
    lateinit var ipEditText: TextView
    lateinit var resultText: TextView

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    {
            result ->

        if (result.resultCode == Activity.RESULT_OK) // User has granted the needed permissions
        {
            // Start the VPN Service
            if (vpnServiceIntent == null)
            {
                vpnServiceIntent = Intent(this, MBAKVpnService::class.java)
            }
            vpnServiceIntent!!.putExtra(SERVER_IP, ipAddress)
            vpnServiceIntent!!.putExtra(SERVER_PORT, serverPort)
            vpnServiceIntent!!.putExtra(DISALLOWED_APP, disallowedApp)
            vpnServiceIntent!!.putExtra(EXCLUDE_ROUTE, excludeRoute)

            // TODO: https://developer.android.com/reference/android/content/Context#bindService(android.content.Intent,%20android.content.ServiceConnection,%20int)
//            bindService(vpnServiceIntent, 0)
            startService(vpnServiceIntent)

            resultText.text ="Starting the VPN service."
        }
        else
        {
            println("Unable to launch VPN Service, the user did not grant the needed permissions.")
            println(result)

            resultText.text = "Unable to launch VPN Service, the user did not grant the needed permissions."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (vpnServiceIntent == null)
        {
            vpnServiceIntent = Intent(this, MBAKVpnService::class.java)
        }

        resultText = findViewById<TextView>(R.id.resultText)

        ipEditText = findViewById<EditText>(R.id.server_address)
        val connectButton = findViewById<Button>(R.id.connect_button)
        val testTCPButton = findViewById<Button>(R.id.test_TCP)
        val testUDPButton = findViewById<Button>(R.id.test_UDP)
        val stopVPNButton = findViewById<Button>(R.id.stopVPN_button)

        configureVPNReceiver()

        connectButton.setOnClickListener {
            connectTapped()
        }

        configureTCPReceiver()
        configureUDPReceiver()

        testTCPButton.setOnClickListener {
            testTCPClicked()
        }

        testUDPButton.setOnClickListener {
            testUDPTapped()
        }

        stopVPNButton.setOnClickListener {
            stopVPNButtonTapped()
        }

    }

    private fun configureVPNReceiver()
    {
        val filter = IntentFilter()
        filter.addAction(broadcastAction)
        vpnStatusReceiver = StatusReceiver()
        val broadcastPermission = ""
        // TODO: See if we can add the BroadcastPermission argument: https://developer.android.com/reference/android/content/Context#registerReceiver(android.content.BroadcastReceiver,%20android.content.IntentFilter,%20java.lang.String,%20android.os.Handler)
        registerReceiver(vpnStatusReceiver, filter)
    }

    private fun configureTCPReceiver()
    {
        val filter = IntentFilter()
        filter.addAction(broadcastTCPAction)
        tcpStatusReceiver = TCPStatusReceiver()
        // TODO: See if we can add the BroadcastPermission argument: https://developer.android.com/reference/android/content/Context#registerReceiver(android.content.BroadcastReceiver,%20android.content.IntentFilter,%20java.lang.String,%20android.os.Handler)
        registerReceiver(tcpStatusReceiver, filter)
    }

    private fun configureUDPReceiver()
    {
        val filter = IntentFilter()
        filter.addAction(broadcastUDPAction)
        udpStatusReceiver = UDPStatusReceiver()
        registerReceiver(udpStatusReceiver, filter)
    }

    fun stopVPNButtonTapped() {
        println("Stop VPN Clicked.")
        resultText.text = "Stop VPN Tapped."

        stopService(vpnServiceIntent)
    }

    override fun stopService(name: Intent?): Boolean
    {
        println("XXXXXXXXX STOP SERVICE CALLED!! XXXXXXXXX")

        return super.stopService(name)
    }

    fun testTCPClicked()
    {
        println("Test TCP Clicked.")

        ipAddress = ipEditText.text.toString()
        networkTests.host = ipAddress
        networkTests.tcpTest()
    }

    fun testUDPTapped()
    {
        println("Test UDP Clicked.")

        ipAddress = ipEditText.text.toString()
        networkTests.host = ipAddress
        networkTests.udpTest()
    }

    fun connectTapped()
    {
        println("Connect tapped.")
        ipAddress = ipEditText.text.toString()

        val portEditText = findViewById<EditText>(R.id.server_port)
        val serverPortString = portEditText.text.toString()

        val disallowedAppEditText = findViewById<EditText>(R.id.disallowed_app)
        disallowedApp = disallowedAppEditText.text.toString()

        val excludeRouteEditText = findViewById<EditText>(R.id.exclude_route)
        excludeRoute = excludeRouteEditText.text.toString()

        if (ipAddress.isEmpty() || ipAddress.isBlank())
        {
            println("A valid server IP and port are required to enable VPN services.")
            resultText.text = "A valid server IP are required to enable VPN services."
            return
        }
        else
        {
            try
            {
                serverPort = serverPortString.toInt()

                // VpnService.prepare() to ask for permission (when needed).
                val vpnPrepareIntent = VpnService.prepare(applicationContext)
                if (vpnPrepareIntent != null) // The user has not yet given the necessary permissions
                {
                    // Launches an activity to request permission
                    resultLauncher.launch(vpnPrepareIntent)
                }
                else // The user has already given permission, and the VPN Service is already prepared
                {
                    if (vpnServiceIntent == null)
                    {
                        vpnServiceIntent = Intent(this, MBAKVpnService::class.java)
                    }
                    // Start the VPN Service
                    // TODO: Implement exclude IP
                    // TODO: This should come from the user.

                    vpnServiceIntent!!.putExtra(SERVER_IP, ipAddress)
                    vpnServiceIntent!!.putExtra(SERVER_PORT, serverPort)
                    vpnServiceIntent!!.putExtra(DISALLOWED_APP, disallowedApp)
                    vpnServiceIntent!!.putExtra(EXCLUDE_ROUTE, excludeRoute)

                    // TODO: https://developer.android.com/reference/android/content/Context#bindService(android.content.Intent,%20android.content.ServiceConnection,%20int)
//            bindService(vpnServiceIntent, 0)

                    startService(vpnServiceIntent)
                }
            }
            catch (error: Exception)
            {
                val errorString = "There was an error creating the VPN Service: " + error.localizedMessage
                println("There was an error creating the VPN Service: " + error.localizedMessage)
                resultText.text = errorString
                return
            }
        }
    }

    override fun onDestroy()
    {
        super.onDestroy()
        unregisterReceiver(vpnStatusReceiver)
        unregisterReceiver(tcpStatusReceiver)
        unregisterReceiver(udpStatusReceiver)
    }
}