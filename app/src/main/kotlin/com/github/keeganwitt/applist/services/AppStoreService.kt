package com.github.keeganwitt.applist.services

import android.util.Log
import com.github.keeganwitt.applist.utils.await
import okhttp3.OkHttpClient
import okhttp3.Request

interface AppStoreService {
    fun installerDisplayName(installerPackageName: String?): String

    suspend fun existsInAppStore(
        packageName: String,
        installerPackageName: String?,
    ): Boolean?

    fun appStoreLink(packageName: String): String?

    fun appStoreLink(
        packageName: String,
        installerPackageName: String?,
    ): String?

    companion object {
        const val AMAZON_APPSTORE = "com.amazon.venezia"
        const val APK = "com.google.android.packageinstaller"
        const val APTOIDE = "cm.aptoide.pt"
        const val BLACKBERRY_WORLD = "net.rim.bb.appworld"
        const val CAFE_BAZAAR = "com.farsitel.bazaar"
        const val GALAXY_STORE = "com.sec.android.app.samsungapps"
        const val GOOGLE_PLAY = "com.android.vending"
        const val HUAWEI_APP_GALLERY = "com.huawei.appmarket"
        const val MI_STORE = "com.xiaomi.market"
        const val ONEPLUS_CLONE_PHONE = "com.oneplus.backuprestore"
        const val SAMSUNG_SMART_SWITCH = "com.sec.android.easyMover"
        const val SLIDEME_MARKETPLACE = "com.slideme.sam.manager"
        const val TENCENT_APPSTORE = "com.tencent.android.qqdownloader"
        const val YANDEX_APPSTORE = "com.yandex.store"
        const val AURORA_STORE = "com.aurora.store"
        const val QOOAPP = "com.qooapp"
        const val QOOAPP_HELPER = "com.qooapp.qoohelper"
        const val TAPTAP = "com.taptap"
        const val TAPTAP_GLOBAL = "com.taptap.global"
        const val APKPURE = "com.apkpure.aegon"
        const val UPTODOWN = "com.uptodown.android.marketplace"
        const val HEYTAP = "com.heytap.market"
        const val OPPO_APP_MARKET = "com.oppo.market"
        const val VIVO_APP_STORE = "com.vivo.appstore"
        const val F_DROID = "org.fdroid.fdroid"
        const val DROIDIFY = "com.looker.droidify"
        const val NEO_STORE = "com.machaiv3lli.fdroid"
        const val GOOGLE_PLAY_ARCHIVE_KEY = "com.android.vending.archive"
    }
}

open class DefaultAppStoreService(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val crashReporter: com.github.keeganwitt.applist.CrashReporter? = null,
) : AppStoreService {
    private val cache = mutableMapOf<String, Boolean?>()

    override fun installerDisplayName(installerPackageName: String?): String {
        if (installerPackageName == null) return "Unknown"
        return INSTALLER_DISPLAY_NAMES[installerPackageName] ?: "Unknown ($installerPackageName)"
    }

    override suspend fun existsInAppStore(
        packageName: String,
        installerPackageName: String?,
    ): Boolean? {
        val isSupportedStore = installerPackageName in SUPPORTED_INSTALLERS
        if (!isSupportedStore) return null

        val cacheKey = "$packageName:$installerPackageName"
        cache[cacheKey]?.let { return it }

        val url = appStoreLink(packageName, installerPackageName) ?: return null
        val request = Request.Builder().url(url).build()

        val response =
            try {
                httpClient.newCall(request).await()
            } catch (e: Exception) {
                val message = "Unable to make HTTP request to $url"
                Log.w(TAG, message, e)
                crashReporter?.recordException(e, message)
                null
            } ?: run {
                cache[cacheKey] = null
                return null
            }

        val result =
            try {
                response.isSuccessful
            } finally {
                response.close()
            }

        cache[cacheKey] = result
        return result
    }

    override fun appStoreLink(packageName: String): String? = appStoreLink(packageName, AppStoreService.GOOGLE_PLAY)

    override fun appStoreLink(
        packageName: String,
        installerPackageName: String?,
    ): String? =
        when (installerPackageName) {
            AppStoreService.GOOGLE_PLAY, AppStoreService.AURORA_STORE -> "https://play.google.com/store/apps/details?id=$packageName"
            AppStoreService.AMAZON_APPSTORE -> "https://www.amazon.com/gp/mas/dl/android?p=$packageName"
            AppStoreService.GALAXY_STORE -> "https://galaxystore.samsung.com/detail/$packageName"
            AppStoreService.HUAWEI_APP_GALLERY -> "https://appgallery.cloud.huawei.com/appDetail?pkgName=$packageName"
            AppStoreService.F_DROID, AppStoreService.DROIDIFY, AppStoreService.NEO_STORE -> "https://f-droid.org/packages/$packageName/"
            AppStoreService.CAFE_BAZAAR -> "https://cafebazaar.ir/app/$packageName"
            else -> null
        }

    companion object {
        private val TAG = AppStoreService::class.java.simpleName
        private val SUPPORTED_INSTALLERS =
            setOf(
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
        private val INSTALLER_DISPLAY_NAMES =
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
    }
}
