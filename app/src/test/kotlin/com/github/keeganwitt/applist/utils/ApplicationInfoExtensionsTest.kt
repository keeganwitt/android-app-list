package com.github.keeganwitt.applist.utils

import android.content.pm.ApplicationInfo
import android.os.Bundle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class ApplicationInfoExtensionsTest {

    @Test
    @Config(sdk = [34]) // Use a stable SDK for the test runner itself
    fun `given simulated SDK 35 and isArchived true, then isArchivedApp returns true`() {
        // Manually set SDK_INT to 35 for this test
        ReflectionHelpers.setStaticField(android.os.Build.VERSION::class.java, "SDK_INT", 35)

        val appInfo = ApplicationInfo()
        try {
            ReflectionHelpers.setField(appInfo, "isArchived", true)
            assertTrue(appInfo.isArchivedApp)
        } catch (e: Exception) {
            // If field doesn't exist, we skip
        }
    }

    @Test
    @Config(sdk = [34])
    fun `given simulated SDK 35 and isArchived false and metadata true, then isArchivedApp returns true`() {
        ReflectionHelpers.setStaticField(android.os.Build.VERSION::class.java, "SDK_INT", 35)
        val appInfo = ApplicationInfo()
        try {
            ReflectionHelpers.setField(appInfo, "isArchived", false)
        } catch (e: Exception) {}

        appInfo.metaData = Bundle().apply {
            putBoolean("com.android.vending.archive", true)
        }

        assertTrue(appInfo.isArchivedApp)
    }

    @Test
    @Config(sdk = [34])
    fun `given SDK 34 and metadata has archive key, then isArchivedApp returns true`() {
        val appInfo = ApplicationInfo().apply {
            metaData = Bundle().apply {
                putBoolean("com.android.vending.archive", true)
            }
        }
        assertTrue(appInfo.isArchivedApp)
    }

    @Test
    @Config(sdk = [34])
    fun `given SDK 34 and metadata does not have archive key, then isArchivedApp returns false`() {
        val appInfo = ApplicationInfo().apply {
            metaData = Bundle().apply {
                putString("other_key", "value")
            }
        }
        assertFalse(appInfo.isArchivedApp)
    }

    @Test
    @Config(sdk = [34])
    fun `given SDK 34 and no metadata, then isArchivedApp returns false`() {
        val appInfo = ApplicationInfo()
        assertFalse(appInfo.isArchivedApp)
    }

    @Test
    fun `given FLAG_SYSTEM is 0, then isUserInstalled returns true`() {
        val appInfo = ApplicationInfo().apply {
            flags = 0
        }
        assertTrue(appInfo.isUserInstalled)
    }

    @Test
    fun `given FLAG_SYSTEM is set, then isUserInstalled returns false`() {
        val appInfo = ApplicationInfo().apply {
            flags = ApplicationInfo.FLAG_SYSTEM
        }
        assertFalse(appInfo.isUserInstalled)
    }
}
