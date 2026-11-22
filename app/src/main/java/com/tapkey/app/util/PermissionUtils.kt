package com.tapkey.app.util

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.tapkey.app.services.LockScreenAccessibilityService

object PermissionUtils {

    /**
     * Normalizes a flattened component name string (expanding shorthand ".Class" to "pkg/pkg.Class")
     * so entries from Settings.Secure and AccessibilityServiceInfo match reliably.
     */
    fun normalizeComponentName(raw: String): String? {
        val trimmed = raw.trim()
        val slashIndex = trimmed.indexOf('/')
        if (slashIndex <= 0 || slashIndex >= trimmed.length - 1) {
            return null
        }
        val pkg = trimmed.substring(0, slashIndex).trim()
        val cls = trimmed.substring(slashIndex + 1).trim()
        if (pkg.isEmpty() || cls.isEmpty()) {
            return null
        }
        val fullClass = if (cls.startsWith(".")) "$pkg$cls" else cls
        return "$pkg/$fullClass"
    }

    /**
     * Parses a colon-separated Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES string
     * into distinct, normalized component strings while preserving order.
     */
    fun parseComponentStrings(rawSetting: String?): List<String> {
        if (rawSetting.isNullOrBlank()) {
            return emptyList()
        }
        return rawSetting.split(':')
            .mapNotNull { normalizeComponentName(it) }
            .distinct()
    }

    /**
     * Merges currently active services with any previously paused snapshot so manually
     * re-enabled services are captured without losing the original restore list.
     */
    fun mergeServicesForPause(
        currentlyActive: Collection<String>,
        previouslyPaused: Collection<String>
    ): Set<String> {
        return buildSet {
            previouslyPaused.mapNotNullTo(this) { normalizeComponentName(it) }
            currentlyActive.mapNotNullTo(this) { normalizeComponentName(it) }
        }
    }

    /**
     * Filters saved component strings against currently installed accessibility services
     * so uninstalled apps are skipped cleanly when restoring.
     */
    fun filterInstalledServices(
        savedServices: Collection<String>,
        installedServices: Set<String>
    ): List<String> {
        val normalizedInstalled = installedServices.mapNotNull { normalizeComponentName(it) }.toSet()
        return savedServices
            .mapNotNull { normalizeComponentName(it) }
            .filter { it in normalizedInstalled }
            .distinct()
    }

    /**
     * Returns all currently enabled accessibility services from Settings.Secure.
     */
    fun getEnabledAccessibilityServices(context: Context): List<String> {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return parseComponentStrings(enabledServices)
    }

    /**
     * Returns true if at least one accessibility service is currently enabled or running.
     */
    fun hasAnyActiveAccessibilityService(context: Context): Boolean {
        if (getEnabledAccessibilityServices(context).isNotEmpty()) {
            return true
        }
        val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)
        return accessibilityManager
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.isNotEmpty() == true
    }

    /**
     * Checks if Tapkey's LockScreenAccessibilityService is currently enabled in system settings.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        if (LockScreenAccessibilityService.instance != null) {
            return true
        }

        val expected = ComponentName(context, LockScreenAccessibilityService::class.java).flattenToString()
        val normalizedExpected = normalizeComponentName(expected) ?: return false
        return getEnabledAccessibilityServices(context).contains(normalizedExpected)
    }

    /**
     * Checks if WRITE_SECURE_SETTINGS permission has been granted via ADB.
     */
    fun hasWriteSecureSettings(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Silently enables or disables LockScreenAccessibilityService via Settings.Secure.
     * Requires WRITE_SECURE_SETTINGS permission.
     */
    fun setAccessibilityServiceEnabledViaSecureSettings(context: Context, enable: Boolean): Boolean {
        if (!hasWriteSecureSettings(context)) {
            return false
        }

        return try {
            val componentName = normalizeComponentName(
                ComponentName(context, LockScreenAccessibilityService::class.java).flattenToString()
            ) ?: return false
            val serviceList = getEnabledAccessibilityServices(context).toMutableList()

            if (enable) {
                if (!serviceList.contains(componentName)) {
                    serviceList.add(componentName)
                }
                writeEnabledServices(context, serviceList, enabledFlag = true)
            } else {
                serviceList.remove(componentName)
                writeEnabledServices(context, serviceList, enabledFlag = serviceList.isNotEmpty())
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Disables all active accessibility services via Settings.Secure.
     * Requires WRITE_SECURE_SETTINGS permission.
     */
    fun disableAllAccessibilityServices(context: Context): Boolean {
        if (!hasWriteSecureSettings(context)) {
            return false
        }
        return try {
            writeEnabledServices(context, emptyList(), enabledFlag = false)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Restores previously saved accessibility services that are still installed on the device.
     * Requires WRITE_SECURE_SETTINGS permission.
     */
    fun restoreAccessibilityServices(context: Context, savedServices: Collection<String>): Boolean {
        if (!hasWriteSecureSettings(context)) {
            return false
        }
        return try {
            val accessibilityManager = context.getSystemService(AccessibilityManager::class.java)
            val installed = accessibilityManager?.installedAccessibilityServiceList
                ?.mapNotNull { info ->
                    val serviceInfo = info.resolveInfo?.serviceInfo ?: return@mapNotNull null
                    normalizeComponentName("${serviceInfo.packageName}/${serviceInfo.name}")
                }
                ?.toSet()
                ?: emptySet()

            val servicesToRestore = if (installed.isNotEmpty()) {
                filterInstalledServices(savedServices, installed)
            } else {
                savedServices.mapNotNull { normalizeComponentName(it) }.distinct()
            }

            if (servicesToRestore.isNotEmpty()) {
                writeEnabledServices(context, servicesToRestore, enabledFlag = true)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun writeEnabledServices(context: Context, services: List<String>, enabledFlag: Boolean) {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            services.joinToString(":")
        )
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            if (enabledFlag) 1 else 0
        )
    }
}
