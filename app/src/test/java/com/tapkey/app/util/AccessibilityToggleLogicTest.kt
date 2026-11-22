package com.tapkey.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccessibilityToggleLogicTest {

    @Test
    fun normalizeComponentName_expandsShorthandAndValidates() {
        assertEquals(
            "com.tapkey.app/com.tapkey.app.services.LockScreenAccessibilityService",
            PermissionUtils.normalizeComponentName("com.tapkey.app/.services.LockScreenAccessibilityService")
        )
        assertEquals(
            "com.x8bit.bitwarden/com.x8bit.bitwarden.Accessibility.AccessibilityService",
            PermissionUtils.normalizeComponentName(" com.x8bit.bitwarden/com.x8bit.bitwarden.Accessibility.AccessibilityService ")
        )
        assertNull(PermissionUtils.normalizeComponentName(""))
        assertNull(PermissionUtils.normalizeComponentName("invalid_without_slash"))
        assertNull(PermissionUtils.normalizeComponentName("/.MissingPackage"))
        assertNull(PermissionUtils.normalizeComponentName("com.tapkey.app/"))
    }

    @Test
    fun parseComponentStrings_handlesEmptyDuplicatesAndShorthand() {
        assertEquals(emptyList<String>(), PermissionUtils.parseComponentStrings(null))
        assertEquals(emptyList<String>(), PermissionUtils.parseComponentStrings(""))
        assertEquals(emptyList<String>(), PermissionUtils.parseComponentStrings("  :  : "))

        val raw = "com.tapkey.app/.services.LockScreenAccessibilityService:" +
            "com.x8bit.bitwarden/com.x8bit.bitwarden.Accessibility.AccessibilityService:" +
            "com.tapkey.app/com.tapkey.app.services.LockScreenAccessibilityService:"
        val expected = listOf(
            "com.tapkey.app/com.tapkey.app.services.LockScreenAccessibilityService",
            "com.x8bit.bitwarden/com.x8bit.bitwarden.Accessibility.AccessibilityService"
        )
        assertEquals(expected, PermissionUtils.parseComponentStrings(raw))
    }

    @Test
    fun mergeServicesForPause_preservesPreviouslyPausedAndAddsNewlyEnabled() {
        val previouslyPaused = setOf(
            "com.x8bit.bitwarden/com.x8bit.bitwarden.Accessibility.AccessibilityService",
            "com.tapkey.app/com.tapkey.app.services.LockScreenAccessibilityService"
        )
        val currentlyActive = listOf(
            "com.google.android.marvin.talkback/.TalkBackService",
            "com.tapkey.app/.services.LockScreenAccessibilityService"
        )

        val merged = PermissionUtils.mergeServicesForPause(currentlyActive, previouslyPaused)
        assertEquals(
            setOf(
                "com.x8bit.bitwarden/com.x8bit.bitwarden.Accessibility.AccessibilityService",
                "com.tapkey.app/com.tapkey.app.services.LockScreenAccessibilityService",
                "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService"
            ),
            merged
        )
    }

    @Test
    fun filterInstalledServices_dropsUninstalledAppsAndPreservesInstalled() {
        val saved = listOf(
            "com.x8bit.bitwarden/com.x8bit.bitwarden.Accessibility.AccessibilityService",
            "com.uninstalled.app/.DeletedService",
            "com.tapkey.app/.services.LockScreenAccessibilityService"
        )
        val installed = setOf(
            "com.tapkey.app/com.tapkey.app.services.LockScreenAccessibilityService",
            "com.x8bit.bitwarden/com.x8bit.bitwarden.Accessibility.AccessibilityService",
            "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService"
        )

        assertEquals(
            listOf(
                "com.x8bit.bitwarden/com.x8bit.bitwarden.Accessibility.AccessibilityService",
                "com.tapkey.app/com.tapkey.app.services.LockScreenAccessibilityService"
            ),
            PermissionUtils.filterInstalledServices(saved, installed)
        )
    }
}
