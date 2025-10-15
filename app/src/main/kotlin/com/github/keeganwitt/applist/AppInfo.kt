package com.github.keeganwitt.applist

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.text.format.Formatter
import java.util.Date

class AppInfo(@JvmField val applicationInfo: ApplicationInfo, @JvmField val appInfoField: AppInfoField) {
    @JvmField
    var lastUsed: Date? = null

    @Throws(PackageManager.NameNotFoundException::class)
    fun getTextValue(
        context: Context,
        packageManager: PackageManager,
        usageStatsManager: UsageStatsManager
    ): String? {
        when (appInfoField) {
            AppInfoField.APK_SIZE -> {
                return ApplicationInfoUtils.getApkSizeText(context, applicationInfo)
            }
            AppInfoField.APP_SIZE -> {
                return Formatter.formatShortFileSize(
                    context,
                    ApplicationInfoUtils.getStorageUsage(context, applicationInfo).appBytes
                )
            }
            AppInfoField.CACHE_SIZE -> {
                return Formatter.formatShortFileSize(
                    context,
                    ApplicationInfoUtils.getStorageUsage(context, applicationInfo).cacheBytes
                )
            }
            AppInfoField.DATA_SIZE -> {
                return Formatter.formatShortFileSize(
                    context,
                    ApplicationInfoUtils.getStorageUsage(context, applicationInfo).dataBytes
                )
            }
            AppInfoField.ENABLED -> {
                return ApplicationInfoUtils.getEnabledText(context, applicationInfo)
            }
            AppInfoField.ARCHIVED -> {
                return ApplicationInfoUtils.getAppIsArchivedText(context, applicationInfo)
            }
            AppInfoField.EXISTS_IN_APP_STORE -> {
                return ApplicationInfoUtils.getExistsInAppStoreText(
                    context,
                    packageManager,
                    applicationInfo
                )
            }
            AppInfoField.EXTERNAL_CACHE_SIZE -> {
                return Formatter.formatShortFileSize(
                    context,
                    ApplicationInfoUtils.getStorageUsage(context, applicationInfo)
                        .externalCacheBytes
                )
            }
            AppInfoField.FIRST_INSTALLED -> {
                return ApplicationInfoUtils.getFirstInstalledText(packageManager, applicationInfo)
            }
            AppInfoField.GRANTED_PERMISSIONS -> {
                // commented out the list display, because it was too wordy, doing a count instead
    //            return String.join(", ", getPermissions(packageManager, applicationInfo, true)));
                return ApplicationInfoUtils.getPermissions(
                    packageManager,
                    applicationInfo,
                    true
                ).size.toString()
            }
            AppInfoField.LAST_UPDATED -> {
                return ApplicationInfoUtils.getLastUpdatedText(packageManager, applicationInfo)
            }
            AppInfoField.LAST_USED -> {
                return ApplicationInfoUtils.getLastUsedText(usageStatsManager, applicationInfo, false)
            }
            AppInfoField.MIN_SDK -> {
                return applicationInfo.minSdkVersion.toString()
            }
            AppInfoField.PACKAGE_MANAGER -> {
                return ApplicationInfoUtils.getPackageInstallerName(
                    ApplicationInfoUtils.getPackageInstaller(
                        packageManager,
                        applicationInfo
                    )
                )
            }
            AppInfoField.REQUESTED_PERMISSIONS -> {
                // commented out the list display, because it was too wordy, doing a count instead
    //            return String.join(", ", getPermissions(packageManager, applicationInfo, false));
                return ApplicationInfoUtils.getPermissions(
                    packageManager,
                    applicationInfo,
                    false
                ).size.toString()
            }
            AppInfoField.TARGET_SDK -> {
                return applicationInfo.targetSdkVersion.toString()
            }
            AppInfoField.TOTAL_SIZE -> {
                return Formatter.formatShortFileSize(
                    context,
                    ApplicationInfoUtils.getStorageUsage(context, applicationInfo).totalBytes
                )
            }
            AppInfoField.VERSION -> {
                return ApplicationInfoUtils.getVersionText(packageManager, applicationInfo)
            }
            else -> return ""
        }
    }
}
