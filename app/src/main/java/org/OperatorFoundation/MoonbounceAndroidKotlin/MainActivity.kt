package org.OperatorFoundation.MoonbounceAndroidKotlin

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.view.View
import android.net.VpnService
import android.widget.Button
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import java.lang.Exception

val SERVER_PORT = "ServerPort"
val SERVER_IP = "ServerIP"

class MainActivity : AppCompatActivity()
{
    var ipAddress = ""
    var serverPort = 0
    lateinit var resultText: TextView

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result ->
        if (result.resultCode == Activity.RESULT_OK) // User has now given permission
        {
            // Start the VPN Service
            val vpnServiceIntent = Intent(this, MBAKVpnService::class.java)
            vpnServiceIntent.putExtra(SERVER_IP, ipAddress)
            vpnServiceIntent.putExtra(SERVER_PORT, serverPort)
            startService(vpnServiceIntent)
        }
        else
        {
            // Once this is a library, we may want to return an error and/or the result code to the calling application.
            print("Attempted to get VPN Service permissions from the user, but failed.")
            print(result.resultCode)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultText = findViewById<TextView>(R.id.resultText)

        val connectButton = findViewById<Button>(R.id.connect_button)
        val testTCPButton = findViewById<Button>(R.id.test_TCP)
        val testUDPButton = findViewById<Button>(R.id.test_UDP)

        connectButton.setOnClickListener {
            connectClicked(connectButton)
        }

        testTCPButton.setOnClickListener {
            testTCPClicked(testTCPButton)
        }

        testUDPButton.setOnClickListener {
            testUDPClicked(testUDPButton)
        }

    }

    // TODO: Display test results text

    fun testTCPClicked(v: View?)
    {
        print("Test TCP Clicked.")
        resultText.text = "Test TCP Clicked."
    }

    fun testUDPClicked(v: View?)
    {
        print("Test UDP Clicked.")
        resultText.text = "Test UDP Clicked."
    }

    fun connectClicked(v: View?)
    {
        println("Clicked the connect button")
        val ipEditText = findViewById<EditText>(R.id.server_address)
        ipAddress = ipEditText.text.toString()

        val portEditText = findViewById<EditText>(R.id.server_port)
        val serverPortString = portEditText.text.toString()

        if (ipAddress.isEmpty() || ipAddress.isBlank())
        {
            print("Can't connect without a valid IP Address")
        }
        else
        {
            try
            {
                serverPort = serverPortString.toInt()

                // Call VpnService.prepare() to ask for permission (when needed).
                val vpnPrepareIntent = VpnService.prepare(applicationContext)
                if (vpnPrepareIntent != null) // User has not yet given permission
                {

                    // Launches an activity to request permission
                    resultLauncher.launch(vpnPrepareIntent)
                }
                else // User has already given permission VPN Service is already prepared
                {
                    // Start the VPN Service
                    val vpnServiceIntent = Intent(this, MBAKVpnService::class.java)
                    vpnServiceIntent.putExtra(SERVER_IP, ipAddress)
                    vpnServiceIntent.putExtra(SERVER_PORT, serverPort)
                    startService(vpnServiceIntent)
                }
            }
            catch (error: Exception)
            {
                print(error)
            }
        }
    }
}