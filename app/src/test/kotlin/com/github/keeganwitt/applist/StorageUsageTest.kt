package com.github.keeganwitt.applist

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StorageUsageTest {
    private lateinit var storageUsage: StorageUsage

    @Before
    fun setup() {
        storageUsage = StorageUsage()
    }

    @Test
    fun `given new instance, when created, then all values are zero`() {
        assertEquals(0L, storageUsage.appBytes)
        assertEquals(0L, storageUsage.cacheBytes)
        assertEquals(0L, storageUsage.dataBytes)
        assertEquals(0L, storageUsage.externalCacheBytes)
        assertEquals(0L, storageUsage.totalBytes)
    }

    @Test
    fun `given app bytes increased, when increaseAppBytes called, then appBytes is updated`() {
        storageUsage.increaseAppBytes(1000L)

        assertEquals(1000L, storageUsage.appBytes)
        assertEquals(1000L, storageUsage.totalBytes)
    }

    @Test
    fun `given cache bytes increased, when increaseCacheBytes called, then cacheBytes is updated`() {
        storageUsage.increaseCacheBytes(500L)

        assertEquals(500L, storageUsage.cacheBytes)
        assertEquals(500L, storageUsage.totalBytes)
    }

    @Test
    fun `given data bytes increased, when increaseDataBytes called, then dataBytes is updated`() {
        storageUsage.increaseDataBytes(2000L)

        assertEquals(2000L, storageUsage.dataBytes)
        assertEquals(2000L, storageUsage.totalBytes)
    }

    @Test
    fun `given external cache bytes increased, when increaseExternalCacheBytes called, then externalCacheBytes is updated`() {
        storageUsage.increaseExternalCacheBytes(300L)

        assertEquals(300L, storageUsage.externalCacheBytes)
        assertEquals(300L, storageUsage.totalBytes)
    }

    @Test
    fun `given multiple increases, when increase methods called, then values accumulate`() {
        storageUsage.increaseAppBytes(1000L)
        storageUsage.increaseAppBytes(500L)

        assertEquals(1500L, storageUsage.appBytes)
    }

    @Test
    fun `given all storage types increased, when totalBytes accessed, then returns sum of all`() {
        storageUsage.increaseAppBytes(1000L)
        storageUsage.increaseCacheBytes(500L)
        storageUsage.increaseDataBytes(2000L)
        storageUsage.increaseExternalCacheBytes(300L)

        assertEquals(3800L, storageUsage.totalBytes)
    }

    @Test
    fun `given zero increases, when totalBytes accessed, then returns zero`() {
        assertEquals(0L, storageUsage.totalBytes)
    }

    @Test
    fun `given large values, when increased, then handles large numbers correctly`() {
        val largeValue = 1_000_000_000L
        storageUsage.increaseAppBytes(largeValue)
        storageUsage.increaseCacheBytes(largeValue)
        storageUsage.increaseDataBytes(largeValue)
        storageUsage.increaseExternalCacheBytes(largeValue)

        assertEquals(4_000_000_000L, storageUsage.totalBytes)
    }

    @Test
    fun `given multiple storage types, when totalBytes calculated, then includes all components`() {
        storageUsage.increaseAppBytes(100L)
        storageUsage.increaseCacheBytes(200L)
        storageUsage.increaseDataBytes(300L)

        assertEquals(600L, storageUsage.totalBytes)
        assertEquals(100L, storageUsage.appBytes)
        assertEquals(200L, storageUsage.cacheBytes)
        assertEquals(300L, storageUsage.dataBytes)
    }
}
