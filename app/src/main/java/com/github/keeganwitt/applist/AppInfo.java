package com.github.keeganwitt.applist;

import static com.github.keeganwitt.applist.ApplicationInfoUtils.getApkSizeText;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getEnabledText;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getExistsInAppStoreText;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getFirstInstalledText;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getLastUpdatedText;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getLastUsedText;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPackageInstaller;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPackageInstallerName;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPermissions;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getStorageUsage;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getVersionText;

import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.format.Formatter;

import java.util.Date;

public class AppInfo {
    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    private ApplicationInfo applicationInfo;
    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    private AppInfoField appInfoField;
    private Date lastUsed;

    public AppInfo(ApplicationInfo applicationInfo, AppInfoField appInfoField) {
        this.applicationInfo = applicationInfo;
        this.appInfoField = appInfoField;
    }

    public ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }

    public AppInfoField getAppInfoField() {
        return appInfoField;
    }

    @SuppressWarnings("unused")
    public Date getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }

    public String getTextValue(Context context, PackageManager packageManager, UsageStatsManager usageStatsManager) throws PackageManager.NameNotFoundException {
        if (appInfoField.equals(AppInfoField.APP_NAME)) {
            return applicationInfo.packageName;
        } else if (appInfoField.equals(AppInfoField.APK_SIZE)) {
            return getApkSizeText(context, applicationInfo);
        } else if (appInfoField.equals(AppInfoField.APP_SIZE)) {
            return Formatter.formatShortFileSize(context, getStorageUsage(context, applicationInfo).getAppBytes());
        } else if (appInfoField.equals(AppInfoField.CACHE_SIZE)) {
            return Formatter.formatShortFileSize(context, getStorageUsage(context, applicationInfo).getCacheBytes());
        } else if (appInfoField.equals(AppInfoField.DATA_SIZE)) {
            return Formatter.formatShortFileSize(context, getStorageUsage(context, applicationInfo).getDataBytes());
        } else if (appInfoField.equals(AppInfoField.ENABLED)) {
            return getEnabledText(context, applicationInfo);
        } else if (appInfoField.equals(AppInfoField.EXISTS_IN_APP_STORE)) {
            return getExistsInAppStoreText(context, packageManager, applicationInfo);
        } else if (appInfoField.equals(AppInfoField.EXTERNAL_CACHE_SIZE)) {
            return Formatter.formatShortFileSize(context, getStorageUsage(context, applicationInfo).getExternalCacheBytes());
        } else if (appInfoField.equals(AppInfoField.FIRST_INSTALLED)) {
            return getFirstInstalledText(packageManager, applicationInfo);
        } else if (appInfoField.equals(AppInfoField.GRANTED_PERMISSIONS)) {
            // commented out the list display, because it was too wordy, doing a count instead
//            return String.join(", ", getPermissions(packageManager, applicationInfo, true)));
            return String.valueOf(getPermissions(packageManager, applicationInfo, true).size());
        } else if (appInfoField.equals(AppInfoField.LAST_UPDATED)) {
            return getLastUpdatedText(packageManager, applicationInfo);
        } else if (appInfoField.equals(AppInfoField.LAST_USED)) {
            return getLastUsedText(usageStatsManager, applicationInfo, false);
        } else if (appInfoField.equals(AppInfoField.MIN_SDK)) {
            return String.valueOf(applicationInfo.minSdkVersion);
        } else if (appInfoField.equals(AppInfoField.PACKAGE_MANAGER)) {
            return getPackageInstallerName(getPackageInstaller(packageManager, applicationInfo));
        } else if (appInfoField.equals(AppInfoField.REQUESTED_PERMISSIONS)) {
            // commented out the list display, because it was too wordy, doing a count instead
//            return String.join(", ", getPermissions(packageManager, applicationInfo, false));
            return String.valueOf(getPermissions(packageManager, applicationInfo, false).size());
        } else if (appInfoField.equals(AppInfoField.TARGET_SDK)) {
            return String.valueOf(applicationInfo.targetSdkVersion);
        } else if (appInfoField.equals(AppInfoField.TOTAL_SIZE)) {
            return Formatter.formatShortFileSize(context, getStorageUsage(context, applicationInfo).getTotalBytes());
        } else if (appInfoField.equals(AppInfoField.VERSION)) {
            return getVersionText(packageManager, applicationInfo);
        }
        return "";
    }
}
