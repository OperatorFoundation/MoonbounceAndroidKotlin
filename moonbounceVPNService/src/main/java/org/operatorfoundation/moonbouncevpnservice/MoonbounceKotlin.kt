package org.operatorfoundation.moonbouncevpnservice

import android.content.*

class MoonbounceKotlin(val context: Context, var ipAddress: String, var serverPort: Int, var disallowedApp: String? = null, var excludeRoute: String? = null)
{
    var vpnServiceIntent: Intent? = null
    var vpnStatusReceiver: BroadcastReceiver

    init {
        val filter = IntentFilter()
        filter.addAction(MBAKVpnService.vpnStatusNotification)
        vpnStatusReceiver = MoonbouncePluginStatusReceiver()

        context.registerReceiver(vpnStatusReceiver, filter)
    }

    fun startVPN(): ComponentName?
    {
        if (vpnServiceIntent == null)
        {
            vpnServiceIntent = Intent(context, MBAKVpnService::class.java)
        }

        println("MainActivity Server IP Address: $ipAddress")
        println("MainActivity Server Port: $serverPort")
        println("MainActivity Disallowed App: $disallowedApp")
        println("MainActivity Exclude Route: $excludeRoute")

        vpnServiceIntent!!.putExtra(SERVER_IP, ipAddress)
        vpnServiceIntent!!.putExtra(SERVER_PORT, serverPort)

        if (!disallowedApp.isNullOrEmpty())
        {
            vpnServiceIntent!!.putExtra(DISALLOWED_APP, disallowedApp)
        }

        if (!excludeRoute.isNullOrEmpty())
        {
            vpnServiceIntent!!.putExtra(EXCLUDE_ROUTE, excludeRoute)
        }

        // Start the VPN Service
        return context.startService(vpnServiceIntent)
    }

    fun stopVPN(): Boolean
    {
        return context.stopService(vpnServiceIntent)
    }
}