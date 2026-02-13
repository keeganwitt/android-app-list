package com.github.keeganwitt.applist

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.github.keeganwitt.applist.services.AppStoreService
import com.github.keeganwitt.applist.services.PackageService
import com.github.keeganwitt.applist.services.StorageService
import com.github.keeganwitt.applist.services.UsageStatsService
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

class AppRepositoryConcurrencyTest {

    @Test
    fun `verify concurrency limit`() = runBlocking {
        val packageService: PackageService = mockk(relaxed = true)
        val usageStatsService: UsageStatsService = mockk(relaxed = true)
        val storageService: StorageService = mockk(relaxed = true)
        val appStoreService: AppStoreService = mockk(relaxed = true)
        val crashReporter: CrashReporter = mockk(relaxed = true)

        val repository = AndroidAppRepository(
            packageService,
            usageStatsService,
            storageService,
            appStoreService,
            crashReporter
        )

        val appCount = 200
        val apps = (1..appCount).map { i ->
            ApplicationInfo().apply {
                packageName = "com.app.$i"
                flags = 0
                enabled = true
            }
        }

        every { packageService.getInstalledApplications(any<Long>()) } returns apps
        every { packageService.loadLabel(any()) } returns "App"
        every { packageService.getLaunchIntentForPackage(any()) } returns mockk()

        // Concurrency tracker
        val activeCoroutines = AtomicInteger(0)
        val maxConcurrency = AtomicInteger(0)

        // Mock a heavy operation
        every { packageService.getPackageInfo(any()) } answers {
            val current = activeCoroutines.incrementAndGet()
            maxConcurrency.updateAndGet { prev -> max(prev, current) }

            // Simulate blocking work
            Thread.sleep(10)

            activeCoroutines.decrementAndGet()
            PackageInfo().apply {
                versionName = "1.0"
                firstInstallTime = 0
                lastUpdateTime = 0
            }
        }

        // Run on IO dispatcher to allow concurrency
        withContext(Dispatchers.IO) {
            repository.loadApps(
                field = AppInfoField.VERSION,
                showSystemApps = true,
                descending = false,
                reload = false
            ).toList()
        }

        val maxObserved = maxConcurrency.get()
        assertTrue("Max concurrency should be limited to 4, but was $maxObserved", maxObserved <= 4)
    }
}
