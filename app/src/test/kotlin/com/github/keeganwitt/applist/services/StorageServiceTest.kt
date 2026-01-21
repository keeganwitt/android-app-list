package com.github.keeganwitt.applist.services

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class StorageServiceTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

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
    fun `given mounted volume with stats, when getStorageUsage called, then returns storage usage`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val storageVolume = mockk<StorageVolume>()
        val storageStats = mockk<StorageStats>()
        val testUuid = UUID.randomUUID()

        every { storageVolume.state } returns Environment.MEDIA_MOUNTED
        every { storageVolume.uuid } returns testUuid.toString()
        every { storageManager.storageVolumes } returns listOf(storageVolume)
        every { storageStats.appBytes } returns 1000L
        every { storageStats.cacheBytes } returns 2000L
        every { storageStats.dataBytes } returns 3000L
        every { storageStatsManager.queryStatsForPackage(testUuid, "com.test.app", any()) } returns storageStats

        val result = service.getStorageUsage(appInfo)

        assertEquals(1000L, result.appBytes)
        assertEquals(2000L, result.cacheBytes)
        assertEquals(3000L, result.dataBytes)
        assertEquals(0L, result.externalCacheBytes)
    }

    @Test
    fun `given null UUID, when getStorageUsage called, then uses default UUID`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val storageVolume = mockk<StorageVolume>()
        val storageStats = mockk<StorageStats>()

        every { storageVolume.state } returns Environment.MEDIA_MOUNTED
        every { storageVolume.uuid } returns null
        every { storageManager.storageVolumes } returns listOf(storageVolume)
        every { storageStats.appBytes } returns 500L
        every { storageStats.cacheBytes } returns 0L
        every { storageStats.dataBytes } returns 0L
        every { storageStatsManager.queryStatsForPackage(StorageManager.UUID_DEFAULT, "com.test.app", any()) } returns storageStats

        val result = service.getStorageUsage(appInfo)

        assertEquals(500L, result.appBytes)
    }

    @Test
    fun `given invalid UUID string, when getStorageUsage called, then skips that volume`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val storageVolume = mockk<StorageVolume>()

        every { storageVolume.state } returns Environment.MEDIA_MOUNTED
        every { storageVolume.uuid } returns "invalid-uuid"
        every { storageManager.storageVolumes } returns listOf(storageVolume)

        val result = service.getStorageUsage(appInfo)

        assertEquals(0L, result.totalBytes)
    }

    @Test
    fun `given unmounted volume, when getStorageUsage called, then skips that volume`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val storageVolume = mockk<StorageVolume>()

        every { storageVolume.state } returns Environment.MEDIA_UNMOUNTED
        every { storageManager.storageVolumes } returns listOf(storageVolume)

        val result = service.getStorageUsage(appInfo)

        assertEquals(0L, result.totalBytes)
    }

    @Test
    fun `given queryStatsForPackage throws exception, when getStorageUsage called, then returns partial results`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val storageVolume = mockk<StorageVolume>()
        val testUuid = UUID.randomUUID()

        every { storageVolume.state } returns Environment.MEDIA_MOUNTED
        every { storageVolume.uuid } returns testUuid.toString()
        every { storageManager.storageVolumes } returns listOf(storageVolume)
        every { storageStatsManager.queryStatsForPackage(testUuid, "com.test.app", any()) } throws SecurityException("No permission")

        val result = service.getStorageUsage(appInfo)

        assertEquals(0L, result.totalBytes)
    }

    @Test
    @Config(sdk = [31])
    fun `given SDK 31 and mounted volume, when getStorageUsage called, then storage stats are included`() {
        val tempFile = tempFolder.newFile("test.apk")
        tempFile.writeBytes(ByteArray(1234))
        val appInfo =
            ApplicationInfo().apply {
                packageName = "com.test.app"
                publicSourceDir = tempFile.absolutePath
            }
        val storageVolume = mockk<StorageVolume>()
        val storageStats = mockk<StorageStats>()
        val testUuid = UUID.randomUUID()

        every { storageVolume.state } returns Environment.MEDIA_MOUNTED
        every { storageVolume.uuid } returns testUuid.toString()
        every { storageManager.storageVolumes } returns listOf(storageVolume)
        every { storageStats.appBytes } returns 1000L
        every { storageStats.cacheBytes } returns 2000L
        every { storageStats.dataBytes } returns 3000L
        every { storageStats.externalCacheBytes } returns 4000L
        every { storageStatsManager.queryStatsForPackage(testUuid, "com.test.app", any()) } returns storageStats

        val result = service.getStorageUsage(appInfo)

        assertEquals(1000L, result.appBytes)
        assertEquals(2000L, result.cacheBytes)
        assertEquals(3000L, result.dataBytes)
        assertEquals(4000L, result.externalCacheBytes)
        assertEquals(1234L, result.apkBytes)
    }

    @Test
    @Config(sdk = [25])
    fun `given SDK below O, when getStorageUsage called, then returns empty storage usage`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }

        val result = service.getStorageUsage(appInfo)

        assertEquals(0L, result.totalBytes)
    }
}
