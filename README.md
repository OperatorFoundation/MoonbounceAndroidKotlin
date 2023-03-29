# The Operator Foundation

[Operator](https://operatorfoundation.org) makes useable tools to help people around the world with censorship, security, and privacy.

## MoonbounceAndroidKotlin (MBAK)
Moonbounce is a Kotlin VPN Service library for Android devices. This VPN service uses the [Flower](https://github.com/OperatorFoundation/FlowerAndroid.git) protocol.
 
The server side companion to this library is [Persona](https://github.com/OperatorFoundation/Persona.git).

MBAK is a graphical user interface for configuring a VPN with Pluggable Transport support. The goal of MBAK is to provide a usability-focused, streamlined user experience to using PT-enabled VPNs.

### Requirements

| Platform | Minimum Swift Version | Status |
| --- | --- | --- |
| Android 4 | API 30 | Building But Unsupported |

### Installation

```
git clone git@github.com:OperatorFoundation/MoonbounceAndroidKotlin.git 
```

***(NOTE: It is recommended that you use the SSH option when cloning this repository.)***

### Developer Instructions

1) Open AndroidStudio -> File -> Open -> go to the directory where you have saved this repository, and open it. 

2) MBAK requires a [Persona](https://github.com/OperatorFoundation/Persona.git) server to be listening on an IP address and Port of your choosing. See the repository for instructions on how to run this server. Continue with step 3 once you have seen the following print in your Persona Server run log if running Persona in Linux:

```
...
listening on 127.0.0.1 2121
Waiting to accept a connection
...
```

3) Return to Android Studio and run the MBAK application. A screen will appear allowing the user to enter the server IP address, the port, an app to disallow, and a route to exclude. At a minimum the user must enter the server IP and port information, and then click the toggle switch to connect to the VPN. 

4) During the initial connection the user will have to give the application permission to run in the background. Once permission is granted the user will see a key icon appear in the toolbar of the phone. To access the notifications pull down on the top of the phone screen. You are now connected to your VPN.

5) Should you prefer to disallow a certain app from being opened via your VPN service, enter that app name in the space provided. You must enter the canonical name of the application as the OS understands it. To determine the correct name of the application you want to disallow, press the purple DISALLOW APPS button. This will display a list of the names of all the apps downloaded on the device you’re running.

7) Additionally, MBAK can exclude a website's route if you can provide MBAK with the IP Address of the website. For example: 142.250.217.78 is a valid IP address for Google.com try entering this IP Address in the exclude route area, turn the VPN on, and Google will be inaccessible or accessed through your normal ISP.


### Running the application

1) Open your Android phone. 

2) Choose the MBAK App

3) Open the application, enter the details necessary to access your VPN server. 
