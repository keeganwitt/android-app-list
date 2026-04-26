package com.github.keeganwitt.applist.services

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import com.github.keeganwitt.applist.CrashReporter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    private lateinit var crashReporter: CrashReporter
    private lateinit var service: AndroidStorageService

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        storageManager = mockk(relaxed = true)
        storageStatsManager = mockk(relaxed = true)
        crashReporter = mockk(relaxed = true)
        service = AndroidStorageService(context, storageManager, storageStatsManager, crashReporter)
    }

    @Test
    fun `given multiple volumes where one has invalid UUID, when getStorageUsage called, then returns results from successful volume`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val volume1 = mockk<StorageVolume>()
        val volume2 = mockk<StorageVolume>()
        val stats2 = mockk<StorageStats>()
        val uuid2 = UUID.randomUUID()

        every { volume1.state } returns Environment.MEDIA_MOUNTED
        every { volume1.uuid } returns "invalid-uuid"
        every { volume2.state } returns Environment.MEDIA_MOUNTED
        every { volume2.uuid } returns uuid2.toString()
        every { storageManager.storageVolumes } returns listOf(volume1, volume2)

        every { stats2.appBytes } returns 1000L
        every { stats2.cacheBytes } returns 2000L
        every { stats2.dataBytes } returns 3000L
        every { storageStatsManager.queryStatsForPackage(uuid2, "com.test.app", any()) } returns stats2

        val result = service.getStorageUsage(appInfo)

        assertEquals(1000L, result.appBytes)
        assertEquals(2000L, result.cacheBytes)
        assertEquals(3000L, result.dataBytes)
    }

    @Test
    fun `given multiple volumes with invalid UUIDs, when getStorageUsage called, then returns null total bytes`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val volume1 = mockk<StorageVolume>()
        val volume2 = mockk<StorageVolume>()

        every { volume1.state } returns Environment.MEDIA_MOUNTED
        every { volume1.uuid } returns "invalid-uuid-1"
        every { volume2.state } returns Environment.MEDIA_MOUNTED
        every { volume2.uuid } returns "invalid-uuid-2"
        every { storageManager.storageVolumes } returns listOf(volume1, volume2)

        val result = service.getStorageUsage(appInfo)

        assertNull(result.totalBytes)
    }

    @Test
    fun `given no mounted volumes, when getStorageUsage called, then returns null storage usage`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        every { storageManager.storageVolumes } returns emptyList()

        val result = service.getStorageUsage(appInfo)

        assertNull(result.appBytes)
        assertNull(result.cacheBytes)
        assertNull(result.dataBytes)
        assertNull(result.externalCacheBytes)
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
        assertNull(result.externalCacheBytes)
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

        assertNull(result.totalBytes)
    }

    @Test
    fun `given unmounted volume, when getStorageUsage called, then skips that volume`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val storageVolume = mockk<StorageVolume>()

        every { storageVolume.state } returns Environment.MEDIA_UNMOUNTED
        every { storageManager.storageVolumes } returns listOf(storageVolume)

        val result = service.getStorageUsage(appInfo)

        assertNull(result.totalBytes)
    }

    @Test
    fun `given queryStatsForPackage throws SecurityException, when getStorageUsage called, then returns empty results and does not log to crash reporter`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val storageVolume = mockk<StorageVolume>()
        val testUuid = UUID.randomUUID()

        every { storageVolume.state } returns Environment.MEDIA_MOUNTED
        every { storageVolume.uuid } returns testUuid.toString()
        every { storageManager.storageVolumes } returns listOf(storageVolume)
        every { storageStatsManager.queryStatsForPackage(testUuid, "com.test.app", any()) } throws SecurityException("No permission")

        val result = service.getStorageUsage(appInfo)

        assertNull(result.totalBytes)
        verify(exactly = 0) { crashReporter.log(any()) }
        verify(exactly = 0) { crashReporter.recordException(any(), any()) }
    }

    @Test
    fun `given queryStatsForPackage throws generic Exception, when getStorageUsage called, then records exception to crash reporter`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val storageVolume = mockk<StorageVolume>()
        val testUuid = UUID.randomUUID()

        every { storageVolume.state } returns Environment.MEDIA_MOUNTED
        every { storageVolume.uuid } returns testUuid.toString()
        every { storageManager.storageVolumes } returns listOf(storageVolume)
        val exception = RuntimeException("Something went wrong")
        every { storageStatsManager.queryStatsForPackage(testUuid, "com.test.app", any()) } throws exception

        val result = service.getStorageUsage(appInfo)

        assertNull(result.totalBytes)
        verify { crashReporter.recordException(exception, match { it.contains("Unable to process storage usage") }) }
    }

    @Test
    fun `given multiple volumes where one fails, when getStorageUsage called, then returns results from successful volume`() {
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val storageVolume1 = mockk<StorageVolume>()
        val storageVolume2 = mockk<StorageVolume>()
        val storageStats2 = mockk<StorageStats>()
        val testUuid1 = UUID.randomUUID()
        val testUuid2 = UUID.randomUUID()

        every { storageVolume1.state } returns Environment.MEDIA_MOUNTED
        every { storageVolume1.uuid } returns testUuid1.toString()
        every { storageVolume2.state } returns Environment.MEDIA_MOUNTED
        every { storageVolume2.uuid } returns testUuid2.toString()
        every { storageManager.storageVolumes } returns listOf(storageVolume1, storageVolume2)

        every { storageStatsManager.queryStatsForPackage(testUuid1, "com.test.app", any()) } throws SecurityException("No permission")

        every { storageStats2.appBytes } returns 1000L
        every { storageStats2.cacheBytes } returns 0L
        every { storageStats2.dataBytes } returns 0L
        every { storageStatsManager.queryStatsForPackage(testUuid2, "com.test.app", any()) } returns storageStats2

        val result = service.getStorageUsage(appInfo)

        assertEquals(1000L, result.appBytes)
    }

    @Test
    fun `given apkBytes is set and queryStatsForPackage fails, when getStorageUsage called, then returns apkBytes`() {
        val tempFile = tempFolder.newFile("test.apk")
        tempFile.writeBytes(ByteArray(1234))
        val appInfo =
            ApplicationInfo().apply {
                packageName = "com.test.app"
                publicSourceDir = tempFile.absolutePath
            }
        val storageVolume = mockk<StorageVolume>()
        val testUuid = UUID.randomUUID()

        every { storageVolume.state } returns Environment.MEDIA_MOUNTED
        every { storageVolume.uuid } returns testUuid.toString()
        every { storageManager.storageVolumes } returns listOf(storageVolume)
        every { storageStatsManager.queryStatsForPackage(testUuid, "com.test.app", any()) } throws SecurityException("No permission")

        val result = service.getStorageUsage(appInfo)

        assertEquals(1234L, result.apkBytes)
        assertNull(result.appBytes)
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
    fun `given SDK below O, when getStorageUsage called, then returns only apk bytes if present`() {
        val tempFile = tempFolder.newFile("test.apk")
        tempFile.writeBytes(ByteArray(1234))
        val appInfo =
            ApplicationInfo().apply {
                packageName = "com.test.app"
                publicSourceDir = tempFile.absolutePath
            }

        val result = service.getStorageUsage(appInfo)

        assertEquals(1234L, result.apkBytes)
        assertNull(result.appBytes)
    }

    @Test
    fun `given SecurityException and null crash reporter, when getStorageUsage called, then handles safely`() {
        val serviceNoCrashReporter = AndroidStorageService(context, storageManager, storageStatsManager, null)
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val storageVolume = mockk<StorageVolume>()
        val testUuid = UUID.randomUUID()

        every { storageVolume.state } returns Environment.MEDIA_MOUNTED
        every { storageVolume.uuid } returns testUuid.toString()
        every { storageManager.storageVolumes } returns listOf(storageVolume)
        every { storageStatsManager.queryStatsForPackage(testUuid, "com.test.app", any()) } throws SecurityException("No permission")

        val result = serviceNoCrashReporter.getStorageUsage(appInfo)
        assertNull(result.totalBytes)
    }

    @Test
    fun `given generic Exception and null crash reporter, when getStorageUsage called, then handles safely`() {
        val serviceNoCrashReporter = AndroidStorageService(context, storageManager, storageStatsManager, null)
        val appInfo = ApplicationInfo().apply { packageName = "com.test.app" }
        val storageVolume = mockk<StorageVolume>()
        val testUuid = UUID.randomUUID()

        every { storageVolume.state } returns Environment.MEDIA_MOUNTED
        every { storageVolume.uuid } returns testUuid.toString()
        every { storageManager.storageVolumes } returns listOf(storageVolume)
        every { storageStatsManager.queryStatsForPackage(testUuid, "com.test.app", any()) } throws RuntimeException("Error")

        val result = serviceNoCrashReporter.getStorageUsage(appInfo)
        assertNull(result.totalBytes)
    }

    @Test
    fun `given null publicSourceDir, when getStorageUsage called, then it handles it safely`() {
        // new File(null) throws NullPointerException, which should be caught by the try-catch block
        val appInfo =
            ApplicationInfo().apply {
                packageName = "com.test.app"
                publicSourceDir = null
            }

        val result = service.getStorageUsage(appInfo)

        assertNull(result.apkBytes)
    }
}
