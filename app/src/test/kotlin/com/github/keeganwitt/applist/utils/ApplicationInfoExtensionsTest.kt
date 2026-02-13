package com.github.keeganwitt.applist.utils

import android.content.pm.ApplicationInfo
import android.os.Bundle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ApplicationInfoExtensionsTest {

    @Test
    @Config(sdk = [35])
    fun `given SDK 35 and isArchived true, then isArchivedApp returns true`() {
        val appInfo = ApplicationInfo()
        val field = ApplicationInfo::class.java.getField("isArchived")
        field.set(appInfo, true)

        assertTrue(appInfo.isArchivedApp)
    }

    @Test
    @Config(sdk = [35])
    fun `given SDK 35 and isArchived false and metadata false, then isArchivedApp returns false`() {
        val appInfo = ApplicationInfo()
        val field = ApplicationInfo::class.java.getField("isArchived")
        field.set(appInfo, false)

        assertFalse(appInfo.isArchivedApp)
    }

    @Test
    @Config(sdk = [34])
    fun `given SDK 34 and metadata has archive key true, then isArchivedApp returns true`() {
        val appInfo = ApplicationInfo().apply {
            metaData = Bundle().apply {
                putBoolean("com.android.vending.archive", true)
            }
        }
        assertTrue(appInfo.isArchivedApp)
    }

    @Test
    @Config(sdk = [34])
    fun `given SDK 34 and metadata has archive key false, then isArchivedApp returns false`() {
        val appInfo = ApplicationInfo().apply {
            metaData = Bundle().apply {
                putBoolean("com.android.vending.archive", false)
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
