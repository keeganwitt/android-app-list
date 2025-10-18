package com.github.keeganwitt.applist

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.github.keeganwitt.applist.ApplicationInfoUtils.isAppArchived
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.text.Collator
import java.util.Comparator

class AppInfoRepository(context: Context) {
    private val context: Context = context.applicationContext
    private val packageManager: PackageManager = context.packageManager
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager

    fun getAppList(
        appInfoField: AppInfoField,
        showSystemApps: Boolean,
        descendingSortOrder: Boolean,
        reload: Boolean
    ): List<AppInfo> {
        val flags = PackageManager.GET_META_DATA or PackageManager.MATCH_UNINSTALLED_PACKAGES
        val allInstalledApps = packageManager.getInstalledApplications(flags)

        val appList = allInstalledApps.mapNotNull { appInfo ->
            try {
                val include = showSystemApps || isUserInstalledApp(appInfo)
                val isArchived = isAppArchived(appInfo) ?: false
                val hasLaunchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName) != null

                if (include && (isArchived || hasLaunchIntent)) {
                    AppInfo(appInfo, appInfoField)
                } else {
                    null
                }
            } catch (e: Exception) {
                val message = "Unable to filter by user/system"
                Log.e(TAG, message, e)
                FirebaseCrashlytics.getInstance().log(message)
                FirebaseCrashlytics.getInstance().recordException(e)
                null
            }
        }

        val appListWithSortKey = appList.mapNotNull { appInfo ->
            try {
                val key = getSortKey(appInfo, appInfoField, reload)
                Pair(appInfo, key)
            } catch (_: PackageManager.NameNotFoundException) {
                Log.w(TAG, "App ${appInfo.applicationInfo.packageName} uninstalled during processing, removing from list.")
                null
            }
        }

        val collator = Collator.getInstance()
        val nameComparator = compareBy(collator) { pair: Pair<AppInfo, Comparable<*>?> ->
            pair.first.applicationInfo.loadLabel(packageManager).toString()
        }

        val primaryComparator = compareBy<Pair<AppInfo, Comparable<*>?>> { it.second }

        var finalComparator = primaryComparator.then(nameComparator)
        if (descendingSortOrder) {
            finalComparator = finalComparator.reversed()
        }

        return appListWithSortKey.sortedWith(finalComparator).map { it.first }
    }

    private fun isUserInstalledApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
    }

    @Throws(PackageManager.NameNotFoundException::class)
    private fun getSortKey(appInfo: AppInfo, appInfoField: AppInfoField, reload: Boolean): Comparable<*>? {
        return when (appInfoField) {
            AppInfoField.APK_SIZE -> ApplicationInfoUtils.getApkSize(appInfo.applicationInfo)
            AppInfoField.APP_SIZE -> ApplicationInfoUtils.getStorageUsage(context, appInfo.applicationInfo).appBytes
            AppInfoField.CACHE_SIZE -> ApplicationInfoUtils.getStorageUsage(context, appInfo.applicationInfo).cacheBytes
            AppInfoField.DATA_SIZE -> ApplicationInfoUtils.getStorageUsage(context, appInfo.applicationInfo).dataBytes
            AppInfoField.ENABLED -> ApplicationInfoUtils.getEnabledText(context, appInfo.applicationInfo)
            AppInfoField.ARCHIVED -> ApplicationInfoUtils.getAppIsArchivedText(context, appInfo.applicationInfo)
            AppInfoField.EXISTS_IN_APP_STORE -> ApplicationInfoUtils.getExistsInAppStoreText(context, packageManager, appInfo.applicationInfo)
            AppInfoField.EXTERNAL_CACHE_SIZE -> ApplicationInfoUtils.getStorageUsage(context, appInfo.applicationInfo).externalCacheBytes
            AppInfoField.FIRST_INSTALLED -> ApplicationInfoUtils.getFirstInstalled(packageManager, appInfo.applicationInfo).time
            AppInfoField.LAST_UPDATED -> ApplicationInfoUtils.getLastUpdated(packageManager, appInfo.applicationInfo).time
            AppInfoField.LAST_USED -> ApplicationInfoUtils.getLastUsed(usageStatsManager, appInfo.applicationInfo, reload).time
            AppInfoField.MIN_SDK -> appInfo.applicationInfo.minSdkVersion
            AppInfoField.PACKAGE_MANAGER -> ApplicationInfoUtils.getPackageInstallerName(ApplicationInfoUtils.getPackageInstaller(packageManager, appInfo.applicationInfo))
            AppInfoField.GRANTED_PERMISSIONS, AppInfoField.REQUESTED_PERMISSIONS -> ApplicationInfoUtils.getPermissions(packageManager, appInfo.applicationInfo, appInfoField == AppInfoField.GRANTED_PERMISSIONS).size
            AppInfoField.TARGET_SDK -> appInfo.applicationInfo.targetSdkVersion
            AppInfoField.TOTAL_SIZE -> ApplicationInfoUtils.getStorageUsage(context, appInfo.applicationInfo).totalBytes
            AppInfoField.VERSION -> ApplicationInfoUtils.getVersionText(packageManager, appInfo.applicationInfo)
        }
    }

    companion object {
        private val TAG: String = AppInfoRepository::class.java.simpleName
    }
}
