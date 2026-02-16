package com.github.keeganwitt.applist.services

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.Clock
import java.time.ZonedDateTime

interface UsageStatsService {
    fun getLastUsedEpochs(reload: Boolean): Map<String, Long>
}

class AndroidUsageStatsService(
    context: Context,
    usageStatsManager: UsageStatsManager? = null,
    private val clock: Clock = Clock.systemDefaultZone(),
) : UsageStatsService {
    private val actualUsageStatsManager = usageStatsManager ?: (context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager)
    private var cache: Map<String, Long>? = null

    override fun getLastUsedEpochs(reload: Boolean): Map<String, Long> {
        if (cache == null || reload) {
            val now = ZonedDateTime.now(clock)
            val end = now.toInstant().toEpochMilli()
            val start = now.minusYears(2).toInstant().toEpochMilli()
            val aggregated: Map<String, UsageStats> = actualUsageStatsManager.queryAndAggregateUsageStats(start, end)
            cache = aggregated.mapValues { it.value.lastTimeUsed }
        }
        return cache!!
    }
}
