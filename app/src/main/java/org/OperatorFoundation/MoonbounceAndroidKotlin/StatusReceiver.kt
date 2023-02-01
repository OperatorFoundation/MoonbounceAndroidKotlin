package org.operatorfoundation.moonbounceAndroidKotlin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.operatorfoundation.moonbouncevpnservice.MBAKVpnService

class StatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        var statusString = "Received a status update."

        if (intent.action == MBAKVpnService.vpnStatusNotification)
        {
            val connected = intent.getBooleanExtra(MBAKVpnService.VPN_CONNECTED_STATUS, false)

            statusString = if (connected) {
                "VPN connected."
            } else {
                "VPN failed to connect."
            }
        }
        else if (intent.action == MBAKVpnService.tcpTestNotification)
        {
            val success = intent.getBooleanExtra(MBAKVpnService.TCP_TEST_STATUS, false)

            statusString = if (success) {
                "The TCP test was succesful."
            } else {
                "The TCP test failed."
            }
        }
        else if (intent.action == MBAKVpnService.udpTestNotification)
        {
            val success = intent.getBooleanExtra(MBAKVpnService.TCP_TEST_STATUS, false)

            statusString = if (success) {
                "The UDP test was succesful."
            } else {
                "The UDP test failed."
            }
        }
        else
        {
            statusString = "Received an unknown notification: ${intent.action}"
        }

        Toast.makeText(context, statusString, Toast.LENGTH_LONG).show()
    }
}

