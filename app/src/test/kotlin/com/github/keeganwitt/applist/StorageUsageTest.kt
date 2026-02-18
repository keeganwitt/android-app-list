package com.github.keeganwitt.applist

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageUsageTest {
    @Test
    fun `given new instance, when created with defaults, then all values are zero`() {
        val storageUsage = StorageUsage()

        assertEquals(0L, storageUsage.apkBytes)
        assertEquals(0L, storageUsage.appBytes)
        assertEquals(0L, storageUsage.cacheBytes)
        assertEquals(0L, storageUsage.dataBytes)
        assertEquals(0L, storageUsage.externalCacheBytes)
        assertEquals(0L, storageUsage.totalBytes)
    }

    @Test
    fun `given values, when created, then properties match values`() {
        val storageUsage =
            StorageUsage(
                apkBytes = 100L,
                appBytes = 200L,
                cacheBytes = 300L,
                dataBytes = 400L,
                externalCacheBytes = 500L,
            )

        assertEquals(100L, storageUsage.apkBytes)
        assertEquals(200L, storageUsage.appBytes)
        assertEquals(300L, storageUsage.cacheBytes)
        assertEquals(400L, storageUsage.dataBytes)
        assertEquals(500L, storageUsage.externalCacheBytes)
    }

    @Test
    fun `given values, when totalBytes accessed, then returns correct sum`() {
        val storageUsage =
            StorageUsage(
                apkBytes = 100L, // Should not be included in total
                appBytes = 1000L,
                cacheBytes = 500L,
                dataBytes = 2000L,
                externalCacheBytes = 300L,
            )

        assertEquals(3800L, storageUsage.totalBytes)
    }

    @Test
    fun `given instance, when copy called with new value, then new instance has updated value`() {
        val storageUsage = StorageUsage(appBytes = 1000L)
        val newStorageUsage = storageUsage.copy(appBytes = 2000L)

        assertEquals(1000L, storageUsage.appBytes)
        assertEquals(2000L, newStorageUsage.appBytes)
        assertEquals(0L, newStorageUsage.cacheBytes) // Check other fields remain default
    }
}
