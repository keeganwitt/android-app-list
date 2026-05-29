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
    private lateinit var service: DefaultAppStoreService

    @Before
    fun setup() {
        httpClient = mockk(relaxed = true)
        crashReporter = mockk(relaxed = true)
        service = DefaultAppStoreService(httpClient, crashReporter)
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
                val result = service.existsInAppStore(packageName, AppStoreService.GOOGLE_PLAY)

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
                val result = service.existsInAppStore(packageName, AppStoreService.GOOGLE_PLAY)

                assertNull(result)
                verify { crashReporter.recordException(any(), any()) }
            } finally {
                unmockkStatic(Log::class)
            }
        }

    @Test
    fun `given Google Play installer, when installerDisplayName called, then returns Google Play`() {
        val result = service.installerDisplayName(AppStoreService.GOOGLE_PLAY)
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
    fun `given unsupported installer, when existsInAppStore called, then returns null`() =
        runBlocking {
            val result = service.existsInAppStore("com.test.app", AppStoreService.APK)
            assertNull(result)
        }

    @Test
    fun `given supported installers and successful response, when existsInAppStore called, then returns true`() =
        runBlocking {
            val call = mockk<Call>()
            val response = mockk<Response>()

            mockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
            try {
                every { response.isSuccessful } returns true
                every { response.close() } returns Unit
                coEvery { call.await() } returns response
                every { httpClient.newCall(any()) } returns call

                val supportedInstallers =
                    listOf(
                        AppStoreService.GOOGLE_PLAY,
                        AppStoreService.AURORA_STORE,
                        AppStoreService.AMAZON_APPSTORE,
                        AppStoreService.GALAXY_STORE,
                        AppStoreService.HUAWEI_APP_GALLERY,
                        AppStoreService.F_DROID,
                        AppStoreService.DROIDIFY,
                        AppStoreService.NEO_STORE,
                        AppStoreService.CAFE_BAZAAR,
                    )

                supportedInstallers.forEach { installer ->
                    val result = service.existsInAppStore("com.test.app", installer)
                    assertEquals(true, result)
                }
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
    fun `given package name and Amazon Appstore installer, when appStoreLink called, then returns correct Amazon URL`() {
        val result = service.appStoreLink("com.test.app", AppStoreService.AMAZON_APPSTORE)
        assertEquals("https://www.amazon.com/gp/mas/dl/android?p=com.test.app", result)
    }

    @Test
    fun `given package name and Galaxy Store installer, when appStoreLink called, then returns correct Galaxy Store URL`() {
        val result = service.appStoreLink("com.test.app", AppStoreService.GALAXY_STORE)
        assertEquals("https://galaxystore.samsung.com/detail/com.test.app", result)
    }

    @Test
    fun `given package name and Huawei App Gallery installer, when appStoreLink called, then returns correct Huawei URL`() {
        val result = service.appStoreLink("com.test.app", AppStoreService.HUAWEI_APP_GALLERY)
        assertEquals("https://appgallery.cloud.huawei.com/appDetail?pkgName=com.test.app", result)
    }

    @Test
    fun `given package name and F-Droid installer, when appStoreLink called, then returns correct F-Droid URL`() {
        val result = service.appStoreLink("com.test.app", AppStoreService.F_DROID)
        assertEquals("https://f-droid.org/packages/com.test.app/", result)
    }

    @Test
    fun `given package name and Aurora Store installer, when appStoreLink called, then returns correct Play Store URL`() {
        val result = service.appStoreLink("com.test.app", AppStoreService.AURORA_STORE)
        assertEquals("https://play.google.com/store/apps/details?id=com.test.app", result)
    }

    @Test
    fun `given package name and Droid-ify installer, when appStoreLink called, then returns correct F-Droid URL`() {
        val result = service.appStoreLink("com.test.app", AppStoreService.DROIDIFY)
        assertEquals("https://f-droid.org/packages/com.test.app/", result)
    }

    @Test
    fun `given package name and Neo Store installer, when appStoreLink called, then returns correct F-Droid URL`() {
        val result = service.appStoreLink("com.test.app", AppStoreService.NEO_STORE)
        assertEquals("https://f-droid.org/packages/com.test.app/", result)
    }

    @Test
    fun `given package name and Cafe Bazaar installer, when appStoreLink called, then returns correct Cafe Bazaar URL`() {
        val result = service.appStoreLink("com.test.app", AppStoreService.CAFE_BAZAAR)
        assertEquals("https://cafebazaar.ir/app/com.test.app", result)
    }

    @Test
    fun `given package name and unknown installer, when appStoreLink called, then returns null`() {
        val result = service.appStoreLink("com.test.app", "com.unknown.installer")
        assertNull(result)
    }

    @Test
    fun `given package name and null installer, when appStoreLink called, then returns null`() {
        val result = service.appStoreLink("com.test.app", null)
        assertNull(result)
    }

    @Test
    fun `given known installers, when installerDisplayName called, then returns expected name`() {
        val installers =
            mapOf(
                AppStoreService.AMAZON_APPSTORE to "Amazon Appstore",
                AppStoreService.APK to "APK",
                AppStoreService.APTOIDE to "Aptoide",
                AppStoreService.F_DROID to "F-Droid",
                AppStoreService.BLACKBERRY_WORLD to "Blackberry World",
                AppStoreService.CAFE_BAZAAR to "Cafe Bazaar",
                AppStoreService.GALAXY_STORE to "Galaxy Store",
                AppStoreService.GOOGLE_PLAY to "Google Play",
                AppStoreService.HUAWEI_APP_GALLERY to "Huawei App Gallery",
                AppStoreService.MI_STORE to "Mi Store",
                AppStoreService.ONEPLUS_CLONE_PHONE to "OnePlus Clone Phone",
                AppStoreService.SAMSUNG_SMART_SWITCH to "Samsung Smart Switch",
                AppStoreService.SLIDEME_MARKETPLACE to "SlideME Marketplace",
                AppStoreService.TENCENT_APPSTORE to "Tencent Appstore",
                AppStoreService.YANDEX_APPSTORE to "Yandex Appstore",
                AppStoreService.AURORA_STORE to "Aurora Store",
                AppStoreService.QOOAPP to "QooApp",
                AppStoreService.QOOAPP_HELPER to "QooApp",
                AppStoreService.TAPTAP to "TapTap",
                AppStoreService.TAPTAP_GLOBAL to "TapTap",
                AppStoreService.APKPURE to "APKPure",
                AppStoreService.UPTODOWN to "Uptodown",
                AppStoreService.HEYTAP to "HeyTap",
                AppStoreService.OPPO_APP_MARKET to "OPPO App Market",
                AppStoreService.VIVO_APP_STORE to "Vivo App Store",
                AppStoreService.DROIDIFY to "Droid-ify",
                AppStoreService.NEO_STORE to "Neo Store",
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

                val result1 = service.existsInAppStore("com.test.app", AppStoreService.GOOGLE_PLAY)
                val result2 = service.existsInAppStore("com.test.app", AppStoreService.GOOGLE_PLAY)

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
                val serviceWithNullReporter = DefaultAppStoreService(httpClient, null)
                every { httpClient.newCall(any()) } throws RuntimeException("Error")
                every { Log.w(any(), any<String>(), any()) } returns 0

                val result = serviceWithNullReporter.existsInAppStore("com.test.app", AppStoreService.GOOGLE_PLAY)

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

                val result1 = service.existsInAppStore("com.test.app", AppStoreService.GOOGLE_PLAY)
                val result2 = service.existsInAppStore("com.test.app", AppStoreService.GOOGLE_PLAY)

                assertEquals(false, result1)
                assertEquals(false, result2)
                verify(exactly = 1) { httpClient.newCall(any()) }
            } finally {
                unmockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
            }
        }

    @Test
    fun `can instantiate with default constructor`() {
        DefaultAppStoreService()
    }

    @Test
    fun `given supported installer but null appStoreLink, when existsInAppStore called, then returns null`() =
        runBlocking {
            val serviceWithNullLink =
                object : DefaultAppStoreService(httpClient, crashReporter) {
                    override fun appStoreLink(
                        packageName: String,
                        installerPackageName: String?,
                    ): String? = null
                }
            val result = serviceWithNullLink.existsInAppStore("com.test.app", AppStoreService.GOOGLE_PLAY)
            assertNull(result)
        }
}
