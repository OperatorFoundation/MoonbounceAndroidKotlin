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
import org.operatorfoundation.moonbouncevpnservice.*

class MainActivity : AppCompatActivity()
{
    val TAG = "MainActivity"
    val networkTests = NetworkTests(this)
    val mbakVpnService = MBAKVpnService()
    var vpnServiceIntent: Intent? = null
    var ipAddress = "0.0.0.0"
    var serverPort = 1234
    var disallowedApp: String? = null
    var excludeRoute: String? = null
    var statusReceiver: BroadcastReceiver? = null

    lateinit var ipEditText: TextView
    lateinit var resultText: TextView

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

        if (vpnServiceIntent == null)
        {
            vpnServiceIntent = Intent(this, MBAKVpnService::class.java)
        }

        resultText = findViewById<TextView>(R.id.resultText)

        ipEditText = findViewById<EditText>(R.id.server_address)
        val chooseDisallowAppsButton = findViewById<Button>(R.id.choose_apps)
        val vpnConnectedSwitchCompat = findViewById<SwitchCompat>(R.id.connect_switch)
        vpnConnectedSwitchCompat.isChecked = false
        val testTCPButton = findViewById<Button>(R.id.test_TCP)
        val testUDPButton = findViewById<Button>(R.id.test_UDP)

        chooseDisallowAppsButton.setOnClickListener {
            chooseDisallowedApps()
        }

        vpnConnectedSwitchCompat.setOnCheckedChangeListener {
                _, isChecked ->
            if (isChecked) {
                vpnConnectedSwitchCompat.text = "On"
                connectTapped()
            } else {
                vpnConnectedSwitchCompat.text = "Off"
                stopVPNTapped()
            }
        }

        testTCPButton.setOnClickListener {
            testTCPTapped()
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

    fun chooseDisallowedApps() {
        val appManager = AppManager(applicationContext)
        val installedApps = appManager.getApps()
        println("Printint installed app information:")
        for (app in installedApps)
        {
            println("\napp name - ${app.name}")
            println("app id - ${app.id}")
            println("is a system app - ${app.isSystem}")
            println("has an icon - ${app.icon != null}\n")
        }

    }

    fun stopVPNTapped() {
        println("Stop VPN Clicked.")
        resultText.text = "Stop VPN Tapped."

        stopService(vpnServiceIntent)
    }



    override fun stopService(name: Intent?): Boolean
    {
        println("XXXXXXXXX STOP SERVICE CALLED!! XXXXXXXXX")

        if (vpnServiceIntent == null)
        {
            print("There is no VPN service to stop.")
            return false
        }
        else
        {
            val serviceStopped = super.stopService(vpnServiceIntent)
//            stopVPN()
            mbakVpnService.stopVPN()
            //topForeground(/* removeNotification = */ true)
            // TODO: We are reaching these functions, but the service does not stop. Work on debugging.
            print("$name Service Stopped: $serviceStopped")
            return serviceStopped
        }
    }

    fun testTCPTapped()
    {
        println("Test TCP Clicked.")

        ipAddress = ipEditText.text.toString()
        networkTests.host = ipAddress
        networkTests.tcpTest()
    }

    fun testUDPTapped()
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

        val portEditText = findViewById<EditText>(R.id.server_port)
        val serverPortString = portEditText.text.toString()

        val disallowedAppEditText = findViewById<EditText>(R.id.disallowed_app)
        disallowedApp = disallowedAppEditText.text.toString()

        val excludeRouteEditText = findViewById<EditText>(R.id.exclude_route)
        excludeRoute = excludeRouteEditText.text.toString()

        if (ipAddress.isEmpty() || ipAddress.isBlank())
        {
            println("A valid server IP and port are required to enable VPN services.")
            resultText.text = "A valid server IP are required to enable VPN services."
            return
        }
        else
        {
            try
            {
                serverPort = serverPortString.toInt()

                // VpnService.prepare() to ask for permission (when needed).
                val vpnPrepareIntent = VpnService.prepare(applicationContext)
                if (vpnPrepareIntent != null) // The user has not yet given the necessary permissions
                {
                    // Launches an activity to request permission
                    resultLauncher.launch(vpnPrepareIntent)
                }
                else // The user has already given permission, and the VPN Service is already prepared
                {
                    startVPNService()
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

    override fun onDestroy()
    {
        println("✋ onDestroy called ✋")
        Log.d(TAG, "onDestroy Called")
        super.onDestroy()
        stopService(vpnServiceIntent)
        unregisterReceiver(statusReceiver)
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop Called")
        unregisterReceiver(statusReceiver)
    }

    fun startVPNService() {
        if (vpnServiceIntent == null)
        {
            vpnServiceIntent = Intent(this, MBAKVpnService::class.java)
        }

        println("MainActivity Server IP Address: $ipAddress")
        println("MainActivity Server Port: $serverPort")
        println("MainActivity Disallowed App: $disallowedApp")
        println("MainActivity Exclude Route: $excludeRoute")

        vpnServiceIntent!!.putExtra(SERVER_IP, ipAddress)
        vpnServiceIntent!!.putExtra(SERVER_PORT, serverPort)
        vpnServiceIntent!!.putExtra(DISALLOWED_APP, disallowedApp)
        vpnServiceIntent!!.putExtra(EXCLUDE_ROUTE, excludeRoute)

        // Start the VPN Service
        startService(vpnServiceIntent)
    }
}
