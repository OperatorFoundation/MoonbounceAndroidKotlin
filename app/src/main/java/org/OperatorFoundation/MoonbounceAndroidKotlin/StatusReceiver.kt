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

        statusString = if (connected) {
            "VPN connected."
        } else {
            "VPN failed to connect."
        }

        Toast.makeText(context, statusString, Toast.LENGTH_LONG).show()
    }
}

class TCPStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isSuccessful = "TCP Tests Successful!"
        var tcpStatusString = "Received a TCP status update."
        var success = intent.getBooleanExtra(isSuccessful, false)

        tcpStatusString = if (success)
        {
            "TCP Test is Successful!"
        }
        else
        {
            "TCP Test Not Successful!"
        }
        Toast.makeText(context, tcpStatusString, Toast.LENGTH_LONG).show()
    }
}

class UDPStatusReceiver : BroadcastReceiver() {

    override  fun onReceive(context: Context, intent: Intent) {
        val isSuccessful = "UDP Tests Successful!"
        var udpStatusString = "Received a UDP status update."
        var success = intent.getBooleanExtra(isSuccessful, false)

        udpStatusString = if (success)
        {
            "UDP Test is Successful!"
        }
        else
        {
            "UDP Test was NOT successful."
        }
        Toast.makeText(context, udpStatusString, Toast.LENGTH_LONG).show()
    }
}