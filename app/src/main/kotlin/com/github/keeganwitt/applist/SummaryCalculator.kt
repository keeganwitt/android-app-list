package com.github.keeganwitt.applist

import android.content.Context
import com.github.keeganwitt.applist.AppInfoField.APK_SIZE
import com.github.keeganwitt.applist.AppInfoField.APP_SIZE
import com.github.keeganwitt.applist.AppInfoField.ARCHIVED
import com.github.keeganwitt.applist.AppInfoField.CACHE_SIZE
import com.github.keeganwitt.applist.AppInfoField.DATA_SIZE
import com.github.keeganwitt.applist.AppInfoField.ENABLED
import com.github.keeganwitt.applist.AppInfoField.EXISTS_IN_APP_STORE
import com.github.keeganwitt.applist.AppInfoField.EXTERNAL_CACHE_SIZE
import com.github.keeganwitt.applist.AppInfoField.FIRST_INSTALLED
import com.github.keeganwitt.applist.AppInfoField.GRANTED_PERMISSIONS
import com.github.keeganwitt.applist.AppInfoField.LAST_UPDATED
import com.github.keeganwitt.applist.AppInfoField.LAST_USED
import com.github.keeganwitt.applist.AppInfoField.MIN_SDK
import com.github.keeganwitt.applist.AppInfoField.PACKAGE_MANAGER
import com.github.keeganwitt.applist.AppInfoField.REQUESTED_PERMISSIONS
import com.github.keeganwitt.applist.AppInfoField.TARGET_SDK
import com.github.keeganwitt.applist.AppInfoField.TOTAL_SIZE
import com.github.keeganwitt.applist.AppInfoField.VERSION

data class SummaryItem(
    val field: AppInfoField,
    val buckets: Map<String, Int>,
)

class SummaryCalculator(
    private val context: Context,
) {
    fun calculate(
        apps: List<App>,
        field: AppInfoField,
    ): SummaryItem? =
        when (field) {
            ENABLED -> {
                val enabledMap =
                    apps
                        .groupingBy {
                            if (it.enabled) context.getString(R.string.enabled) else context.getString(R.string.disabled)
                        }.eachCount()
                SummaryItem(ENABLED, enabledMap)
            }

            ARCHIVED -> {
                val archivedMap =
                    apps
                        .groupingBy {
                            if (it.archived == true) context.getString(R.string.archived) else context.getString(R.string.installed)
                        }.eachCount()
                SummaryItem(ARCHIVED, archivedMap)
            }

            EXISTS_IN_APP_STORE -> {
                val existsMap =
                    apps
                        .groupingBy {
                            if (it.existsInStore ==
                                true
                            ) {
                                context.getString(R.string.boolean_true)
                            } else {
                                context.getString(R.string.boolean_false)
                            }
                        }.eachCount()
                SummaryItem(EXISTS_IN_APP_STORE, existsMap)
            }

            APK_SIZE -> {
                calculateSizeSummary(APK_SIZE, apps.map { it.sizes.apkBytes })
            }

            APP_SIZE -> {
                calculateSizeSummary(APP_SIZE, apps.map { it.sizes.appBytes })
            }

            DATA_SIZE -> {
                calculateSizeSummary(DATA_SIZE, apps.map { it.sizes.dataBytes })
            }

            CACHE_SIZE -> {
                calculateSizeSummary(CACHE_SIZE, apps.map { it.sizes.cacheBytes })
            }

            EXTERNAL_CACHE_SIZE -> {
                calculateSizeSummary(EXTERNAL_CACHE_SIZE, apps.map { it.sizes.externalCacheBytes })
            }

            TOTAL_SIZE -> {
                calculateSizeSummary(TOTAL_SIZE, apps.map { it.sizes.totalBytes })
            }

            TARGET_SDK -> {
                calculateSdkSummary(TARGET_SDK, apps.mapNotNull { it.targetSdk })
            }

            MIN_SDK -> {
                calculateSdkSummary(MIN_SDK, apps.mapNotNull { it.minSdk })
            }

            GRANTED_PERMISSIONS -> {
                calculatePermissionSummary(GRANTED_PERMISSIONS, apps.mapNotNull { it.grantedPermissionsCount })
            }

            REQUESTED_PERMISSIONS -> {
                calculatePermissionSummary(REQUESTED_PERMISSIONS, apps.mapNotNull { it.requestedPermissionsCount })
            }

            VERSION, FIRST_INSTALLED, LAST_UPDATED, LAST_USED, PACKAGE_MANAGER -> {
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
}
