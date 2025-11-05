package com.github.keeganwitt.applist.services

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

interface UsageStatsService {
    fun getLastUsedEpochs(reload: Boolean): Map<String, Long>
}

class AndroidUsageStatsService(
    context: Context,
    usageStatsManager: UsageStatsManager? = null,
) : UsageStatsService {
    private val actualUsageStatsManager = usageStatsManager ?: (context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager)
    private var cache: Map<String, Long>? = null

    override fun getLastUsedEpochs(reload: Boolean): Map<String, Long> {
        if (cache == null || reload) {
            val calendar = Calendar.getInstance()
            val end = calendar.timeInMillis
            calendar.add(Calendar.YEAR, -2)
            val start = calendar.timeInMillis
            val aggregated: Map<String, UsageStats> = actualUsageStatsManager.queryAndAggregateUsageStats(start, end)
            cache = aggregated.mapValues { it.value.lastTimeUsed }
        }
        return cache!!
    }
}
