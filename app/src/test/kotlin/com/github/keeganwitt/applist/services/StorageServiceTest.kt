package com.github.keeganwitt.applist.services

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.test.core.app.ApplicationProvider
import com.github.keeganwitt.applist.StorageUsage
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StorageServiceTest {
    private lateinit var context: Context
    private lateinit var service: AndroidStorageService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        service = AndroidStorageService(context)
    }

    @Test
    fun `given application info, when getStorageUsage called, then returns storage usage`() {
        val appInfo =
            ApplicationInfo().apply {
                packageName = "com.test.app"
            }

        val result = service.getStorageUsage(appInfo)

        assertEquals(0L, result.appBytes)
        assertEquals(0L, result.cacheBytes)
        assertEquals(0L, result.dataBytes)
        assertEquals(0L, result.externalCacheBytes)
    }

    @Test
    @Config(sdk = [26])
    fun `given SDK below O, when getStorageUsage called, then returns empty storage usage`() {
        val appInfo =
            ApplicationInfo().apply {
                packageName = "com.test.app"
            }

        val result = service.getStorageUsage(appInfo)

        assertEquals(0L, result.totalBytes)
    }
}
