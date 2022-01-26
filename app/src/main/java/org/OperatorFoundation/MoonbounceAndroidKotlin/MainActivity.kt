package org.OperatorFoundation.MoonbounceAndroidKotlin

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.view.View
import android.net.VpnService
import android.widget.Button
import android.os.Bundle

class MoonbounceAndroidKotlinVpnService : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val connectButton = findViewById<Button>(R.id.connect_button)

        connectButton.setOnClickListener {
            // Verify the connectButton parameter is correct.
            onClick(connectButton)
        }
    }

    fun onClick(v: View?) {
        val intent = VpnService.prepare(applicationContext)
        if (intent != null) {
            startActivityForResult(intent, 0)
        } else {
            onActivityResult(0, RESULT_OK, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == AppCompatActivity.RESULT_OK) {
            val intent = Intent(this, MoonbounceAndroidKotlinVpnService::class.java)
            startService(intent)
        }
    }
}