package com.github.keeganwitt.applist.utils

import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApplicationInfoExtensionsTest {

    @Test
    fun `given SDK 34 and metadata has archive key, then isArchivedApp returns true`() {
        val appInfo = ApplicationInfo()
        appInfo.metaData = Bundle().apply {
            putBoolean("com.android.vending.archive", true)
        }
        assertTrue(appInfo.isArchivedApp)
    }

    @Test
    fun `given SDK 34 and no metadata, then isArchivedApp returns false`() {
        val appInfo = ApplicationInfo()
        appInfo.metaData = null
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

    @Test
    fun `given simulated SDK 35, then it handles isArchived property and fallback`() {
        val originalSdk = Build.VERSION.SDK_INT
        try {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 35)

            // Case 1: isArchived field exists and is true
            val appInfo1 = ApplicationInfo()
            var fieldSetSucceeded = false
            try {
                ReflectionHelpers.setField(appInfo1, "isArchived", true)
                fieldSetSucceeded = true
            } catch (e: Exception) {}

            if (fieldSetSucceeded) {
                assertTrue(appInfo1.isArchivedApp)
            }

            // Case 2: isArchived is false (or field missing), check metadata fallback
            val appInfo2 = ApplicationInfo()
            try {
                ReflectionHelpers.setField(appInfo2, "isArchived", false)
            } catch (e: Exception) {}
            appInfo2.metaData = Bundle().apply { putBoolean("com.android.vending.archive", true) }
            assertTrue(appInfo2.isArchivedApp)

            // Case 3: Both false
            val appInfo3 = ApplicationInfo()
            try {
                ReflectionHelpers.setField(appInfo3, "isArchived", false)
            } catch (e: Exception) {}
            appInfo3.metaData = null
            assertFalse(appInfo3.isArchivedApp)

        } finally {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", originalSdk)
        }
    }
}
