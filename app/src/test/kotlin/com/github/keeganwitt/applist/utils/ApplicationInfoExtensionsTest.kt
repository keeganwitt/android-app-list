package com.github.keeganwitt.applist.utils

import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApplicationInfoExtensionsTest {

    @Before
    fun setup() {
        mockkStatic(Build.VERSION::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Build.VERSION::class)
    }

    @Test
    fun `given SDK 35 and isArchived true, then isArchivedApp returns true`() {
        every { Build.VERSION.SDK_INT } returns 35
        val appInfo = mockk<ApplicationInfo>()
        every { appInfo.isArchived } returns true

        assertTrue(appInfo.isArchivedApp)
    }

    @Test
    fun `given SDK 35 and isArchived false and metadata true, then isArchivedApp returns true`() {
        every { Build.VERSION.SDK_INT } returns 35
        val appInfo = mockk<ApplicationInfo>()
        every { appInfo.isArchived } returns false
        val bundle = mockk<Bundle>()
        every { bundle.containsKey("com.android.vending.archive") } returns true
        every { appInfo.metaData } returns bundle

        assertTrue(appInfo.isArchivedApp)
    }

    @Test
    fun `given SDK 35 and isArchived false and no metadata, then isArchivedApp returns false`() {
        every { Build.VERSION.SDK_INT } returns 35
        val appInfo = mockk<ApplicationInfo>()
        every { appInfo.isArchived } returns false
        every { appInfo.metaData } returns null

        assertFalse(appInfo.isArchivedApp)
    }

    @Test
    fun `given SDK 35 and field missing, then falls back to metadata`() {
        every { Build.VERSION.SDK_INT } returns 35
        val appInfo = ApplicationInfo()
        // On SDK 34 runtime, accessing appInfo.isArchived will throw NoSuchFieldError
        appInfo.metaData = Bundle().apply { putBoolean("com.android.vending.archive", true) }

        assertTrue(appInfo.isArchivedApp)
    }

    @Test
    fun `given SDK 35 and method missing, then falls back to metadata`() {
        every { Build.VERSION.SDK_INT } returns 35
        val appInfo = mockk<ApplicationInfo>()
        every { appInfo.isArchived } throws NoSuchMethodError()
        val bundle = Bundle().apply { putBoolean("com.android.vending.archive", true) }
        every { appInfo.metaData } returns bundle

        assertTrue(appInfo.isArchivedApp)
    }

    @Test
    fun `given SDK 34 and metadata has archive key, then isArchivedApp returns true`() {
        every { Build.VERSION.SDK_INT } returns 34
        val appInfo = ApplicationInfo().apply {
            metaData = Bundle().apply {
                putBoolean("com.android.vending.archive", true)
            }
        }
        assertTrue(appInfo.isArchivedApp)
    }

    @Test
    fun `given SDK 34 and no metadata, then isArchivedApp returns false`() {
        every { Build.VERSION.SDK_INT } returns 34
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
}
