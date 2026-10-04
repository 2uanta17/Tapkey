package com.android.moderntiles.util

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TileManager {

    /**
     * Dynamically enables or disables a TileService component in the PackageManager.
     * Offloaded to Dispatchers.IO to prevent blocking the main thread during system Binder IPC.
     */
    suspend fun setTileEnabled(context: Context, serviceClass: Class<*>, enabled: Boolean) = withContext(Dispatchers.IO) {
        val componentName = ComponentName(context, serviceClass)
        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        context.packageManager.setComponentEnabledSetting(
            componentName,
            newState,
            PackageManager.DONT_KILL_APP
        )
    }

    /**
     * Prompts the system UI to add the tile to the Quick Settings panel using StatusBarManager.
     * Ensures the component is enabled in the PackageManager prior to sending the request.
     * Offloaded to Dispatchers.IO to eliminate UI jank.
     */
    suspend fun requestPinTile(
        context: Context,
        serviceClass: Class<*>,
        title: CharSequence,
        iconResId: Int,
        onResult: ((Int) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        // Ensure component is enabled first so SystemUI can find and bind it
        setTileEnabled(context, serviceClass, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val statusBarManager = context.getSystemService(StatusBarManager::class.java)
            val componentName = ComponentName(context, serviceClass)
            val icon = Icon.createWithResource(context, iconResId)

            statusBarManager?.requestAddTileService(
                componentName,
                title,
                icon,
                context.mainExecutor
            ) { result ->
                onResult?.invoke(result)
            }
        }
    }
}
