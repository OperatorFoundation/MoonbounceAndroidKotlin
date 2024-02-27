package org.operatorfoundation.moonbounceAndroidKotlin

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import org.operatorfoundation.moonbouncevpnservice.APP_PACKAGE
import org.operatorfoundation.moonbouncevpnservice.DISALLOWED_APPS
import org.operatorfoundation.moonbouncevpnservice.EXCLUDE_ROUTES
import org.operatorfoundation.moonbouncevpnservice.MBAKVpnService
import org.operatorfoundation.moonbouncevpnservice.NetworkTests
import org.operatorfoundation.moonbouncevpnservice.SERVER_IP
import org.operatorfoundation.moonbouncevpnservice.SERVER_PORT
import org.operatorfoundation.moonbouncevpnservice.SERVER_PUBLIC_KEY
import org.operatorfoundation.moonbouncevpnservice.START_VPN_ACTION
import org.operatorfoundation.moonbouncevpnservice.STOP_VPN_ACTION
import org.operatorfoundation.moonbouncevpnservice.USE_PLUGGABLE_TRANSPORTS

class MainActivity : AppCompatActivity()
{
    val TAG = "MainActivity"
    val networkTests = NetworkTests(this)
    var ipAddress = "0.0.0.0"
    var serverPort = 1234
    var serverPublicKey: String? = null
    var disallowedApps: Array<String>? = null
    var excludeRoutes: Array<String>? = null
    var usePluggableTransports: Boolean = false
    var statusReceiver: BroadcastReceiver? = null

    lateinit var ipEditText: EditText
    lateinit var portEditText: EditText
    lateinit var serverPublicKeyEditText: EditText
    lateinit var disallowedAppEditText: EditText
    lateinit var excludeRouteEditText: EditText
    lateinit var resultText: TextView
    lateinit var chooseDisallowAppsButton: Button
    lateinit var testTCPButton: Button
    lateinit var testTCPConnectButton: Button
    lateinit var testTCPBigDataButton: Button
    lateinit var testUDPButton: Button
    lateinit var testHTTPButton: Button
    lateinit var testResolveDNSButton: Button
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
        serverPublicKeyEditText = findViewById(R.id.server_public_key)
        disallowedAppEditText = findViewById(R.id.disallowed_app)
        excludeRouteEditText = findViewById(R.id.exclude_route)
        chooseDisallowAppsButton = findViewById(R.id.installed_apps)
        testTCPButton = findViewById(R.id.test_TCP)
        testTCPConnectButton = findViewById(R.id.test_TCP_connect)
        testTCPBigDataButton = findViewById(R.id.test_TCP_Big_Data)
        testUDPButton = findViewById(R.id.test_UDP)
        testHTTPButton = findViewById(R.id.test_HTTP)
        testResolveDNSButton = findViewById(R.id.test_resolve_DNS)
        vpnConnectedSwitch = findViewById(R.id.connect_switch)
        pluggableTransportsSwitch = findViewById(R.id.pluggable_transports_switch)

        chooseDisallowAppsButton.setOnClickListener {
            chooseDisallowedApps()
        }

        vpnConnectedSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
            {
                pluggableTransportsSwitch.isClickable = false
                connectTapped()
            }
            else
            {
                pluggableTransportsSwitch.isClickable = true
                disconnectTapped()
            }
        }

        pluggableTransportsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
            {
                usePluggableTransports = true
                pluggableTransportsSwitch.text = "Using Pluggable Transports"
            }
            else
            {
                usePluggableTransports = false
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

        testHTTPButton.setOnClickListener{
            testHTTPTapped()
        }

        testResolveDNSButton.setOnClickListener {
            testResolveDNS()
        }
    }

    private fun configureReceiver()
    {
        val filter = IntentFilter()
        filter.addAction(MBAKVpnService.vpnStatusNotification)
        filter.addAction(MBAKVpnService.tcpTestNotification)
        filter.addAction(MBAKVpnService.udpTestNotification)
        filter.addAction(MBAKVpnService.httpTestNotification)
        filter.addAction(MBAKVpnService.dnsTestNotification)
        statusReceiver = StatusReceiver()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            registerReceiver(statusReceiver, filter, RECEIVER_NOT_EXPORTED)
        }
        else
        {
            registerReceiver(statusReceiver, filter)
        }
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
        }
    }

    private fun testTCPTapped()
    {
        ipAddress = ipEditText.text.toString()
        networkTests.host = ipAddress
        networkTests.tcpTest()
    }

    private fun testTCPConnectTapped()
    {
        ipAddress = ipEditText.text.toString()
        networkTests.host = ipAddress
        networkTests.tcpConnectTest()
    }

    private fun testTCPBigDataButtonTapped()
    {
        ipAddress = ipEditText.text.toString()
        networkTests.host = ipAddress
        networkTests.tcpTest2k()
    }

    private fun testUDPTapped()
    {
        ipAddress = ipEditText.text.toString()
        networkTests.host = ipAddress
        networkTests.udpTest()
    }

    private fun testHTTPTapped()
    {
        networkTests.testHTTP()
    }

    private fun testResolveDNS()
    {
        networkTests.testResolveDNS()
    }

    fun connectTapped()
    {
        if (ipEditText.text.toString().isBlank() || portEditText.text.toString().isBlank())
        {
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
        stopService(moonbounceVPNIntent)
        moonbounceVPNIntent.action = STOP_VPN_ACTION
        startService(moonbounceVPNIntent)
    }

    fun startVPNService()
    {
        // Set the action (start not stop)
        moonbounceVPNIntent.action = START_VPN_ACTION

        // Provide the activity that should receive the vpn status broadcasts
        moonbounceVPNIntent.putExtra(APP_PACKAGE, MainActivity::class.java)

        // Set the IP address of the vpn server (transport server if a transport is being used)
        ipAddress = ipEditText.text.toString()
        moonbounceVPNIntent.putExtra(SERVER_IP, ipAddress)
        println("MainActivity Server IP Address: $ipAddress")

        // Set the port of the vpn server (transport server if a transport is being used)
        serverPort = portEditText.text.toString().toInt()
        moonbounceVPNIntent.putExtra(SERVER_PORT, serverPort)
        println("MainActivity Server Port: $serverPort")

        // Add the app id's of any apps that should not use the VPN
        val selectedApps = disallowedAppEditText.text.toString()
        if (selectedApps.isNotEmpty())
        {
            disallowedApps = selectedApps.split(" ").toTypedArray()
            moonbounceVPNIntent.putExtra(DISALLOWED_APPS, disallowedApps)
            println("MainActivity Disallowed App: $disallowedApps")
        }

        // Add any routes that should be excluded from the VPN
        val routes = excludeRouteEditText.text.toString()
        if (routes.isNotEmpty())
        {
            excludeRoutes = routes.split(" ").toTypedArray()
            moonbounceVPNIntent.putExtra(EXCLUDE_ROUTES, excludeRoutes)
            println("MainActivity Exclude Routes: $excludeRoutes")
        }

        // Indicate whether or not pluggable transports should be used
        moonbounceVPNIntent.putExtra(USE_PLUGGABLE_TRANSPORTS, usePluggableTransports)
        println("MainActivity Using Pluggable Transports: $usePluggableTransports")

        // Currently only the shadow transport is supported
        // If opting to use a transport, provide the public key for that Shadow server
        if (usePluggableTransports)
        {
            serverPublicKey = serverPublicKeyEditText.text.toString()
            if (serverPublicKey != null) {
                moonbounceVPNIntent.putExtra(SERVER_PUBLIC_KEY, serverPublicKey)
                println("MainActivity Server Public Key: $serverPublicKey")
            }
        }

        // Start the VPN Service
        val startVPNResult = startService(moonbounceVPNIntent)
        if (startVPNResult != null)
        {
            vpnConnectedSwitch.isChecked = true
            vpnConnectedSwitch.text = "VPN connected"
        }
        else
        {
            vpnConnectedSwitch.isChecked = false
            vpnConnectedSwitch.text = "Connect VPN"
        }
    }
}
