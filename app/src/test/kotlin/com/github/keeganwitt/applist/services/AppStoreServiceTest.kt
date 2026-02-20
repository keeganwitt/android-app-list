package com.github.keeganwitt.applist.services

import com.github.keeganwitt.applist.utils.await
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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
        every { response.isSuccessful } returns true
        every { response.close() } returns Unit
        coEvery { call.await() } returns response
        every { httpClient.newCall(any()) } returns call

        val result = service.existsInAppStore("com.test.app", "com.android.vending")

        assertTrue(result == true)
    }

    @Test
    fun `given Google Play installer and unsuccessful response, when existsInAppStore called, then returns false`() = runBlocking {
        val call = mockk<Call>()
        val response = mockk<Response>()

        mockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
        every { response.isSuccessful } returns false
        every { response.close() } returns Unit
        coEvery { call.await() } returns response
        every { httpClient.newCall(any()) } returns call

        val result = service.existsInAppStore("com.test.app", "com.android.vending")

        assertFalse(result == true)
    }

    @Test
    fun `given Google Play installer and network error, when existsInAppStore called, then returns null`() = runBlocking {
        val call = mockk<Call>()

        mockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
        coEvery { call.await() } throws IOException("Network error")
        every { httpClient.newCall(any()) } returns call

        val result = service.existsInAppStore("com.test.app", "com.android.vending")

        assertNull(result)
    }

    @Test
    fun `given cached result, when existsInAppStore called again, then returns cached value without network call`() = runBlocking {
        val call = mockk<Call>()
        val response = mockk<Response>()

        mockkStatic("com.github.keeganwitt.applist.utils.OkHttpExtensionsKt")
        every { response.isSuccessful } returns true
        every { response.close() } returns Unit
        coEvery { call.await() } returns response
        every { httpClient.newCall(any()) } returns call

        val result1 = service.existsInAppStore("com.test.app", "com.android.vending")
        val result2 = service.existsInAppStore("com.test.app", "com.android.vending")

        assertTrue(result1 == true)
        assertTrue(result2 == true)
    }

    @Test
    fun `given package name, when appStoreLink called, then returns correct Play Store URL`() {
        val result = service.appStoreLink("com.test.app")
        assertEquals("https://play.google.com/store/apps/details?id=com.test.app", result)
    }

    @Test
    fun `given known installers, when installerDisplayName called, then returns expected name`() {
        assertEquals("Amazon Appstore", service.installerDisplayName("com.amazon.venezia"))
        assertEquals("F-Droid", service.installerDisplayName("org.fdroid.fdroid"))
        assertEquals("APK", service.installerDisplayName("com.google.android.packageinstaller"))
        assertEquals("Galaxy Store", service.installerDisplayName("com.sec.android.app.samsungapps"))
        assertEquals("Huawei App Gallery", service.installerDisplayName("com.huawei.appmarket"))
        assertEquals("Samsung Smart Switch", service.installerDisplayName("com.sec.android.easyMover"))
        assertEquals("OnePlus Clone Phone", service.installerDisplayName("com.oneplus.backuprestore"))
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
}
