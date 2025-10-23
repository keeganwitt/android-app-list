package com.github.keeganwitt.applist.services

import okhttp3.OkHttpClient
import okhttp3.Request

interface AppStoreService {
    fun installerDisplayName(installerPackageName: String?): String {
        return when (installerPackageName) {
            "com.amazon.venezia" -> "Amazon Appstore"
            "com.google.android.packageinstaller" -> "APK"
            "cm.aptoide.pt" -> "Aptoide"
            "org.fdroid.fdroid" -> "F-Droid"
            "net.rim.bb.appworld" -> "Blackberry World"
            "com.farsitel.bazaar" -> "Cafe Bazaar"
            "com.sec.android.app.samsungapps" -> "Galaxy Store"
            "com.android.vending" -> "Google Play"
            "com.huawei.appmarket" -> "Huawei App Gallery"
            "com.xiaomi.market" -> "Mi Store"
            "com.oneplus.backuprestore" -> "OnePlus Clone Phone"
            "com.sec.android.easyMover" -> "Samsung Smart Switch"
            "com.slideme.sam.manager" -> "SlideME Marketplace"
            "com.tencent.android.qqdownloader" -> "Tencent Appstore"
            "com.yandex.store" -> "Yandex Appstore"
            null -> "Unknown"
            else -> "Unknown ($installerPackageName)"
        }
    }
    fun existsInAppStore(packageName: String, installerPackageName: String?): Boolean?
    fun appStoreLink(packageName: String): String
}

class PlayStoreService(private val httpClient: OkHttpClient = OkHttpClient()) : AppStoreService {
    private val cache = mutableMapOf<String, Boolean?>()

    override fun existsInAppStore(packageName: String, installerPackageName: String?): Boolean? {
        if (installerPackageName != "com.android.vending") return null
        cache[packageName]?.let { return it }
        val url = appStoreLink(packageName)
        val request = Request.Builder().url(url).build()
        val result = try {
            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (_: Exception) {
            null
        }
        cache[packageName] = result
        return result
    }

    override fun appStoreLink(packageName: String): String =
        "https://play.google.com/store/apps/details?id=$packageName"
}
