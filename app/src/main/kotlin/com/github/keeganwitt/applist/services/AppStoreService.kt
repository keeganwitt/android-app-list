package com.github.keeganwitt.applist.services

import android.util.Log
import com.github.keeganwitt.applist.utils.await
import okhttp3.OkHttpClient
import okhttp3.Request

interface AppStoreService {
    fun installerDisplayName(installerPackageName: String?): String =
        installerPackageName?.let { INSTALLER_NAMES[it] } ?: when (installerPackageName) {
            null -> "Unknown"
            else -> "Unknown ($installerPackageName)"
        }

    suspend fun existsInAppStore(
        packageName: String,
        installerPackageName: String?,
    ): Boolean?

    fun appStoreLink(packageName: String): String

    companion object {
        private val INSTALLER_NAMES =
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
    }
}

class PlayStoreService(
    private val httpClient: OkHttpClient = OkHttpClient(),
) : AppStoreService {
    private val cache = mutableMapOf<String, Boolean?>()

    override suspend fun existsInAppStore(
        packageName: String,
        installerPackageName: String?,
    ): Boolean? {
        if (installerPackageName != "com.android.vending") return null
        cache[packageName]?.let { return it }
        val url = appStoreLink(packageName)
        val request = Request.Builder().url(url).build()
        val result =
            try {
                httpClient.newCall(request).await().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                val message = "Unable to make HTTP request to $url"
                Log.w(TAG, message, e)
                null
            }
        cache[packageName] = result
        return result
    }

    override fun appStoreLink(packageName: String): String = "https://play.google.com/store/apps/details?id=$packageName"

    companion object {
        private val TAG = AppStoreService::class.java.simpleName
    }
}
