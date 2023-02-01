package org.operatorfoundation.moonbounceAndroidKotlin

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.content.Context

class AppManager(val context: Context)
{
    fun getApps(): List<App>
    {
        print("Fetching apps")
        val installed = try {
            context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.packageName != context.packageName }
        } catch (ex: Exception) {
            print("Could not fetch apps, error: $ex")
            emptyList<ApplicationInfo>()
        }

        val apps = installed.mapNotNull {
            try {
                App(
                    id = it.packageName,
                    name = context.packageManager.getApplicationLabel(it).toString(),
                    isSystem = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    icon = context.packageManager.getApplicationIcon(context.packageManager.getApplicationInfo(it.packageName, PackageManager.GET_META_DATA))
                )
            } catch (ex: Exception) {
                print("Failed to get information for an app, error: $ex")
                null
            }
        }
        print("Found ${apps.size} apps")
        return apps
    }
}

typealias AppId = String

class App(
    val id: AppId,
    val name: String,
    val isSystem: Boolean,
    val icon: Drawable?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as App

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}