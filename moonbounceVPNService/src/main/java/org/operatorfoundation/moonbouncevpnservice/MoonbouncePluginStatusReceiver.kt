package org.operatorfoundation.moonbouncevpnservice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class MoonbouncePluginStatusReceiver: BroadcastReceiver()
{
    override fun onReceive(context: Context, intent: Intent)
    {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        val statusString: String

        if (intent.action == MBAKVpnService.vpnStatusNotification)
        {
            if (intent.hasExtra(MBAKVpnService.VPN_CONNECTED_STATUS))
            {
                val connected = intent.getBooleanExtra(MBAKVpnService.VPN_CONNECTED_STATUS, false)

                statusString = if (connected) {
                    "VPN connected."
                } else {
                    "VPN failed to connect."
                }
            }
            else
            {
                statusString = "Received a VPN connection notification that did not provide a status."
            }
        }
        else
        {
            statusString = "Received an unknown notification: ${intent.action}"
        }

        Toast.makeText(context, statusString, Toast.LENGTH_LONG).show()
    }
}