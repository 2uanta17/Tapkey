package com.tapkey.app.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.atomic.AtomicReference

class LockScreenAccessibilityService : AccessibilityService() {

    companion object {
        private val instanceRef = AtomicReference<LockScreenAccessibilityService?>()

        val instance: LockScreenAccessibilityService?
            get() = instanceRef.get()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef.set(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: service exclusively executes global lock action
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        super.onDestroy()
        instanceRef.compareAndSet(this, null)
    }

    /**
     * Executes the system global action to lock the device instantly.
     */
    fun lockScreen(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }
}
