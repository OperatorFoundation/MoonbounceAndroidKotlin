package org.operatorfoundation.moonbouncevpnservice

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("org.operatorfoundation.moonbouncevpnservice.test", appContext.packageName)
    }

    @Test
    fun pluginStopService() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//        assertEquals("org.operatorfoundation.moonbouncevpnservice.test", appContext.packageName)

        val moonbounceKotlin = MoonbounceKotlin(appContext, "164.92.71.230", 1234)
        moonbounceKotlin.startVPN()
        moonbounceKotlin.stopVPN()
    }
}