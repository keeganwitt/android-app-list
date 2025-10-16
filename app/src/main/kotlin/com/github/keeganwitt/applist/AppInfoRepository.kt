package com.github.keeganwitt.applist

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.github.keeganwitt.applist.ApplicationInfoUtils.isAppArchived
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
            // Filter out apps that don't meet the criteria
            try {
                val include = showSystemApps || isUserInstalledApp(appInfo)
                val isArchived = isAppArchived(appInfo) ?: false
                val hasLaunchIntent =
                    packageManager.getLaunchIntentForPackage(appInfo.packageName) != null

                if (include && (isArchived || hasLaunchIntent)) {
                    AppInfo(appInfo, appInfoField)
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to filter by user/system", e)
                null
            }
        }.onEach { appInfo ->
            // Populate last used time if needed
            if (appInfoField == AppInfoField.LAST_USED) {
                appInfo.lastUsed = ApplicationInfoUtils.getLastUsed(usageStatsManager, appInfo.applicationInfo, reload)
            }
        }

        val sortedList = if (descendingSortOrder) {
            appList.sortedWith(determineComparator(appInfoField).reversed())
        } else {
            appList.sortedWith(determineComparator(appInfoField))
        }

        return sortedList
    }

    private fun isUserInstalledApp(appInfo: ApplicationInfo): Boolean {
        // A user-installed app has the system flag bit UNSET
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
    }

    private fun determineComparator(appInfoField: AppInfoField): Comparator<AppInfo> {
        val collator = Collator.getInstance()
        val nameComparator = Comparator<AppInfo> { a, b ->
            val aName = a.applicationInfo.loadLabel(packageManager).toString()
            val bName = b.applicationInfo.loadLabel(packageManager).toString()
            collator.compare(aName, bName)
        }

        val primaryComparator: Comparator<AppInfo> = when (appInfoField) {
            AppInfoField.APK_SIZE -> Comparator.comparingLong { ApplicationInfoUtils.getApkSize(it.applicationInfo) }
            AppInfoField.APP_SIZE -> Comparator.comparingLong { ApplicationInfoUtils.getStorageUsage(context, it.applicationInfo).appBytes }
            AppInfoField.CACHE_SIZE -> Comparator.comparingLong { ApplicationInfoUtils.getStorageUsage(context, it.applicationInfo).cacheBytes }
            AppInfoField.DATA_SIZE -> Comparator.comparingLong { ApplicationInfoUtils.getStorageUsage(context, it.applicationInfo).dataBytes }
            AppInfoField.ENABLED -> Comparator.comparing { ApplicationInfoUtils.getEnabledText(context, it.applicationInfo) }
            AppInfoField.ARCHIVED -> Comparator.comparing { ApplicationInfoUtils.getAppIsArchivedText(context, it.applicationInfo) }
            AppInfoField.EXISTS_IN_APP_STORE -> Comparator.comparing { ApplicationInfoUtils.getExistsInAppStoreText(context, packageManager, it.applicationInfo) }
            AppInfoField.EXTERNAL_CACHE_SIZE -> Comparator.comparingLong { ApplicationInfoUtils.getStorageUsage(context, it.applicationInfo).externalCacheBytes }
            AppInfoField.FIRST_INSTALLED -> Comparator.comparingLong { try { ApplicationInfoUtils.getFirstInstalled(packageManager, it.applicationInfo).time } catch (_: PackageManager.NameNotFoundException) { 0L } }
            AppInfoField.LAST_UPDATED -> Comparator.comparingLong { try { ApplicationInfoUtils.getLastUpdated(packageManager, it.applicationInfo).time } catch (_: PackageManager.NameNotFoundException) { 0L } }
            AppInfoField.LAST_USED -> Comparator.comparingLong { ApplicationInfoUtils.getLastUsed(usageStatsManager, it.applicationInfo, false).time }
            AppInfoField.MIN_SDK -> Comparator.comparingInt { it.applicationInfo.minSdkVersion }
            AppInfoField.PACKAGE_MANAGER -> Comparator.comparing { ApplicationInfoUtils.getPackageInstallerName(ApplicationInfoUtils.getPackageInstaller(packageManager, it.applicationInfo)) }
            AppInfoField.GRANTED_PERMISSIONS, AppInfoField.REQUESTED_PERMISSIONS -> Comparator.comparingInt { try { ApplicationInfoUtils.getPermissions(packageManager, it.applicationInfo, appInfoField == AppInfoField.GRANTED_PERMISSIONS).size } catch (_: Exception) { 0 } }
            AppInfoField.TARGET_SDK -> Comparator.comparingInt { it.applicationInfo.targetSdkVersion }
            AppInfoField.TOTAL_SIZE -> Comparator.comparingLong { ApplicationInfoUtils.getStorageUsage(context, it.applicationInfo).totalBytes }
            AppInfoField.VERSION -> Comparator.comparing { (try { ApplicationInfoUtils.getVersionText(packageManager, it.applicationInfo) } catch (_: PackageManager.NameNotFoundException) { "" }) ?: "" }
        }

        return primaryComparator.thenComparing(nameComparator)
    }

    companion object {
        private val TAG: String = AppInfoRepository::class.java.simpleName
    }
}
