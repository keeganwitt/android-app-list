package com.github.keeganwitt.applist.services

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.storage.StorageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StorageServiceTest {
    private lateinit var context: Context
    private lateinit var storageManager: StorageManager
    private lateinit var storageStatsManager: StorageStatsManager
    private lateinit var service: AndroidStorageService

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        storageManager = mockk(relaxed = true)
        storageStatsManager = mockk(relaxed = true)
        service = AndroidStorageService(context, storageManager, storageStatsManager)
    }

    @Test
    fun `given no mounted volumes, when getStorageUsage called, then returns empty storage usage`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        every { storageManager.storageVolumes } returns emptyList()

        val result = service.getStorageUsage(appInfo)

        assertEquals(0L, result.appBytes)
        assertEquals(0L, result.cacheBytes)
        assertEquals(0L, result.dataBytes)
        assertEquals(0L, result.externalCacheBytes)
    }

    @Test
    fun `given application info, when getStorageUsage called, then returns storage usage`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }

        val result = service.getStorageUsage(appInfo)

        assertEquals(0L, result.appBytes)
        assertEquals(0L, result.cacheBytes)
        assertEquals(0L, result.dataBytes)
        assertEquals(0L, result.externalCacheBytes)
    }
}
