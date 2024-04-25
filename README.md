### The Operator Foundation

[Operator](https://operatorfoundation.org) makes useable tools to help people around the world with censorship, security, and privacy.

## The Moonbounce Project
The Moonbounce Project is an initiative covering several clients, servers, and libraries. The goal of the project is to provide a simple VPN service that integrates
Pluggable Transport technology. This allows the Moonbounce VPN to operate on network with restrictive Internet censorship that blocks VPN protocols such as OpenVPN
and Wireguard. This project, MoonbounceAndroidKotlin, is one of several components of the Moonbounce project.

# MoonbounceAndroidKotlin
MoonbounceAndroidKotlin (MBAK) is a demo application showing how to build an Android client, in the Kotlin programming language, for the Moonbounce VPN initiative.
This application is just a demo and is not meant to be used by end users. The target audience for this demo is other developers that want to make their own Moonbounce-compatible
VPN client for Android. As all of the Moonbounce code is licensed under an unrestrictive MIT Open Source license, anyone is free to use this source code to make their
own VPN client

Please note some differences between the Moonbounce VPN client and other VPN clients. First, the Moonbounce project does not use an existing legacy VPN
protocol such as OpenVPN or Wireguard. These protocols have unfortunately been implemented in such a way that it would be difficult to add Pluggable Transport support to them.
Instead, Moonbounce uses its own, very simple VPN protocol that just sends IPv4 packets encapsulated into a data stream, with only minimal length-prefix framing. All
other aspects of the protocol, such as encryption and authentication, are handled by the Pluggable Transports layer. Therefore, in order to run the Moonbounce client, you will
also need a compatible server. The associated server for the Moonbounce project is called [Persona](https://github.com/OperatorFoundation/Persona.git). Additionally, for each Pluggable
Transport used by the client, an associated Pluggable Transport server will also need to be run. The Persona server does not accept Pluggable Transport traffic directly, instead
the Pluggable Transport layer of the traffic is handled by a proxy server such as Shapeshifter Dispatcher, which forwards the Moonbounce traffic to the Persona server. 

## Using the Demo Application

In order to use the demo application, you must provide the IP and port of the server. There are two modes for testing purposes: with and without Pluggable Transports. If you want to test
without Pluggable Transports, provide the IP and port of the Persona server. If you want to test with Pluggable Transports, provide the IP and port of the Pluggable Transport server, such
as Shapeshifter Dispatcher. Please make sure that you open the correct ports on your firewalls. There are some built-in tests you can run inside of the demo application. You can also test
VPN functionality by leaving the demo application and going to a web browser application. Sites such as "whatismyip.com" can show you whether your traffic is going through the VPN or
directly to the Internet.

Important information about using the demo application:
- It runs on Android SDK levels 29-33 (Android release versions 10-13). While it may run on other versions of Android, please do not report bugs about that because other SDK versions are out of scope for the project.
- You need a Persona server and a Pluggable Transport server (such as Shapeshifter Dispatcher).
- For Pluggable Transports, we use the Shadow transport for the demo application. You will need to paste in the public key of the Shadow server.
- Also available to users are fields to exclude specific apps and a routes from using the VPN, in which case they will go directly to the Internet.
- During the initial connection the user will have to give the application permission to run in the background. Once permission is granted the user will see a key icon appear in the toolbar of the phone (To access the notifications pull down on the top of the phone screen). You are now connected to your VPN.

### Disallow App (Demo)

Should you prefer to disallow certain apps from using the VPN tunnel, enter the app IDs in the text field provided seperated by spaces. To determine the correct name of the application you want to disallow, the PRINT INSTALLED APPS button will print a list of the names and IDs of all the apps installed on the device you’re running to your IDE.

### Exclude Route (Demo)

Additionally, Moonbounce can exclude specific routes, enter the IP addresses you wish to exclude, seperated by spaces, in the text field provided.

## Building Your Own VPN Application

The demo application is just a starting place to build your own VPN application with Pluggable Transport support.

### Starting the VPN Service

You must make sure that you have requested (and have been granted) the correct permissions in order to start a VPN service. See the demo app for an example of how to do this.

To start the Moonbounce VPNService:

1) Create an intent with the MBAKVpnService class.
2) Set the intent action to START_VPN_ACTION.
3) Provide the server IP as an extra using the Moonbounce key words SERVER_IP.
4) Provide the port as an extra using the Moonbounce key word SERVER_PORT.
5) Call startService() with your intent.

```
// 1. Create the Intent
var moonbounceVPNIntent = Intent(this, MBAKVpnService::class.java)

// 2. Set the action (start not stop)
moonbounceVPNIntent.action = START_VPN_ACTION

// 3. Set the IP address of the vpn server (transport server if a transport is being used)
moonbounceVPNIntent.putExtra(SERVER_IP, "127.0.0.1") // Use your actual Persona server IP here

// 4. Set the port of the vpn server (transport server if a transport is being used)
moonbounceVPNIntent.putExtra(SERVER_PORT, 1111) // Use your actual Persona server port here

// 5. Start the VPN Service
startService(moonbounceVPNIntent)
```

### Split Proxy Usages

The Moonbounce library can be used to create two different split tunneling options. One that allows one or more applications to be disallowed from tunneling, and one that allows one or more routes (IP Addresses) to be excluded from tunneling. Split tunneling allows users to use two different network connections simultaneously. Users can determine which applications and/or routes they would like to exclude. Excluded apps and routes will connect to the internet in the usual way while all other traffic will be sent through the VPN tunnel. 

#### Disallow Applications from Using the VPN

Note: A minimum Android API of 26 is required to use this feature.

Moonbounce for Android allows users to enter applications to exclude from the VPN tunnel. This allows internet traffic for that application to be routed directly to the open network without passing through the VPN tunnel. 

To disallow applications from using the VPN tunnel, provide an array containing the application ID's (e.g. ‘com.android.chrome’) of the apps you wish to disallow, to the VPN library. This is done by adding it as an “extra” to the Moonbounce VPN Service intent before calling startService() with that intent. The demo application shows an example of how to do this correctly (please note that you must use the keys such as “DISALLOWED_APPS” that are shown in the demo application for the service to be able to find the values that you provide):

```
var disallowedApps: Array<String> = arrayOf("com.android.chrome")
var moonbounceVPNIntent = Intent(this, MBAKVpnService::class.java)
...
moonbounceVPNIntent.putExtra(DISALLOWED_APPS, disallowedApp)
startService(moonbounceVPNIntent)
```

The demo application has a button that prints the package names of all applications installed on the device. This can be used to find the package name of the application you wish to disallow. Use this in lieu of the “disallowedApp” in the example above.

#### Exclude Routes

Note: A minimum Android API of 33 is required to use this feature.

Moonbounce for Android allows users to enter a specific IP address to exclude from the VPN tunnel. This allows internet traffic destined for the specific IP address to be routed directly to the open network without passing through the VPN tunnel. 

To exclude a route from the VPN tunnel, provide an array of the IP addresses that should be excluded to the Moonbounce VPN library. This is done by adding it as an “extra” to the Moonbounce VPN Service intent before calling startService() with that intent. This is the same way that you would provide the vpn server’s IP address and port, as well as any applications that you wish to exclude. The demo application shows an example of how to do this correctly (please note that you must use the keys such as “EXCLUDE_ROUTES” that are shown in the demo application for the service to be able to find the values that you provide):

 ```
var excludeRoutes: Array<String> = arrayOf("8.8.8.8")
var moonbounceVPNIntent = Intent(this, MBAKVpnService::class.java)
...
moonbounceVPNIntent.putExtra(EXCLUDE_ROUTES, excludeRoutes)
startService(moonbounceVPNIntent)
```

### Pluggable Transports

Moonbounce for Android allows for the use of pluggable transports. (Currently only Shadow servers have been tested and are supported in the demo application)
1) Use the IP and Port of the transport server you wish to connect to (as mentioned above in the "Starting the VPN Service" section).
2) Provide the public key for the Shadow server
3) Toggle the "Use Pluggable Transports" switch.
4) Connect to VPN

```
// 1. Set the IP address of the vpn server (transport server)
moonbounceVPNIntent.putExtra(SERVER_IP, "127.0.0.1") // Use your actual Shadow server IP here

// 2. Set the port of the vpn server (transport server)
moonbounceVPNIntent.putExtra(SERVER_PORT, 1111) // Use your actual Shadow server port here

// 3. Provide the public key for that Shadow server
moonbounceVPNIntent.putExtra(SERVER_PUBLIC_KEY, serverPublicKey)

// 4. Indicate whether or not pluggable transports should be used
moonbounceVPNIntent.putExtra(USE_PLUGGABLE_TRANSPORTS, usePluggableTransports)

// 5. Start the VPN Service
startService(moonbounceVPNIntent)
```

