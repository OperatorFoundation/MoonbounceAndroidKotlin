package org.OperatorFoundation.MoonbounceAndroidKotlin

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.view.View
import android.net.VpnService
import android.widget.Button
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts

class MoonbounceAndroidKotlinVpnService : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val connectButton = findViewById<Button>(R.id.connect_button)

        connectButton.setOnClickListener {

            onClick(connectButton)
        }
    }

    fun onClick(v: View?) {
        println("Clicked the connect button")
        val prepareIntent = VpnService.prepare(applicationContext)
        if (prepareIntent != null) {
            var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val startIntent = Intent(this, MoonbounceAndroidKotlinVpnService::class.java)
                    startService(startIntent)
                }
            }
            resultLauncher.launch(prepareIntent)
        } else {
            val startIntent = Intent(this, MoonbounceAndroidKotlinVpnService::class.java)
            startService(startIntent)
        }
    }
}