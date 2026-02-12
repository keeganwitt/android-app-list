package com.github.keeganwitt.applist

import android.content.Context
import com.github.keeganwitt.applist.AppInfoField.*

data class SummaryItem(
    val field: AppInfoField,
    val buckets: Map<String, Int>
)

class SummaryCalculator(private val context: Context) {

    fun calculate(apps: List<App>): List<SummaryItem> {
        val summaries = mutableListOf<SummaryItem>()

        // ENABLED
        val enabledMap = apps.groupingBy {
            if (it.enabled) context.getString(R.string.enabled) else context.getString(R.string.disabled)
        }.eachCount()
        summaries.add(SummaryItem(ENABLED, enabledMap))

        // ARCHIVED
        val archivedMap = apps.groupingBy {
            if (it.archived == true) context.getString(R.string.archived) else context.getString(R.string.installed)
        }.eachCount()
        summaries.add(SummaryItem(ARCHIVED, archivedMap))

        // EXISTS_IN_APP_STORE
        val existsMap = apps.groupingBy {
            if (it.existsInStore == true) context.getString(R.string.boolean_true) else context.getString(R.string.boolean_false)
        }.eachCount()
        summaries.add(SummaryItem(EXISTS_IN_APP_STORE, existsMap))

        // SIZE FIELDS
        summaries.add(calculateSizeSummary(APK_SIZE, apps.map { it.sizes.apkBytes }))
        summaries.add(calculateSizeSummary(APP_SIZE, apps.map { it.sizes.appBytes }))
        summaries.add(calculateSizeSummary(DATA_SIZE, apps.map { it.sizes.dataBytes }))
        summaries.add(calculateSizeSummary(CACHE_SIZE, apps.map { it.sizes.cacheBytes }))
        summaries.add(calculateSizeSummary(EXTERNAL_CACHE_SIZE, apps.map { it.sizes.externalCacheBytes }))
        summaries.add(calculateSizeSummary(TOTAL_SIZE, apps.map { it.sizes.totalBytes }))

        // SDK Versions
        summaries.add(calculateSdkSummary(TARGET_SDK, apps.mapNotNull { it.targetSdk }))
        summaries.add(calculateSdkSummary(MIN_SDK, apps.mapNotNull { it.minSdk }))

        // Permissions
        summaries.add(calculatePermissionSummary(GRANTED_PERMISSIONS, apps.mapNotNull { it.grantedPermissionsCount }))
        summaries.add(calculatePermissionSummary(REQUESTED_PERMISSIONS, apps.mapNotNull { it.requestedPermissionsCount }))

        return summaries
    }

    private fun calculateSizeSummary(field: AppInfoField, sizes: List<Long>): SummaryItem {
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
            val key = when {
                size < 10 * 1024 * 1024 -> small
                size < 50 * 1024 * 1024 -> medium
                size < 100 * 1024 * 1024 -> large
                else -> huge
            }
            orderedBuckets[key] = orderedBuckets[key]!! + 1
        }

        return SummaryItem(field, orderedBuckets)
    }

    private fun calculateSdkSummary(field: AppInfoField, sdks: List<Int>): SummaryItem {
        val buckets = sdks.groupingBy { it.toString() }
            .eachCount()
            .toSortedMap(compareByDescending { it.toIntOrNull() ?: 0 })
        return SummaryItem(field, buckets)
    }

    private fun calculatePermissionSummary(field: AppInfoField, counts: List<Int>): SummaryItem {
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
            val key = when {
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
