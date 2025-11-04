package com.github.keeganwitt.applist.services

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class UsageStatsServiceTest {
    private lateinit var context: Context
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var service: AndroidUsageStatsService

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        usageStatsManager = mockk(relaxed = true)
        every { context.getSystemService(Context.USAGE_STATS_SERVICE) } returns usageStatsManager
        service = AndroidUsageStatsService(context)
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
            (1..5)
                .map { i ->
                    val stats = mockk<UsageStats>()
                    every { stats.lastTimeUsed } returns i * 1000L
                    "com.test.app$i" to stats
                }.toMap()

        every { usageStatsManager.queryAndAggregateUsageStats(any(), any()) } returns apps

        val result = service.getLastUsedEpochs(reload = false)

        assertEquals(5, result.size)
        assertEquals(1000L, result["com.test.app1"])
        assertEquals(5000L, result["com.test.app5"])
    }
}
