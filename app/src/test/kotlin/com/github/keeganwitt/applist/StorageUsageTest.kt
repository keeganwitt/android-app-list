package com.github.keeganwitt.applist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StorageUsageTest {
    @Test
    fun `given new instance, when created with defaults, then all values are null`() {
        val usage = StorageUsage()
        assertNull(usage.apkBytes)
        assertNull(usage.appBytes)
        assertNull(usage.cacheBytes)
        assertNull(usage.dataBytes)
        assertNull(usage.externalCacheBytes)
        assertNull(usage.totalBytes)
    }

    @Test
    fun `given instance with some nulls, when totalBytes called, then returns sum of non-nulls`() {
        val usage = StorageUsage(appBytes = 100L, cacheBytes = null, dataBytes = 200L)
        assertEquals(300L, usage.totalBytes)
    }

    @Test
    fun `given instance with all values, when totalBytes called, then returns sum`() {
        val usage =
            StorageUsage(
                apkBytes = 100L,
                appBytes = 200L,
                cacheBytes = 300L,
                dataBytes = 400L,
                externalCacheBytes = 500L,
            )
        assertEquals(1400L, usage.totalBytes)
    }

    @Test
    fun `given instance with zero values, when totalBytes called, then returns zero`() {
        val usage =
            StorageUsage(
                appBytes = 0L,
                cacheBytes = 0L,
                dataBytes = 0L,
                externalCacheBytes = 0L,
            )
        assertEquals(0L, usage.totalBytes)
    }

    @Test
    fun `given instance, when copy called with new value, then new instance has updated value`() {
        val usage = StorageUsage(apkBytes = 100L)
        val newUsage = usage.copy(apkBytes = 200L)
        assertEquals(200L, newUsage.apkBytes)
        assertNull(newUsage.appBytes)
    }
}
