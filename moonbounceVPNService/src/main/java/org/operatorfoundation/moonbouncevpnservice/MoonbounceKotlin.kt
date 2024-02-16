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
            vpnServiceIntent!!.putExtra(DISALLOWED_APPS, disallowedApp)
        }

        if (!excludeRoute.isNullOrEmpty())
        {
            vpnServiceIntent!!.putExtra(EXCLUDE_ROUTES, excludeRoute)
        }

        // Start the VPN Service
        vpnServiceIntent!!.action = START_VPN_ACTION
        return context.startService(vpnServiceIntent)
    }

    fun stopVPN(): Boolean
    {
        if (vpnServiceIntent == null)
        {
            // The intent doesn't exist so there is nothing to stop
            return false
        }

        vpnServiceIntent!!.action = STOP_VPN_ACTION
        context.startService(vpnServiceIntent)

        return context.stopService(vpnServiceIntent)
    }
}