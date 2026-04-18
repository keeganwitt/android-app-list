package com.github.keeganwitt.applist.services

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import java.time.Clock
import java.time.ZonedDateTime

interface UsageStatsService {
    fun getLastUsedEpochs(reload: Boolean): Map<String, Long>?
}

class AndroidUsageStatsService(
    context: Context,
    usageStatsManager: UsageStatsManager? = null,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val crashReporter: com.github.keeganwitt.applist.CrashReporter? = null,
) : UsageStatsService {
    private val actualUsageStatsManager = usageStatsManager ?: (context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager)
    private var cache: Map<String, Long>? = null
    private var lastLoadFailed = false

    override fun getLastUsedEpochs(reload: Boolean): Map<String, Long>? {
        if (cache == null || reload) {
            try {
                val now = ZonedDateTime.now(clock)
                val end = now.toInstant().toEpochMilli()
                val start = now.minusYears(2).toInstant().toEpochMilli()
                val aggregated: Map<String, UsageStats> = actualUsageStatsManager.queryAndAggregateUsageStats(start, end)
                cache = aggregated.mapValues { it.value.lastTimeUsed }
                lastLoadFailed = false
            } catch (e: SecurityException) {
                Log.w(TAG, "AndroidUsageStatsService.getLastUsedEpochs failed: missing permission", e)
                cache = emptyMap()
                lastLoadFailed = true
            } catch (e: Exception) {
                crashReporter?.recordException(e, "AndroidUsageStatsService.getLastUsedEpochs failed")
                cache = emptyMap()
                lastLoadFailed = true
            }
        }
        return if (lastLoadFailed) null else cache
    }

    companion object {
        private val TAG = UsageStatsService::class.java.simpleName
    }
}
