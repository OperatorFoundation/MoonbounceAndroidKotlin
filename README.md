# The Operator Foundation

[Operator](https://operatorfoundation.org) makes useable tools to help people around the world with censorship, security, and privacy.

# MoonbounceAndroidKotlin
Moonbounce is a VPN Service library for Android devices written in Kotlin. This VPN service uses the [Flower](https://github.com/OperatorFoundation/FlowerAndroid.git) protocol.
 
The server side companion to this library is [Persona](https://github.com/OperatorFoundation/Persona.git).

## Split Proxy Usages

Note: A minimum Android API of 26 is required to use the Proxy feature of this library.

The Moonbounce library can be used to create two different split tunneling options. One that allows one or more applications to be disallowed from tunneling, and one that allows one or more routes (IP Addresses) to be excluded from tunneling. Split tunneling allows users to use two different network connections simultaneously. Users can determine which applications and/or routes they would like to exclude. Excluded apps and routes will connect to the internet in the usual way while all other traffic will be sent through the VPN tunnel. 

### Disallowed App

Moonbounce for Android allows users to enter a specific application to exclude from the VPN tunnel. This allows internet traffic for that application to be routed directly to the open network without passing through the VPN tunnel. 

To disallow an application from using the VPN tunnel, provide the package name of the application (e.g. ‘com.android.chrome’) to be disallowed, to the VPN library. This is done by adding it as an “extra” to the Moonbounce VPN Service intent before calling startService() with that intent. The demo application shows an example of how to do this correctly (please note that you must use the keys such as “DISALLOWED_APP” that are shown in the demo application for the service to be able to find the values that you provide):

`moonbounceVPNIntent.putExtra(DISALLOWED_APP, disallowedApp)`

The demo application has a button that prints the package names of all applications installed on the device. This can be used to find the package name of the application you wish to disallow. Use this in lieu of the “disallowedApp” in the example above.

### Exclude Route

Note: A minimum Android API of 33 is required to use this feature.

Moonbounce for Android allows users to enter a specific IP address to exclude from the VPN tunnel. This allows internet traffic destined for the specific IP address to be routed directly to the open network without passing through the VPN tunnel. 

To exclude a route from the VPN tunnel, provide the IP address that should be excluded to the Moonbounce VPN library. This is done by adding it as an “extra” to the Moonbounce VPN Service intent before calling startService() with that intent. This is the same way that you would provide the vpn server’s IP address, or any application that you wish to also exclude. The demo application shows an example of how to do this correctly (please note that you must use the keys such as “EXCLUDE_ROUTE” that are shown in the demo application for the service to be able to find the values that you provide):

 `moonbounceVPNIntent.putExtra(EXCLUDE_ROUTE, excludeRoute)`

## Demo Application

A rudimentory demo application is included in this repository as an example of how to use this library. Install it on any Android device with platforms between 7.0 (API 24) through 12.0 (API 34). This is experimental software and is still under development.

- Moonbounce requires a [Persona](https://github.com/OperatorFoundation/Persona.git) server to be listening on an IP address and Port of your choosing. See the repository for instructions on how to run this server.

- The Moonbounce demo app allows the user to connect to the VPN. At a minimum, the user must enter the server IP and port for the Persona server, and then tap the toggle switch to connect to the VPN. Also available to users are the enter an app to disallow and a route to exclude button. Use these buttons to route internet traffic through your default ISP.

- During the initial connection the user will have to give the application permission to run in the background. Once permission is granted the user will see a key icon appear in the toolbar of the phone (To access the notifications pull down on the top of the phone screen). You are now connected to your VPN.

#### Disallow App (Demo)

Should you prefer to disallow a certain app from being opened via your VPN service, enter that app name in the space provided. You must enter the canonical name of the application as the OS understands it. For example: com.android.chrome. To determine the correct name of the application you want to disallow, the PRINT INSTALLED APPS button will print a list of the names of all the apps downloaded on the device you’re running to your IDE.

#### Exclude Route (Demo)

Additionally, Moonbounce can exclude a website's route if you can provide Moonbounce with the IP Address of the website. For example: 142.250.217.78 is a valid IP address for Google.com try entering this IP Address in the exclude route area, turn the VPN on, and Google will be accessed through your normal ISP.
