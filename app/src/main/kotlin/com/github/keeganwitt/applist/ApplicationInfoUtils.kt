package com.github.keeganwitt.applist

import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.storage.StorageManager
import android.text.format.Formatter
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.UUID

object ApplicationInfoUtils {
    private val TAG = ApplicationInfoUtils::class.java.simpleName
    private val httpClient = OkHttpClient()
    private val existsInAppStoreCache: MutableMap<String, Boolean> = HashMap()
    private var lastUsedEpochsCache: Map<String, Long>? = null
    private val dateFormat = SimpleDateFormat.getDateTimeInstance()

    @JvmStatic
    fun getPackageInstaller(
        packageManager: PackageManager,
        applicationInfo: ApplicationInfo
    ): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                packageManager.getInstallSourceInfo(applicationInfo.packageName).installingPackageName
            } catch (e: PackageManager.NameNotFoundException) {
                val message = "Could not get package installer"
                Log.e(TAG, message + " for " + applicationInfo.packageName, e)
                FirebaseCrashlytics.getInstance().log(message)
                FirebaseCrashlytics.getInstance().recordException(e)
                null
            }
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(applicationInfo.packageName)
        }
    }

    @JvmStatic
    fun getPackageInstallerName(installerPackageName: String?): String {
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

    @JvmStatic
    fun getEnabledText(context: Context, applicationInfo: ApplicationInfo): String {
        return if (applicationInfo.enabled) context.getString(R.string.enabled) else context.getString(R.string.disabled)
    }

    @JvmStatic
    fun getApkSizeText(context: Context, applicationInfo: ApplicationInfo): String {
        return Formatter.formatShortFileSize(context, getApkSize(applicationInfo))
    }

    @JvmStatic
    fun isAppArchived(applicationInfo: ApplicationInfo): Boolean? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            applicationInfo.isArchived
        } else {
            null
        }
    }

    @JvmStatic
    fun getAppIsArchivedText(context: Context, applicationInfo: ApplicationInfo): String {
        val archived = isAppArchived(applicationInfo)
        return when (archived) {
            null -> context.getString(R.string.unknown)
            true -> context.getString(R.string.archived)
            false -> context.getString(R.string.installed)
        }
    }

    @JvmStatic
    fun getExistsInAppStoreText(context: Context, packageManager: PackageManager, applicationInfo: ApplicationInfo): String {
        val exists = existsInAppStore(packageManager, applicationInfo)
        return when (exists) {
            null -> context.getString(R.string.unknown)
            true -> {
                val link = getAppStoreLink(applicationInfo)
                "<a href=\"$link\">${context.getString(R.string.boolean_true)}</a>"
            }
            false -> context.getString(R.string.boolean_false)
        }
    }

    @JvmStatic
    @Throws(PackageManager.NameNotFoundException::class)
    fun getFirstInstalled(packageManager: PackageManager, applicationInfo: ApplicationInfo): Date {
        return Date(getPackageInfo(packageManager, applicationInfo).firstInstallTime)
    }

    @JvmStatic
    @Throws(PackageManager.NameNotFoundException::class)
    fun getFirstInstalledText(packageManager: PackageManager, applicationInfo: ApplicationInfo): String {
        return dateFormat.format(getFirstInstalled(packageManager, applicationInfo))
    }

    @JvmStatic
    @Throws(PackageManager.NameNotFoundException::class)
    fun getLastUpdated(packageManager: PackageManager, applicationInfo: ApplicationInfo): Date {
        return Date(getPackageInfo(packageManager, applicationInfo).lastUpdateTime)
    }

    @JvmStatic
    @Throws(PackageManager.NameNotFoundException::class)
    fun getLastUpdatedText(packageManager: PackageManager, applicationInfo: ApplicationInfo): String {
        return dateFormat.format(getLastUpdated(packageManager, applicationInfo))
    }

    @JvmStatic
    fun getLastUsed(usageStatsManager: UsageStatsManager, applicationInfo: ApplicationInfo, reload: Boolean): Date {
        val lastUsedEpochs = getLastUsedEpochs(usageStatsManager, reload)
        val epoch = lastUsedEpochs[applicationInfo.packageName] ?: 0L
        return Date(epoch)
    }

    fun getLastUsedText(usageStatsManager: UsageStatsManager, applicationInfo: ApplicationInfo, reload: Boolean): String {
        val lastUsed = getLastUsed(usageStatsManager, applicationInfo, reload)
        val calendar = Calendar.getInstance().apply {
            add(Calendar.YEAR, -2)
        }
        return if (lastUsed.before(calendar.time)) {
            "Unknown"
        } else {
            dateFormat.format(lastUsed)
        }
    }

    private fun getLastUsedEpochs(usageStatsManager: UsageStatsManager, reload: Boolean): Map<String, Long> {
        if (lastUsedEpochsCache == null || reload) {
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.YEAR, -2)
            val startTime = calendar.timeInMillis
            lastUsedEpochsCache = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
                .mapValues { it.value.lastTimeUsed }
        }
        return lastUsedEpochsCache!!
    }

    @JvmStatic
    @Throws(PackageManager.NameNotFoundException::class)
    fun getVersionText(packageManager: PackageManager, applicationInfo: ApplicationInfo): String? {
        return getPackageInfo(packageManager, applicationInfo).versionName
    }

    @JvmStatic
    @Throws(PackageManager.NameNotFoundException::class)
    fun getPermissions(packageManager: PackageManager, applicationInfo: ApplicationInfo, grantedPermissionsOnly: Boolean): List<String> {
        val packageInfo = getPackageInfo(packageManager, applicationInfo)
        val requestedPermissions = packageInfo.requestedPermissions ?: return emptyList()
        val permissionFlags = packageInfo.requestedPermissionsFlags ?: return emptyList()

        return requestedPermissions.mapIndexedNotNull { i, permission ->
            if (permission.startsWith("android.permission")) {
                val granted = (permissionFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                if (grantedPermissionsOnly && !granted) {
                    null
                } else {
                    permission
                }
            } else {
                null
            }
        }
    }

    @JvmStatic
    fun getApkSize(applicationInfo: ApplicationInfo): Long {
        return File(applicationInfo.publicSourceDir).length()
    }

    @JvmStatic
    fun getStorageUsage(context: Context, applicationInfo: ApplicationInfo): StorageUsage {
        val storageUsage = StorageUsage()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val message = "Unable to calculate storage usage (requires API " + Build.VERSION_CODES.O + ")"
            Log.w(TAG, message)
            return storageUsage
        }

        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager

        storageManager.storageVolumes.forEach { storageVolume ->
            if (Environment.MEDIA_MOUNTED == storageVolume.state) {
                val uuidStr = storageVolume.uuid
                val uuid: UUID = try {
                    if (uuidStr == null) StorageManager.UUID_DEFAULT else UUID.fromString(uuidStr)
                } catch (_: IllegalArgumentException) {
                    val message = "Could not parse UUID $uuidStr for calculating storage usage. This is a known issue on some devices/storage volumes."
                    Log.w(TAG, message)
                    return@forEach
                }

                try {
                    val storageStats = storageStatsManager.queryStatsForPackage(uuid, applicationInfo.packageName, Process.myUserHandle())
                    storageUsage.increaseAppBytes(storageStats.appBytes)
                    storageUsage.increaseCacheBytes(storageStats.cacheBytes)
                    storageUsage.increaseDataBytes(storageStats.dataBytes)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        storageUsage.increaseExternalCacheBytes(storageStats.externalCacheBytes)
                    }
                } catch (e: SecurityException) {
                    val message = "Missing storage permission"
                    Log.e(TAG, message, e)
                    FirebaseCrashlytics.getInstance().log(message)
                    FirebaseCrashlytics.getInstance().recordException(e)
                } catch (e: Exception) {
                    val message = "Unable to process storage usage"
                    Log.e(TAG, message + " for ${applicationInfo.packageName} on $uuid", e)
                    FirebaseCrashlytics.getInstance().log(message)
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }

        return storageUsage
    }

    fun existsInAppStore(packageManager: PackageManager, applicationInfo: ApplicationInfo): Boolean? {
        val id = applicationInfo.packageName
        if (existsInAppStoreCache.containsKey(id)) {
            return existsInAppStoreCache[id]
        }

        if ("Google Play" != getPackageInstallerName(getPackageInstaller(packageManager, applicationInfo))) {
            return null
        }

        val url = getAppStoreLink(applicationInfo)
        val request = Request.Builder().url(url!!).build()
        Log.d(TAG, "Querying $url")

        return try {
            httpClient.newCall(request).execute().use { response ->
                val code = response.code
                val exists = code in 200..299
                if (!exists) {
                    Log.d(TAG, "HTTP response code for $url was $code")
                }
                existsInAppStoreCache[id] = exists
                exists
            }
        } catch (e: IOException) {
            val message = "Unable to make HTTP request to $url"
            Log.e(TAG, message, e)
            FirebaseCrashlytics.getInstance().log(message)
            FirebaseCrashlytics.getInstance().recordException(e)
            null
        }
    }

    fun getAppStoreLink(applicationInfo: ApplicationInfo): String? {
        return "https://play.google.com/store/apps/details?id=${applicationInfo.packageName}"
    }

    @Throws(PackageManager.NameNotFoundException::class)
    private fun getPackageInfo(packageManager: PackageManager, applicationInfo: ApplicationInfo): PackageInfo {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES
        }
        return packageManager.getPackageInfo(applicationInfo.packageName, flags)
    }
}
