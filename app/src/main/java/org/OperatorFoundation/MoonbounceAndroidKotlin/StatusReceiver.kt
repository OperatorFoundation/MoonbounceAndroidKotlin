package org.operatorfoundation.moonbounceAndroidKotlin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.operatorfoundation.moonbouncevpnservice.MBAKVpnService

class StatusReceiver : BroadcastReceiver()
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
        else if (intent.action == MBAKVpnService.tcpTestNotification)
        {
            if (intent.hasExtra(MBAKVpnService.TCP_TEST_STATUS))
            {
                val success = intent.getBooleanExtra(MBAKVpnService.TCP_TEST_STATUS, false)

                statusString = if (success) {
                    "The TCP test was succesful."
                } else {
                    "The TCP test failed."
                }
            }
            else
            {
                statusString = "Received a TCP test notification that did not provide a status."
            }

        }
        else if (intent.action == MBAKVpnService.udpTestNotification)
        {
            if(intent.hasExtra(MBAKVpnService.UDP_TEST_STATUS))
            {
                val success = intent.getBooleanExtra(MBAKVpnService.UDP_TEST_STATUS, false)

                statusString = if (success) {
                    "The UDP test was succesful."
                } else {
                    "The UDP test failed."
                }
            }
            else
            {
                statusString = "Received a UDP test notification that did not provide a status."
            }
        }
        else if (intent.action == MBAKVpnService.dnsTestNotification)
        {
            if (intent.hasExtra(MBAKVpnService.DNS_TEST_STATUS))
            {
                val success = intent.getBooleanExtra(MBAKVpnService.DNS_TEST_STATUS, false)

                statusString = if (success) {
                    "The DNS test was succesful."
                } else {
                    "The DNS test failed."
                }
            }
            else
            {
                statusString = "Received a DNS test notification that did not provide a status."
            }
        }
        else if (intent.action == MBAKVpnService.httpTestNotification)
        {
            if (intent.hasExtra(MBAKVpnService.HTTP_TEST_STATUS))
            {
                val success = intent.getBooleanExtra(MBAKVpnService.HTTP_TEST_STATUS, false)

                statusString = if (success) {
                    "The HTTP test was succesful."
                } else {
                    "The HTTP test failed."
                }
            }
            else
            {
                statusString = "Received a DNS test notification that did not provide a status."
            }
        }
        else
        {
            statusString = "Received an unknown notification: ${intent.action}"
        }

        Toast.makeText(context, statusString, Toast.LENGTH_LONG).show()
    }
}

