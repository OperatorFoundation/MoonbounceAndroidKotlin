package org.operatorfoundation.moonbounceAndroidKotlin

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import org.operatorfoundation.moonbouncevpnservice.DISALLOWED_APP
import org.operatorfoundation.moonbouncevpnservice.EXCLUDE_ROUTE
import org.operatorfoundation.moonbouncevpnservice.MBAKVpnService
import org.operatorfoundation.moonbouncevpnservice.NetworkTests
import org.operatorfoundation.moonbouncevpnservice.SERVER_IP
import org.operatorfoundation.moonbouncevpnservice.SERVER_PORT
import org.operatorfoundation.moonbouncevpnservice.START_VPN_ACTION
import org.operatorfoundation.moonbouncevpnservice.STOP_VPN_ACTION
import org.operatorfoundation.moonbouncevpnservice.USE_PLUGGABLE_TRANSPORTS

class MainActivity : AppCompatActivity()
{
    val TAG = "MainActivity"
    val networkTests = NetworkTests(this)
    var ipAddress = "0.0.0.0"
    var serverPort = 1234
    var disallowedApp: String? = null
    var excludeRoute: String? = null
    var usePluggableTransports: Boolean = false
    var statusReceiver: BroadcastReceiver? = null

    lateinit var ipEditText: EditText
    lateinit var portEditText: EditText
    lateinit var disallowedAppEditText: EditText
    lateinit var excludeRouteEditText: EditText
    lateinit var resultText: TextView
    lateinit var chooseDisallowAppsButton: Button
    lateinit var testTCPButton: Button
    lateinit var testTCPConnectButton: Button
    lateinit var testTCPBigDataButton: Button
    lateinit var testUDPButton: Button
    lateinit var vpnConnectedSwitch: SwitchCompat
    lateinit var pluggableTransportsSwitch: SwitchCompat
    lateinit var moonbounceVPNIntent: Intent

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    {
            result ->

        if (result.resultCode == Activity.RESULT_OK) // User has granted the needed permissions
        {
            startVPNService()

            resultText.text ="Starting the VPN service."
        }
        else
        {
            println("Unable to launch VPN Service, the user did not grant the needed permissions.")
            println(result)

            resultText.text = "Unable to launch VPN Service, the user did not grant the needed permissions."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate Called")
        setContentView(R.layout.activity_main)
        configureReceiver()

        moonbounceVPNIntent = Intent(this, MBAKVpnService::class.java)

        resultText = findViewById(R.id.resultText)
        ipEditText = findViewById(R.id.server_address)
        portEditText = findViewById(R.id.server_port)
        disallowedAppEditText = findViewById(R.id.disallowed_app)
        excludeRouteEditText = findViewById(R.id.exclude_route)
        chooseDisallowAppsButton = findViewById(R.id.installed_apps)
        testTCPButton = findViewById(R.id.test_TCP)
        testTCPConnectButton = findViewById(R.id.test_TCP_connect)
        testTCPBigDataButton = findViewById(R.id.test_TCP_Big_Data)
        testUDPButton = findViewById(R.id.test_UDP)
        vpnConnectedSwitch = findViewById(R.id.connect_switch)
        pluggableTransportsSwitch = findViewById(R.id.pluggable_transports_switch)

        chooseDisallowAppsButton.setOnClickListener {
            chooseDisallowedApps()
        }

        vpnConnectedSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
            {
                connectTapped()
            }
            else
            {
                disconnectTapped()
            }
        }

        pluggableTransportsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
            {
                usePluggableTransports()
                pluggableTransportsSwitch.text = "Using Pluggable Transports"
            }
            else
            {
                pluggableTransportsSwitch.text = "Use Pluggable Transports"
            }
        }

        testTCPButton.setOnClickListener {
            testTCPTapped()
        }

        testTCPConnectButton.setOnClickListener {
            testTCPConnectTapped()
        }

        testTCPBigDataButton.setOnClickListener {
            testTCPBigDataButtonTapped()
        }

        testUDPButton.setOnClickListener {
            testUDPTapped()
        }
    }

    private fun configureReceiver()
    {
        val filter = IntentFilter()
        filter.addAction(MBAKVpnService.vpnStatusNotification)
        filter.addAction(MBAKVpnService.tcpTestNotification)
        filter.addAction(MBAKVpnService.udpTestNotification)
        statusReceiver = StatusReceiver()
        // TODO: See if we can add the BroadcastPermission argument: https://developer.android.com/reference/android/content/Context#registerReceiver(android.content.BroadcastReceiver,%20android.content.IntentFilter,%20java.lang.String,%20android.os.Handler)
        registerReceiver(statusReceiver, filter)
    }

    // Use this to see a printed list of applications with the ID needed to disallow them from the tunnel
    private fun chooseDisallowedApps()
    {
        val appManager = AppManager(applicationContext)
        val installedApps = appManager.getApps()
        println("Installed applications:")
        for (app in installedApps)
        {
            println("\napp name - ${app.name}")
            println("app id - ${app.id}")
            println("is a system app - ${app.isSystem}")
            println("has an icon - ${app.icon != null}\n")
        }
    }

    private fun testTCPTapped()
    {
        println("Test TCP Clicked.")

        ipAddress = ipEditText.text.toString()
        networkTests.host = ipAddress
        networkTests.tcpTest()
    }

    private fun testTCPConnectTapped()
    {
        println("Test TCP Connect Clicked.")

        ipAddress = ipEditText.text.toString()
        networkTests.host = ipAddress
        networkTests.tcpConnectTest()
    }

    private fun testTCPBigDataButtonTapped()
    {
        println("Test TCP Big Data Clicked.")

        ipAddress = ipEditText.text.toString()
        networkTests.host = ipAddress
        networkTests.tcpTest2k()
    }

    private fun testUDPTapped()
    {
        println("Test UDP Clicked.")

        ipAddress = ipEditText.text.toString()
        networkTests.host = ipAddress
        networkTests.udpTest()
    }
    fun connectTapped()
    {
        println("Connect tapped.")
        ipAddress = ipEditText.text.toString()
        serverPort = portEditText.text.toString().toInt()
        disallowedApp = disallowedAppEditText.text.toString()
        excludeRoute = excludeRouteEditText.text.toString()

        if (ipAddress.isEmpty() || ipAddress.isBlank())
        {
            println("A valid server IP and port are required to enable VPN services.")
            resultText.text = "A valid server IP and port are required to enable VPN services."
            vpnConnectedSwitch.isChecked = false
            return
        }
        else
        {
            try
            {
                // VpnService.prepare() to ask for permission (when needed).
                val vpnPrepareIntent = VpnService.prepare(applicationContext)
                if (vpnPrepareIntent != null) // The user has not yet given the necessary permissions
                {
                    // Launches an activity to request permission
                    resultLauncher.launch(vpnPrepareIntent)
                    vpnConnectedSwitch.isChecked = false
                }
                else // The user has already given permission, and the VPN Service is already prepared
                {
                    startVPNService()
                    vpnConnectedSwitch.isChecked = true
                }
            }
            catch (error: Exception)
            {
                val errorString = "There was an error creating the VPN Service: " + error.localizedMessage
                println("There was an error creating the VPN Service: " + error.localizedMessage)
                resultText.text = errorString
                return
            }
        }
    }

    fun disconnectTapped()
    {
        vpnConnectedSwitch.text = "Connect VPN"
        println("User request to close the tunnel.")
        stopService(moonbounceVPNIntent)
        moonbounceVPNIntent.action = STOP_VPN_ACTION
        startService(moonbounceVPNIntent)
    }

    fun usePluggableTransports()
    {
        println("Using Pluggable Transports")
        usePluggableTransports = true
        // TODO: Not yet implemented.
    }

    fun startVPNService()
    {
        println("MainActivity Server IP Address: $ipAddress")
        println("MainActivity Server Port: $serverPort")
        println("MainActivity Disallowed App: $disallowedApp")
        println("MainActivity Exclude Route: $excludeRoute")
        println("MainActivity VPN Switched on: $vpnConnectedSwitch")
        println("MainActivity Using Pluggable Transports: ")

        moonbounceVPNIntent.putExtra(SERVER_IP, ipAddress)
        moonbounceVPNIntent.putExtra(SERVER_PORT, serverPort)
        moonbounceVPNIntent.putExtra(DISALLOWED_APP, disallowedApp)
        moonbounceVPNIntent.putExtra(EXCLUDE_ROUTE, excludeRoute)
        moonbounceVPNIntent.putExtra(USE_PLUGGABLE_TRANSPORTS, usePluggableTransports)
        moonbounceVPNIntent.action = START_VPN_ACTION

                // Start the VPN Service
        startService(moonbounceVPNIntent)
        vpnConnectedSwitch.text = "VPN connected"
    }
}
