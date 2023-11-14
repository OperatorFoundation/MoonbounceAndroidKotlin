# The Operator Foundation

[Operator](https://operatorfoundation.org) makes useable tools to help people around the world with censorship, security, and privacy.

## MoonbounceAndroidKotlin
Moonbounce is a VPN Service library for Android devices written in Kotlin. This VPN service uses the [Flower](https://github.com/OperatorFoundation/FlowerAndroid.git) protocol.
 
The server side companion to this library is [Persona](https://github.com/OperatorFoundation/Persona.git).

### Demo Application

A rudimentory demo application is included in this repository as an example of how to use this library. Install it on any Android device with platforms between 7.0 (API 24) through 12.0 (API 34). This is experimental software and is still under development.

- Moonbounce requires a [Persona](https://github.com/OperatorFoundation/Persona.git) server to be listening on an IP address and Port of your choosing. See the repository for instructions on how to run this server.

- The Moonbounce demo app allows the user to connect to the VPN. At a minimum, the user must enter the server IP and port for the Persona server, and then tap the toggle switch to connect to the VPN. Also available to users are the enter an app to disallow and a route to exclude button. Use these buttons to route internet traffic through your default ISP.

- During the initial connection the user will have to give the application permission to run in the background. Once permission is granted the user will see a key icon appear in the toolbar of the phone (To access the notifications pull down on the top of the phone screen). You are now connected to your VPN.

#### Disallow App

Should you prefer to disallow a certain app from being opened via your VPN service, enter that app name in the space provided. You must enter the canonical name of the application as the OS understands it. For example: com.android.chrome. To determine the correct name of the application you want to disallow, the PRINT INSTALLED APPS button will print a list of the names of all the apps downloaded on the device you’re running to your IDE.

#### Exclude Route

Additionally, Moonbounce can exclude a website's route if you can provide Moonbounce with the IP Address of the website. For example: 142.250.217.78 is a valid IP address for Google.com try entering this IP Address in the exclude route area, turn the VPN on, and Google will be accessed through your normal ISP.
