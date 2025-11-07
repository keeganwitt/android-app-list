package com.github.keeganwitt.applist.services

import io.mockk.every
import io.mockk.mockk
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
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
    fun `given Amazon Appstore installer, when installerDisplayName called, then returns Amazon Appstore`() {
        val result = service.installerDisplayName("com.amazon.venezia")
        assertEquals("Amazon Appstore", result)
    }

    @Test
    fun `given F-Droid installer, when installerDisplayName called, then returns F-Droid`() {
        val result = service.installerDisplayName("org.fdroid.fdroid")
        assertEquals("F-Droid", result)
    }

    @Test
    fun `given APK installer, when installerDisplayName called, then returns APK`() {
        val result = service.installerDisplayName("com.google.android.packageinstaller")
        assertEquals("APK", result)
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
    fun `given Galaxy Store installer, when installerDisplayName called, then returns Galaxy Store`() {
        val result = service.installerDisplayName("com.sec.android.app.samsungapps")
        assertEquals("Galaxy Store", result)
    }

    @Test
    fun `given Huawei App Gallery installer, when installerDisplayName called, then returns Huawei App Gallery`() {
        val result = service.installerDisplayName("com.huawei.appmarket")
        assertEquals("Huawei App Gallery", result)
    }

    @Test
    fun `given non-Google Play installer, when existsInAppStore called, then returns null`() {
        val result = service.existsInAppStore("com.test.app", "com.amazon.venezia")
        assertNull(result)
    }

    @Test
    fun `given Google Play installer and successful response, when existsInAppStore called, then returns true`() {
        val call = mockk<Call>()
        val response = mockk<Response>()

        every { response.isSuccessful } returns true
        every { response.close() } returns Unit
        every { call.execute() } returns response
        every { httpClient.newCall(any()) } returns call

        val result = service.existsInAppStore("com.test.app", "com.android.vending")

        assertTrue(result == true)
    }

    @Test
    fun `given Google Play installer and unsuccessful response, when existsInAppStore called, then returns false`() {
        val call = mockk<Call>()
        val response = mockk<Response>()

        every { response.isSuccessful } returns false
        every { response.close() } returns Unit
        every { call.execute() } returns response
        every { httpClient.newCall(any()) } returns call

        val result = service.existsInAppStore("com.test.app", "com.android.vending")

        assertFalse(result == true)
    }

    @Test
    fun `given Google Play installer and network error, when existsInAppStore called, then returns null`() {
        val call = mockk<Call>()

        every { call.execute() } throws IOException("Network error")
        every { httpClient.newCall(any()) } returns call

        val result = service.existsInAppStore("com.test.app", "com.android.vending")

        assertNull(result)
    }

    @Test
    fun `given cached result, when existsInAppStore called again, then returns cached value without network call`() {
        val call = mockk<Call>()
        val response = mockk<Response>()

        every { response.isSuccessful } returns true
        every { response.close() } returns Unit
        every { call.execute() } returns response
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
    fun `given Samsung Smart Switch installer, when installerDisplayName called, then returns Samsung Smart Switch`() {
        val result = service.installerDisplayName("com.sec.android.easyMover")
        assertEquals("Samsung Smart Switch", result)
    }

    @Test
    fun `given OnePlus Clone Phone installer, when installerDisplayName called, then returns OnePlus Clone Phone`() {
        val result = service.installerDisplayName("com.oneplus.backuprestore")
        assertEquals("OnePlus Clone Phone", result)
    }
}
