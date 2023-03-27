package org.operatorfoundation.moonbouncevpnservice;

import android.content.ComponentName;
import android.content.Context;

public class MoonbounceJava
{
    MoonbounceKotlin moonbounceKotlin;

    public MoonbounceJava(Context context, String serverIP, Integer serverPort, String disallowedApp, String excludeIP)
    {
        moonbounceKotlin = new MoonbounceKotlin(context, serverIP, serverPort, disallowedApp, excludeIP);
    }
    public ComponentName startVPN()
    {
        return moonbounceKotlin.startVPN();
    }

    public Boolean stopVPN()
    {
        return moonbounceKotlin.stopVPN();
    }
}
