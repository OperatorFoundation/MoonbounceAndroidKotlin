package org.OperatorFoundation.MoonbounceAndroidKotlin

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.view.View
import android.net.VpnService
import android.widget.Button
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts

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
        // Check to see if the user has already given permission
        val vpnPrepareIntent = VpnService.prepare(applicationContext)
        if (vpnPrepareIntent != null) // User has not yet given permission
        {
            var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result ->
                if (result.resultCode == Activity.RESULT_OK) // User has now given permission
                {
                    // Start the VPN Service
                    val vpnServiceIntent = Intent(this, MBAKVpnService::class.java)
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
            startService(vpnServiceIntent)
        }
    }
}