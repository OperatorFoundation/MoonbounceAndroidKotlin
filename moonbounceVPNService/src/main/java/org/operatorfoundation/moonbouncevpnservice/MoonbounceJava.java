package org.operatorfoundation.moonbouncevpnservice;

import android.content.Context;

public class MoonbounceJava
{
    MoonbounceKotlin moonbounceKotlin;

    public MoonbounceJava(Context context, String serverIP, Integer serverPort, String disallowedApp, String excludeIP)
    {
        moonbounceKotlin = new MoonbounceKotlin(context, serverIP, serverPort, disallowedApp, excludeIP);
    }
    public void startVPN()
    {
        moonbounceKotlin.startVPN();
    }

    public void stopVPN()
    {
        moonbounceKotlin.stopVPN();
    }
}
