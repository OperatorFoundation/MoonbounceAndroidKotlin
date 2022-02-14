package org.OperatorFoundation.MoonbounceAndroidKotlin

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.view.View
import android.net.VpnService
import android.widget.Button
import android.os.Bundle
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import java.lang.Exception

val SERVER_PORT = "ServerPort"
val SERVER_IP = "ServerIP"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val connectButton = findViewById<Button>(R.id.connect_button)

        connectButton.setOnClickListener {

            onClick(connectButton)
        }
    }

    fun onClick(v: View?)
    {
        println("Clicked the connect button")
        val ipEditText = findViewById<EditText>(R.id.server_address)
        val ipAddress = ipEditText.text.toString()

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
                val serverPort = serverPortString.toInt()

                // Call VpnService.prepare() to ask for permission (when needed).
                val vpnPrepareIntent = VpnService.prepare(applicationContext)
                if (vpnPrepareIntent != null) // User has not yet given permission
                {
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