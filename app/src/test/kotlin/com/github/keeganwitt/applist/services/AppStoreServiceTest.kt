package com.github.keeganwitt.applist.services

import android.util.Log
import com.github.keeganwitt.applist.CrashReporter
import com.github.keeganwitt.applist.utils.await
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class AppStoreServiceTest {
    private lateinit var httpClient: OkHttpClient
    private lateinit var crashReporter: CrashReporter
    private lateinit var service: PlayStoreService

    @Before
    fun setup() {
        httpClient = mockk(relaxed = true)
        crashReporter = mockk(relaxed = true)
        service = PlayStoreService(httpClient, crashReporter)
    }

    @Test
    fun `given Google Play installer and generic exception, when existsInAppStore called, then records exception to crash reporter`() =
        runBlocking {
            mockkStatic(Log::class)
            try {
                val exception = RuntimeException("generic error")
                every { httpClient.newCall(any()) } throws exception
                every { Log.w(any(), any<String>(), any()) } returns 0

                val packageName = "com.test.app"
                val result = service.existsInAppStore(packageName, "com.android.vending")

                assertNull(result)
                verify { crashReporter.recordException(exception, match { it.contains("Unable to make HTTP request") }) }
            } finally {
                unmockkStatic(Log::class)
            }
        }

    @Test
    fun `given Google Play installer and IOException, when existsInAppStore called, then does NOT record exception to crash reporter`() =
        runBlocking {
            mockkStatic(Log::class)
            try {
                val exception = IOException("Network error")
                every { httpClient.newCall(any()) } throws exception
                every { Log.w(any(), any<String>(), any()) } returns 0

                val packageName = "com.test.app"
                val result = service.existsInAppStore(packageName, "com.android.vending")

                assertNull(result)
                verify(exactly = 0) { crashReporter.recordException(any(), any()) }
            } finally {
                unmockkStatic(Log::class)
            }
        }

    @Test
    fun `given Google Play installer, when installerDisplayName called, then returns Google Play`() {
        val result = service.installerDisplayName("com.android.vending")
        assertEquals("Google Play", result)
    }

    @Test
    fun `given null installer, when installerDisplayName called, then returns Unknown`() {
        val result = service.installerDisplayName(null)
        assertEquals("Unknown", result)
    }

    @Test
    fun `given unknown installer, when installerDisplayName called, then returns Unknown with package name`() {
        val result = service.installerDisplayName("com.unknown.installer")
        assertEquals("Unknown (com.unknown.installer)", result)
    }

    @Test
    fun `given non-Google Play installer, when existsInAppStore called, then returns null`() =
        runBlocking {
            val result = service.existsInAppStore("com.test.app", "com.amazon.venezia")
            assertNull(result)
        }

    @Test
    fun `given Google Play installer and successful response, when existsInAppStore called, then returns true`() =
        runBlocking {
            val call = mockk<Call>()
            val response = mockk<Response>()

            mockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
            try {
                every { response.isSuccessful } returns true
                every { response.close() } returns Unit
                coEvery { call.await() } returns response
                every { httpClient.newCall(any()) } returns call

                val result = service.existsInAppStore("com.test.app", "com.android.vending")

                assertEquals(true, result)
            } finally {
                unmockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
            }
        }

    @Test
    fun `given package name, when appStoreLink called, then returns correct Play Store URL`() {
        val result = service.appStoreLink("com.test.app")
        assertEquals("https://play.google.com/store/apps/details?id=com.test.app", result)
    }

    @Test
    fun `given known installers, when installerDisplayName called, then returns expected name`() {
        val installers =
            mapOf(
                "com.amazon.venezia" to "Amazon Appstore",
                "com.google.android.packageinstaller" to "APK",
                "cm.aptoide.pt" to "Aptoide",
                "org.fdroid.fdroid" to "F-Droid",
                "net.rim.bb.appworld" to "Blackberry World",
                "com.farsitel.bazaar" to "Cafe Bazaar",
                "com.sec.android.app.samsungapps" to "Galaxy Store",
                "com.android.vending" to "Google Play",
                "com.huawei.appmarket" to "Huawei App Gallery",
                "com.xiaomi.market" to "Mi Store",
                "com.oneplus.backuprestore" to "OnePlus Clone Phone",
                "com.sec.android.easyMover" to "Samsung Smart Switch",
                "com.slideme.sam.manager" to "SlideME Marketplace",
                "com.tencent.android.qqdownloader" to "Tencent Appstore",
                "com.yandex.store" to "Yandex Appstore",
                "com.aurora.store" to "Aurora Store",
                "com.qooapp" to "QooApp",
                "com.qooapp.qoohelper" to "QooApp",
                "com.taptap" to "TapTap",
                "com.taptap.global" to "TapTap",
                "com.apkpure.aegon" to "APKPure",
                "com.uptodown.android.marketplace" to "Uptodown",
                "com.heytap.market" to "HeyTap",
                "com.oppo.market" to "OPPO App Market",
                "com.vivo.appstore" to "Vivo App Store",
                "com.looker.droidify" to "Droid-ify",
                "com.machaiv3lli.fdroid" to "Neo Store",
            )

        installers.forEach { (pkg, name) ->
            assertEquals(name, service.installerDisplayName(pkg))
        }
    }

    @Test
    fun `given Google Play installer, when existsInAppStore called twice, then result is cached`() =
        runBlocking {
            val call = mockk<Call>()
            val response = mockk<Response>()

            mockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
            try {
                every { response.isSuccessful } returns true
                every { response.close() } returns Unit
                coEvery { call.await() } returns response
                every { httpClient.newCall(any()) } returns call

                val result1 = service.existsInAppStore("com.test.app", "com.android.vending")
                val result2 = service.existsInAppStore("com.test.app", "com.android.vending")

                assertEquals(true, result1)
                assertEquals(true, result2)
                verify(exactly = 1) { httpClient.newCall(any()) }
            } finally {
                unmockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
            }
        }

    @Test
    fun `given null crash reporter and exception, when existsInAppStore called, then handles it safely`() =
        runBlocking {
            mockkStatic(Log::class)
            try {
                val serviceWithNullReporter = PlayStoreService(httpClient, null)
                every { httpClient.newCall(any()) } throws RuntimeException("Error")
                every { Log.w(any(), any<String>(), any()) } returns 0

                val result = serviceWithNullReporter.existsInAppStore("com.test.app", "com.android.vending")

                assertNull(result)
            } finally {
                unmockkStatic(Log::class)
            }
        }

    @Test
    fun `given null installer, when existsInAppStore called, then returns null`() =
        runBlocking {
            val result = service.existsInAppStore("com.test.app", null)
            assertNull(result)
        }

    @Test
    fun `given failing request, when existsInAppStore called twice, then failure is cached`() =
        runBlocking {
            val call = mockk<Call>()
            val response = mockk<Response>()

            mockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
            try {
                every { response.isSuccessful } returns false
                every { response.close() } returns Unit
                coEvery { call.await() } returns response
                every { httpClient.newCall(any()) } returns call

                val result1 = service.existsInAppStore("com.test.app", "com.android.vending")
                val result2 = service.existsInAppStore("com.test.app", "com.android.vending")

                assertEquals(false, result1)
                assertEquals(false, result2)
                verify(exactly = 1) { httpClient.newCall(any()) }
            } finally {
                unmockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
            }
        }

    @Test
    fun `can instantiate with default constructor`() {
        PlayStoreService()
    }
}
