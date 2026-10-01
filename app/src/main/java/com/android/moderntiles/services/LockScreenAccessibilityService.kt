package com.android.moderntiles.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class LockScreenAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: LockScreenAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: service is exclusively utilized for global actions
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    /**
     * Executes the system global action to lock the device.
     * Available on API 28+ without requiring device administration permissions.
     */
    fun lockScreen(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }
}
