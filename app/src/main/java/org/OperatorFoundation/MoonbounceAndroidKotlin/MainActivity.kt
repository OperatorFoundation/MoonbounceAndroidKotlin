package org.operatorfoundation.moonbounceAndroidKotlin

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.VpnService
import android.widget.Button
import android.os.Bundle
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import java.lang.Exception
import org.operatorfoundation.moonbouncevpnservice.*

class MainActivity : AppCompatActivity()
{
    val networkTests = NetworkTests()
    var vpnServiceIntent: Intent? = null
    var ipAddress = "0.0.0.0"
    var serverPort = 1234
    val disallowedApp: String? = null
    val excludeRoute: String? = null
    lateinit var resultText: TextView

    // BroadcastReceiver to show VPN status updates

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

        networkTests.host = ipAddress

        val connectButton = findViewById<Button>(R.id.connect_button)
        val testTCPButton = findViewById<Button>(R.id.test_TCP)
        val testUDPButton = findViewById<Button>(R.id.test_UDP)
        val stopVPNButton = findViewById<Button>(R.id.stopVPN_button)

        connectButton.setOnClickListener {
            connectTapped()
        }

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
        resultText.text = "Test TCP Tapped."

       networkTests.tcpTest()
    }

    fun testUDPTapped()
    {
        println("Test UDP Clicked.")
        resultText.text = "Test UDP Tapped."

        networkTests.udpTest()
    }

    fun connectTapped()
    {
        println("Connect tapped.")
        val ipEditText = findViewById<EditText>(R.id.server_address)
        ipAddress = ipEditText.text.toString()

        val portEditText = findViewById<EditText>(R.id.server_port)
        val serverPortString = portEditText.text.toString()

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
}