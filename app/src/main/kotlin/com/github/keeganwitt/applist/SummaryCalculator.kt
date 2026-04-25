package com.github.keeganwitt.applist

import android.content.Context
import com.github.keeganwitt.applist.SummaryType.BOOLEAN
import com.github.keeganwitt.applist.SummaryType.DATE
import com.github.keeganwitt.applist.SummaryType.NONE
import com.github.keeganwitt.applist.SummaryType.PACKAGE_MANAGER
import com.github.keeganwitt.applist.SummaryType.PERMISSION
import com.github.keeganwitt.applist.SummaryType.SDK
import com.github.keeganwitt.applist.SummaryType.SIZE
import com.github.keeganwitt.applist.services.AppStoreService
import java.util.concurrent.TimeUnit

data class SummaryItem(
    val field: AppInfoField,
    val buckets: Map<String, Int>,
)

class SummaryCalculator(
    private val context: Context,
    private val appStoreService: AppStoreService,
) {
    fun calculate(
        apps: List<App>,
        field: AppInfoField,
    ): SummaryItem? =
        when (field.summaryType) {
            BOOLEAN, PACKAGE_MANAGER -> {
                val map =
                    apps
                        .groupingBy {
                            field.getSummaryKey(it, context, appStoreService) ?: ""
                        }.eachCount()
                val sortedMap =
                    if (field.summaryType == PACKAGE_MANAGER) {
                        map.toSortedMap()
                    } else {
                        map
                    }
                SummaryItem(field, sortedMap)
            }

            SIZE -> {
                calculateSizeSummary(field, apps.map { field.getValue(it) as Long })
            }

            SDK -> {
                calculateSdkSummary(field, apps.mapNotNull { field.getValue(it) as? Int })
            }

            PERMISSION -> {
                calculatePermissionSummary(field, apps.mapNotNull { field.getValue(it) as? Int })
            }

            DATE -> {
                calculateDateSummary(field, apps.mapNotNull { field.getValue(it) as? Long })
            }

            NONE -> {
                null
            }
        }

    private fun calculateSizeSummary(
        field: AppInfoField,
        sizes: List<Long>,
    ): SummaryItem {
        val orderedBuckets = linkedMapOf<String, Int>()
        val small = context.getString(R.string.size_bucket_small)
        val medium = context.getString(R.string.size_bucket_medium)
        val large = context.getString(R.string.size_bucket_large)
        val huge = context.getString(R.string.size_bucket_huge)

        orderedBuckets[small] = 0
        orderedBuckets[medium] = 0
        orderedBuckets[large] = 0
        orderedBuckets[huge] = 0

        sizes.forEach { size ->
            val key =
                when {
                    size < 10 * 1024 * 1024 -> small
                    size < 50 * 1024 * 1024 -> medium
                    size < 100 * 1024 * 1024 -> large
                    else -> huge
                }
            orderedBuckets[key] = orderedBuckets[key]!! + 1
        }

        return SummaryItem(field, orderedBuckets)
    }

    private fun calculateSdkSummary(
        field: AppInfoField,
        sdks: List<Int>,
    ): SummaryItem {
        val buckets =
            sdks
                .groupingBy { it.toString() }
                .eachCount()
                .toSortedMap(compareByDescending { it.toIntOrNull() ?: 0 })
        return SummaryItem(field, buckets)
    }

    private fun calculatePermissionSummary(
        field: AppInfoField,
        counts: List<Int>,
    ): SummaryItem {
        val orderedBuckets = linkedMapOf<String, Int>()
        val sNone = context.getString(R.string.perm_bucket_none)
        val sFew = context.getString(R.string.perm_bucket_few)
        val sSome = context.getString(R.string.perm_bucket_some)
        val sMany = context.getString(R.string.perm_bucket_many)
        val sLots = context.getString(R.string.perm_bucket_lots)

        orderedBuckets[sNone] = 0
        orderedBuckets[sFew] = 0
        orderedBuckets[sSome] = 0
        orderedBuckets[sMany] = 0
        orderedBuckets[sLots] = 0

        counts.forEach { count ->
            val key =
                when {
                    count == 0 -> sNone
                    count <= 5 -> sFew
                    count <= 10 -> sSome
                    count <= 20 -> sMany
                    else -> sLots
                }
            orderedBuckets[key] = orderedBuckets[key]!! + 1
        }

        return SummaryItem(field, orderedBuckets)
    }

    private fun calculateDateSummary(
        field: AppInfoField,
        timestamps: List<Long>,
    ): SummaryItem {
        val orderedBuckets = linkedMapOf<String, Int>()
        val lastMonth = context.getString(R.string.date_bucket_last_month)
        val lastThreeMonths = context.getString(R.string.date_bucket_last_three_months)
        val lastSixMonths = context.getString(R.string.date_bucket_last_six_months)
        val older = context.getString(R.string.date_bucket_older)

        orderedBuckets[lastMonth] = 0
        orderedBuckets[lastThreeMonths] = 0
        orderedBuckets[lastSixMonths] = 0
        orderedBuckets[older] = 0

        val now = System.currentTimeMillis()
        val oneMonthAgo = now - TimeUnit.DAYS.toMillis(30)
        val threeMonthsAgo = now - TimeUnit.DAYS.toMillis(90)
        val sixMonthsAgo = now - TimeUnit.DAYS.toMillis(180)

        timestamps.forEach { timestamp ->
            val key =
                when {
                    timestamp > oneMonthAgo -> lastMonth
                    timestamp > threeMonthsAgo -> lastThreeMonths
                    timestamp > sixMonthsAgo -> lastSixMonths
                    else -> older
                }
            orderedBuckets[key] = orderedBuckets[key]!! + 1
        }

        return SummaryItem(field, orderedBuckets)
    }
}
