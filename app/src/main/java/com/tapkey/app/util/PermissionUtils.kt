package com.tapkey.app.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.text.TextUtils
import com.tapkey.app.services.LockScreenAccessibilityService

object PermissionUtils {

    /**
     * Checks if Tapkey's LockScreenAccessibilityService is currently enabled in system settings.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        if (LockScreenAccessibilityService.instance != null) {
            return true
        }

        val expectedComponentName = ComponentName(context, LockScreenAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        while (colonSplitter.hasNext()) {
            val componentStr = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentStr)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }

    /**
     * Checks if WRITE_SECURE_SETTINGS permission has been granted via ADB.
     */
    fun hasWriteSecureSettings(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Silently enables LockScreenAccessibilityService via Settings.Secure (Option B).
     * Requires WRITE_SECURE_SETTINGS permission.
     */
    fun setAccessibilityServiceEnabledViaSecureSettings(context: Context, enable: Boolean): Boolean {
        if (!hasWriteSecureSettings(context)) {
            return false
        }

        try {
            val componentName = ComponentName(context, LockScreenAccessibilityService::class.java).flattenToString()
            val currentServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""

            val serviceList = currentServices.split(':').filter { it.isNotBlank() }.toMutableList()

            if (enable) {
                if (!serviceList.contains(componentName)) {
                    serviceList.add(componentName)
                }
                Settings.Secure.putString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    serviceList.joinToString(":")
                )
                Settings.Secure.putInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    1
                )
            } else {
                serviceList.remove(componentName)
                Settings.Secure.putString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    serviceList.joinToString(":")
                )
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }
}
