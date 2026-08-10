package com.github.keeganwitt.applist

import org.junit.Test
import kotlin.system.measureTimeMillis

class FilterBenchmark {

    private fun createTestApp(i: Int): App = App(
        packageName = "com.test.app$i",
        name = "App $i",
        versionName = "1.0.$i",
        archived = false,
        minSdk = 24,
        targetSdk = 33,
        firstInstalled = System.currentTimeMillis(),
        lastUpdated = System.currentTimeMillis(),
        lastUsed = System.currentTimeMillis(),
        sizes = StorageUsage(apkBytes = i.toLong() * 1024),
        installerName = "Google Play",
        existsInStore = true,
        grantedPermissionsCount = 5,
        requestedPermissionsCount = 10,
        enabled = true,
        isDetailed = true
    )

    private fun mapToItem(app: App): AppItemUiModel = AppItemUiModel(
        packageName = app.packageName,
        appName = app.name,
        infoText = app.versionName ?: "",
        isLoading = false
    )

    @Test
    fun benchmarkFiltering() {
        val appCount = 2000
        val allApps = List(appCount) { createTestApp(it) }
        val cachedMappedItems = allApps.map { mapToItem(it) }
        val query = "App 4" // Should match some apps

        val iterations = 100

        // Baseline (Original approach before optimization)
        val baselineTime = measureTimeMillis {
            repeat(iterations) {
                val filtered = cachedMappedItems.filter { item ->
                    item.appName.contains(query, ignoreCase = true) ||
                            item.packageName.contains(query, ignoreCase = true) ||
                            item.infoText.contains(query, ignoreCase = true)
                }
                val filteredPackageNames = filtered.map { it.packageName }.toSet()
                val filteredApps = allApps.filter { app -> app.packageName in filteredPackageNames }

                if (filtered.size != filteredApps.size) throw RuntimeException("Mismatch")
            }
        }

        // Optimized approach (Using indices, avoiding set creation and second pass)
        val optimizedTime = measureTimeMillis {
            repeat(iterations) {
                val filteredIndices = mutableListOf<Int>()
                val filteredItems = mutableListOf<AppItemUiModel>()
                for (i in cachedMappedItems.indices) {
                    val item = cachedMappedItems[i]
                    if (item.appName.contains(query, ignoreCase = true) ||
                        item.packageName.contains(query, ignoreCase = true) ||
                        item.infoText.contains(query, ignoreCase = true)
                    ) {
                        filteredItems.add(item)
                        filteredIndices.add(i)
                    }
                }
                val filteredApps = filteredIndices.map { allApps[it] }

                if (filteredItems.size != filteredApps.size) throw RuntimeException("Mismatch")
            }
        }

        println("Benchmark results for $appCount apps over $iterations iterations:")
        println("Baseline time: $baselineTime ms")
        println("Optimized time: $optimizedTime ms")
        println("Improvement: ${baselineTime - optimizedTime} ms (${"%.2f".format((baselineTime - optimizedTime).toDouble() / baselineTime * 100)}%)")
    }
}
