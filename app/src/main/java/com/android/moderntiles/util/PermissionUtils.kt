package com.android.moderntiles.util

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.android.moderntiles.services.LockScreenAccessibilityService

object PermissionUtils {

    /**
     * Checks if ModernTiles' LockScreenAccessibilityService is currently enabled.
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
}
