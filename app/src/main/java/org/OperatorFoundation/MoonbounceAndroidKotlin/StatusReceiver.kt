package org.OperatorFoundation.MoonbounceAndroidKotlin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class StatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        val isConnected = "moonbounceVpnConnected"
        var statusString = "Received a VPN status update."
        var connected = intent.getBooleanExtra(isConnected, false)

        statusString = if (connected)
        {
            "VPN connected."
        }
        else
        {
            "VPN failed to connect."
        }

        Toast.makeText(context, statusString, Toast.LENGTH_LONG).show()
    }
}