package com.github.keeganwitt.applist

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.text.Html
import android.text.Spanned
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
    ): Spanned? {
        val text = when (appInfoField) {
            AppInfoField.APK_SIZE -> {
                ApplicationInfoUtils.getApkSizeText(context, applicationInfo)
            }
            AppInfoField.APP_SIZE -> {
                Formatter.formatShortFileSize(
                    context,
                    ApplicationInfoUtils.getStorageUsage(context, applicationInfo).appBytes
                )
            }
            AppInfoField.CACHE_SIZE -> {
                Formatter.formatShortFileSize(
                    context,
                    ApplicationInfoUtils.getStorageUsage(context, applicationInfo).cacheBytes
                )
            }
            AppInfoField.DATA_SIZE -> {
                Formatter.formatShortFileSize(
                    context,
                    ApplicationInfoUtils.getStorageUsage(context, applicationInfo).dataBytes
                )
            }
            AppInfoField.ENABLED -> {
                ApplicationInfoUtils.getEnabledText(context, applicationInfo)
            }
            AppInfoField.ARCHIVED -> {
                ApplicationInfoUtils.getAppIsArchivedText(context, applicationInfo)
            }
            AppInfoField.EXISTS_IN_APP_STORE -> {
                ApplicationInfoUtils.getExistsInAppStoreText(
                    context,
                    packageManager,
                    applicationInfo
                )
            }
            AppInfoField.EXTERNAL_CACHE_SIZE -> {
                Formatter.formatShortFileSize(
                    context,
                    ApplicationInfoUtils.getStorageUsage(context, applicationInfo)
                        .externalCacheBytes
                )
            }
            AppInfoField.FIRST_INSTALLED -> {
                ApplicationInfoUtils.getFirstInstalledText(packageManager, applicationInfo)
            }
            AppInfoField.GRANTED_PERMISSIONS -> {
                // commented out the list display, because it was too wordy, doing a count instead
    //            String.join(", ", getPermissions(packageManager, applicationInfo, true)));
                ApplicationInfoUtils.getPermissions(
                    packageManager,
                    applicationInfo,
                    true
                ).size.toString()
            }
            AppInfoField.LAST_UPDATED -> {
                ApplicationInfoUtils.getLastUpdatedText(packageManager, applicationInfo)
            }
            AppInfoField.LAST_USED -> {
                ApplicationInfoUtils.getLastUsedText(usageStatsManager, applicationInfo, false)
            }
            AppInfoField.MIN_SDK -> {
                applicationInfo.minSdkVersion.toString()
            }
            AppInfoField.PACKAGE_MANAGER -> {
                ApplicationInfoUtils.getPackageInstallerName(
                    ApplicationInfoUtils.getPackageInstaller(
                        packageManager,
                        applicationInfo
                    )
                )
            }
            AppInfoField.REQUESTED_PERMISSIONS -> {
                // commented out the list display, because it was too wordy, doing a count instead
    //            String.join(", ", getPermissions(packageManager, applicationInfo, false));
                ApplicationInfoUtils.getPermissions(
                    packageManager,
                    applicationInfo,
                    false
                ).size.toString()
            }
            AppInfoField.TARGET_SDK -> {
                applicationInfo.targetSdkVersion.toString()
            }
            AppInfoField.TOTAL_SIZE -> {
                Formatter.formatShortFileSize(
                    context,
                    ApplicationInfoUtils.getStorageUsage(context, applicationInfo).totalBytes
                )
            }
            AppInfoField.VERSION -> {
                ApplicationInfoUtils.getVersionText(packageManager, applicationInfo)
            }
        }
        return Html.fromHtml(text ?: "", Html.FROM_HTML_MODE_COMPACT)
    }
}
