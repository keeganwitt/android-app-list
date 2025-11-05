package com.github.keeganwitt.applist.services

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UsageStatsServiceTest {
    private lateinit var context: Context
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var service: AndroidUsageStatsService

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        usageStatsManager = mockk(relaxed = true)
        service = AndroidUsageStatsService(context, usageStatsManager)
    }

    @Test
    fun `given no usage stats, when getLastUsedEpochs called, then returns empty map`() {
        every { usageStatsManager.queryAndAggregateUsageStats(any(), any()) } returns emptyMap()

        val result = service.getLastUsedEpochs(reload = false)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `given usage stats, when getLastUsedEpochs called, then returns map with last used times`() {
        val usageStats1 = mockk<UsageStats>()
        val usageStats2 = mockk<UsageStats>()
        every { usageStats1.lastTimeUsed } returns 1000L
        every { usageStats2.lastTimeUsed } returns 2000L

        val statsMap =
            mapOf(
                "com.test.app1" to usageStats1,
                "com.test.app2" to usageStats2,
            )
        every { usageStatsManager.queryAndAggregateUsageStats(any(), any()) } returns statsMap

        val result = service.getLastUsedEpochs(reload = false)

        assertEquals(2, result.size)
        assertEquals(1000L, result["com.test.app1"])
        assertEquals(2000L, result["com.test.app2"])
    }

    @Test
    fun `given cached data, when getLastUsedEpochs called with reload false, then returns cached data`() {
        val usageStats = mockk<UsageStats>()
        every { usageStats.lastTimeUsed } returns 1000L
        val statsMap = mapOf("com.test.app" to usageStats)
        every { usageStatsManager.queryAndAggregateUsageStats(any(), any()) } returns statsMap

        service.getLastUsedEpochs(reload = false)
        service.getLastUsedEpochs(reload = false)

        verify(exactly = 1) { usageStatsManager.queryAndAggregateUsageStats(any(), any()) }
    }

    @Test
    fun `given cached data, when getLastUsedEpochs called with reload true, then fetches new data`() {
        val usageStats = mockk<UsageStats>()
        every { usageStats.lastTimeUsed } returns 1000L
        val statsMap = mapOf("com.test.app" to usageStats)
        every { usageStatsManager.queryAndAggregateUsageStats(any(), any()) } returns statsMap

        service.getLastUsedEpochs(reload = false)
        service.getLastUsedEpochs(reload = true)

        verify(exactly = 2) { usageStatsManager.queryAndAggregateUsageStats(any(), any()) }
    }

    @Test
    fun `given multiple apps, when getLastUsedEpochs called, then all apps are included in result`() {
        val apps =
            (1..5).associate { i ->
                val stats = mockk<UsageStats>()
                every { stats.lastTimeUsed } returns i * 1000L
                "com.test.app$i" to stats
            }

        every { usageStatsManager.queryAndAggregateUsageStats(any(), any()) } returns apps

        val result = service.getLastUsedEpochs(reload = false)

        assertEquals(5, result.size)
        assertEquals(1000L, result["com.test.app1"])
        assertEquals(5000L, result["com.test.app5"])
    }

    @Test
    fun `given zero lastTimeUsed, when getLastUsedEpochs called, then includes zero value`() {
        val usageStats = mockk<UsageStats>()
        every { usageStats.lastTimeUsed } returns 0L
        val statsMap = mapOf("com.test.app" to usageStats)
        every { usageStatsManager.queryAndAggregateUsageStats(any(), any()) } returns statsMap

        val result = service.getLastUsedEpochs(reload = false)

        assertEquals(1, result.size)
        assertEquals(0L, result["com.test.app"])
    }

    @Test
    fun `given negative lastTimeUsed, when getLastUsedEpochs called, then includes negative value`() {
        val usageStats = mockk<UsageStats>()
        every { usageStats.lastTimeUsed } returns -1L
        val statsMap = mapOf("com.test.app" to usageStats)
        every { usageStatsManager.queryAndAggregateUsageStats(any(), any()) } returns statsMap

        val result = service.getLastUsedEpochs(reload = false)

        assertEquals(1, result.size)
        assertEquals(-1L, result["com.test.app"])
    }

    @Test
    fun `given first call with reload true, when getLastUsedEpochs called, then fetches data`() {
        val usageStats = mockk<UsageStats>()
        every { usageStats.lastTimeUsed } returns 1000L
        val statsMap = mapOf("com.test.app" to usageStats)
        every { usageStatsManager.queryAndAggregateUsageStats(any(), any()) } returns statsMap

        val result = service.getLastUsedEpochs(reload = true)

        assertEquals(1, result.size)
        assertEquals(1000L, result["com.test.app"])
        verify(exactly = 1) { usageStatsManager.queryAndAggregateUsageStats(any(), any()) }
    }

    @Test
    fun `given cached data with different values, when reload true, then returns new data`() {
        val oldStats = mockk<UsageStats>()
        val newStats = mockk<UsageStats>()
        every { oldStats.lastTimeUsed } returns 1000L
        every { newStats.lastTimeUsed } returns 2000L

        every {
            usageStatsManager.queryAndAggregateUsageStats(any(), any())
        } returns mapOf("com.test.app" to oldStats) andThen mapOf("com.test.app" to newStats)

        val firstResult = service.getLastUsedEpochs(reload = false)
        val secondResult = service.getLastUsedEpochs(reload = true)

        assertEquals(1000L, firstResult["com.test.app"])
        assertEquals(2000L, secondResult["com.test.app"])
    }

    @Test
    fun `given large number of apps, when getLastUsedEpochs called, then all are processed`() {
        val apps =
            (1..100).associate { i ->
                val stats = mockk<UsageStats>()
                every { stats.lastTimeUsed } returns i * 1000L
                "com.test.app$i" to stats
            }

        every { usageStatsManager.queryAndAggregateUsageStats(any(), any()) } returns apps

        val result = service.getLastUsedEpochs(reload = false)

        assertEquals(100, result.size)
        assertEquals(1000L, result["com.test.app1"])
        assertEquals(100000L, result["com.test.app100"])
    }

    @Test
    fun `given apps with same lastTimeUsed, when getLastUsedEpochs called, then all are included`() {
        val stats1 = mockk<UsageStats>()
        val stats2 = mockk<UsageStats>()
        every { stats1.lastTimeUsed } returns 5000L
        every { stats2.lastTimeUsed } returns 5000L

        val statsMap =
            mapOf(
                "com.test.app1" to stats1,
                "com.test.app2" to stats2,
            )
        every { usageStatsManager.queryAndAggregateUsageStats(any(), any()) } returns statsMap

        val result = service.getLastUsedEpochs(reload = false)

        assertEquals(2, result.size)
        assertEquals(5000L, result["com.test.app1"])
        assertEquals(5000L, result["com.test.app2"])
    }
}
