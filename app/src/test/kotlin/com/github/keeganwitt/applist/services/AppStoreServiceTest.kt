package com.github.keeganwitt.applist.services

import android.util.Log
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class AppStoreServiceTest {
    private lateinit var httpClient: OkHttpClient
    private lateinit var service: PlayStoreService

    @Before
    fun setup() {
        httpClient = mockk(relaxed = true)
        service = PlayStoreService(httpClient)
    }

    @Test
    fun `given Google Play installer and newCall throws exception, when existsInAppStore called, then returns null and logs warning`() = runBlocking {
        mockkStatic(Log::class)
        try {
            val exception = RuntimeException("newCall error")
            every { httpClient.newCall(any()) } throws exception
            every { Log.w(any(), any<String>(), any()) } returns 0

            val packageName = "com.test.app"
            val result = service.existsInAppStore(packageName, "com.android.vending")

            assertNull(result)
            verify {
                Log.w(
                    "AppStoreService",
                    "Unable to make HTTP request to https://play.google.com/store/apps/details?id=$packageName",
                    exception
                )
            }
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
    fun `given non-Google Play installer, when existsInAppStore called, then returns null`() = runBlocking {
        val result = service.existsInAppStore("com.test.app", "com.amazon.venezia")
        assertNull(result)
    }

    @Test
    fun `given Google Play installer and successful response, when existsInAppStore called, then returns true`() = runBlocking {
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
    fun `given Google Play installer and unsuccessful response, when existsInAppStore called, then returns false`() = runBlocking {
        val call = mockk<Call>()
        val response = mockk<Response>()

        mockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
        try {
            every { response.isSuccessful } returns false
            every { response.close() } returns Unit
            coEvery { call.await() } returns response
            every { httpClient.newCall(any()) } returns call

            val result = service.existsInAppStore("com.test.app", "com.android.vending")

            assertEquals(false, result)
        } finally {
            unmockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
        }
    }

    @Test
    fun `given Google Play installer and network error, when existsInAppStore called, then returns null and logs warning`() = runBlocking {
        val call = mockk<Call>()
        val exception = IOException("Network error")

        mockkStatic(Log::class)
        mockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
        try {
            coEvery { call.await() } throws exception
            every { httpClient.newCall(any()) } returns call
            every { Log.w(any(), any<String>(), any()) } returns 0

            val packageName = "com.test.app"
            val result = service.existsInAppStore(packageName, "com.android.vending")

            assertNull(result)
            verify {
                Log.w(
                    "AppStoreService",
                    "Unable to make HTTP request to https://play.google.com/store/apps/details?id=$packageName",
                    exception
                )
            }
        } finally {
            unmockkStatic(Log::class)
            unmockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
        }
    }

    @Test
    fun `given cached result, when existsInAppStore called again, then returns cached value without network call`() = runBlocking {
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
    fun `given package name, when appStoreLink called, then returns correct Play Store URL`() {
        val result = service.appStoreLink("com.test.app")
        assertEquals("https://play.google.com/store/apps/details?id=com.test.app", result)
    }

    @Test
    fun `given known installers, when installerDisplayName called, then returns expected name`() {
        assertEquals("Amazon Appstore", service.installerDisplayName("com.amazon.venezia"))
        assertEquals("APK", service.installerDisplayName("com.google.android.packageinstaller"))
        assertEquals("Aptoide", service.installerDisplayName("cm.aptoide.pt"))
        assertEquals("F-Droid", service.installerDisplayName("org.fdroid.fdroid"))
        assertEquals("Blackberry World", service.installerDisplayName("net.rim.bb.appworld"))
        assertEquals("Cafe Bazaar", service.installerDisplayName("com.farsitel.bazaar"))
        assertEquals("Galaxy Store", service.installerDisplayName("com.sec.android.app.samsungapps"))
        assertEquals("Google Play", service.installerDisplayName("com.android.vending"))
        assertEquals("Huawei App Gallery", service.installerDisplayName("com.huawei.appmarket"))
        assertEquals("Mi Store", service.installerDisplayName("com.xiaomi.market"))
        assertEquals("OnePlus Clone Phone", service.installerDisplayName("com.oneplus.backuprestore"))
        assertEquals("Samsung Smart Switch", service.installerDisplayName("com.sec.android.easyMover"))
        assertEquals("SlideME Marketplace", service.installerDisplayName("com.slideme.sam.manager"))
        assertEquals("Tencent Appstore", service.installerDisplayName("com.tencent.android.qqdownloader"))
        assertEquals("Yandex Appstore", service.installerDisplayName("com.yandex.store"))
        assertEquals("Aurora Store", service.installerDisplayName("com.aurora.store"))
        assertEquals("QooApp", service.installerDisplayName("com.qooapp"))
        assertEquals("QooApp", service.installerDisplayName("com.qooapp.qoohelper"))
        assertEquals("TapTap", service.installerDisplayName("com.taptap"))
        assertEquals("TapTap", service.installerDisplayName("com.taptap.global"))
        assertEquals("APKPure", service.installerDisplayName("com.apkpure.aegon"))
        assertEquals("Uptodown", service.installerDisplayName("com.uptodown.android.marketplace"))
        assertEquals("HeyTap", service.installerDisplayName("com.heytap.market"))
        assertEquals("OPPO App Market", service.installerDisplayName("com.oppo.market"))
        assertEquals("Vivo App Store", service.installerDisplayName("com.vivo.appstore"))
        assertEquals("Droid-ify", service.installerDisplayName("com.looker.droidify"))
        assertEquals("Neo Store", service.installerDisplayName("com.machaiv3lli.fdroid"))
    }

    @Test
    fun `given Google Play installer and unexpected exception, when existsInAppStore called, then returns null and logs warning`() = runBlocking {
        mockkStatic(Log::class)
        mockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
        try {
            val call = mockk<Call>()
            val exception = RuntimeException("Unexpected error")

            coEvery { call.await() } throws exception
            every { httpClient.newCall(any()) } returns call
            every { Log.w(any(), any<String>(), any()) } returns 0

            val packageName = "com.test.app"
            val result = service.existsInAppStore(packageName, "com.android.vending")

            assertNull(result)
            verify {
                Log.w(
                    "AppStoreService",
                    "Unable to make HTTP request to https://play.google.com/store/apps/details?id=$packageName",
                    exception
                )
            }
        } finally {
            unmockkStatic(Log::class)
            unmockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
        }
    }

    @Test
    fun `given null installer, when existsInAppStore called, then returns null`() = runBlocking {
        val result = service.existsInAppStore("com.test.app", null)
        assertNull(result)
    }

    @Test
    fun `given Google Play installer and newCall throws IOException, when existsInAppStore called, then returns null and logs warning`() = runBlocking {
        mockkStatic(Log::class)
        try {
            val exception = IOException("newCall error")
            every { httpClient.newCall(any()) } throws exception
            every { Log.w(any(), any<String>(), any()) } returns 0

            val packageName = "com.test.app"
            val result = service.existsInAppStore(packageName, "com.android.vending")

            assertNull(result)
            verify {
                Log.w(
                    "AppStoreService",
                    "Unable to make HTTP request to https://play.google.com/store/apps/details?id=$packageName",
                    exception
                )
            }
        } finally {
            unmockkStatic(Log::class)
        }
    }

    @Test
    fun `given Google Play installer and SecurityException, when existsInAppStore called, then returns null and logs warning`() = runBlocking {
        mockkStatic(Log::class)
        mockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
        try {
            val call = mockk<Call>()
            val exception = SecurityException("Security error")

            coEvery { call.await() } throws exception
            every { httpClient.newCall(any()) } returns call
            every { Log.w(any(), any<String>(), any()) } returns 0

            val packageName = "com.test.app"
            val result = service.existsInAppStore(packageName, "com.android.vending")

            assertNull(result)
            verify {
                Log.w(
                    "AppStoreService",
                    "Unable to make HTTP request to https://play.google.com/store/apps/details?id=$packageName",
                    exception
                )
            }
        } finally {
            unmockkStatic(Log::class)
            unmockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
        }
    }

    @Test
    fun `given network error, when existsInAppStore called again, then network call is retried`() = runBlocking {
        val call = mockk<Call>()
        val response = mockk<Response>()

        mockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
        try {
            // First call fails
            coEvery { call.await() } throws IOException("Network error")
            every { httpClient.newCall(any()) } returns call
            val result1 = service.existsInAppStore("com.test.app", "com.android.vending")
            assertNull(result1)

            // Second call succeeds
            every { response.isSuccessful } returns true
            every { response.close() } returns Unit
            coEvery { call.await() } returns response
            val result2 = service.existsInAppStore("com.test.app", "com.android.vending")
            assertEquals(true, result2)

            verify(exactly = 2) { httpClient.newCall(any()) }
        } finally {
            unmockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
        }
    }

    @Test
    fun `given negative result, when existsInAppStore called again, then returns cached value without network call`() = runBlocking {
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
