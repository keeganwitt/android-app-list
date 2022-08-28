package com.github.keeganwitt.applist;

import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.icu.text.DateFormat;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.format.Formatter;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static android.os.Process.myUserHandle;

public class ApplicationInfoUtils {
    private static final String TAG = ApplicationInfoUtils.class.getSimpleName();

    private static Map<String, Long> lastUsedEpochsCache;

    private ApplicationInfoUtils() {}

    @SuppressWarnings("deprecation")
    public static String getPackageInstaller(PackageManager packageManager, ApplicationInfo applicationInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                return packageManager.getInstallSourceInfo(applicationInfo.packageName).getInstallingPackageName();
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Unable to determine package installer for " + applicationInfo.packageName, e);
                return null;
            }
        } else {
            return packageManager.getInstallerPackageName(applicationInfo.packageName);
        }
    }

    public static String getPackageInstallerName(String installerPackageName) {
        if ("com.amazon.venezia".equals(installerPackageName)) {
            return "Amazon Appstore";
        } else if ("cm.aptoide.pt".equals(installerPackageName)) {
            return "Aptoide";
        } else if ("net.rim.bb.appworld".equals(installerPackageName)) {
            return "Blackberry World";
        } else if ("com.farsitel.bazaar".equals(installerPackageName)) {
            return "Cafe Bazaar";
        } else if ("com.sec.android.app.samsungapps".equals(installerPackageName)) {
            return "Galaxy Store";
        } else if ("com.android.vending".equals(installerPackageName)) {
            return "Google Play";
        } else if ("com.huawei.appmarket".equals(installerPackageName)) {
            return "Huawei App Galary";
        } else if ("com.xiaomi.market".equals(installerPackageName)) {
            return "Mi Store";
        } else if ("com.oneplus.backuprestore".equals(installerPackageName)) {
            return "OnePlus Clone Phone";
        } else if ("com.sec.android.easyMover".equals(installerPackageName)) {
            return "Samsung Smart Switch";
        } else if ("com.slideme.sam.manager".equals(installerPackageName)) {
            return "SlideME Marketplace";
        } else if ("com.tencent.android.qqdownloader".equals(installerPackageName)) {
            return "TenCent Appstore";
        } else if ("com.yandex.store".equals(installerPackageName)) {
            return "Yandex Appstore";
        } else if (installerPackageName != null) {
            return "Unknown (" + installerPackageName + ")";
        } else {
            return "Unknown";
        }
    }

    public static String getEnabledText(ApplicationInfo applicationInfo) {
        return applicationInfo.enabled ? "Enabled" : "Disabled";
    }

    public static Date getFirstInstalled(PackageManager packageManager, ApplicationInfo applicationInfo) throws PackageManager.NameNotFoundException {
        return new Date(getPackageInfo(packageManager, applicationInfo).firstInstallTime);
    }

    public static String getFirstInstalledText(PackageManager packageManager, ApplicationInfo applicationInfo) throws PackageManager.NameNotFoundException {
        return getDateFormat().format(getFirstInstalled(packageManager, applicationInfo));
    }

    public static Date getLastUpdated(PackageManager packageManager, ApplicationInfo applicationInfo) throws PackageManager.NameNotFoundException {
        return new Date(getPackageInfo(packageManager, applicationInfo).lastUpdateTime);
    }

    public static String getLastUpdatedText(PackageManager packageManager, ApplicationInfo applicationInfo) throws PackageManager.NameNotFoundException {
        return getDateFormat().format(getLastUpdated(packageManager, applicationInfo));
    }

    public static Date getLastUsed(UsageStatsManager usageStatsManager, ApplicationInfo applicationInfo, boolean reload) {
        Map<String, Long> lastUsedEpochs = getLastUsedEpochs(usageStatsManager, reload);
        Long epoch = lastUsedEpochs.getOrDefault(applicationInfo.packageName, 0L);
        return new Date(epoch == null ? 0L : epoch);
    }

    public static String getLastUsedText(UsageStatsManager usageStatsManager, ApplicationInfo applicationInfo, boolean reload) {
        Date lastUsed = getLastUsed(usageStatsManager, applicationInfo, reload);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -2);
        if (lastUsed.before(calendar.getTime())) {
            return "Unknown";
        }
        return getDateFormat().format(lastUsed);
    }

    private static Map<String, Long> getLastUsedEpochs(UsageStatsManager usageStatsManager, boolean reload) {
        if (lastUsedEpochsCache == null || reload) {
            Calendar calendar = Calendar.getInstance();
            long endTime = calendar.getTimeInMillis();
            calendar.add(Calendar.YEAR, -2);
            long startTime = calendar.getTimeInMillis();
            lastUsedEpochsCache = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime).entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getLastTimeUsed()));
        }
        return lastUsedEpochsCache;
    }

    public static String getVersionText(PackageManager packageManager, ApplicationInfo applicationInfo) throws PackageManager.NameNotFoundException {
        return getPackageInfo(packageManager, applicationInfo).versionName;
    }

    public static List<String> getPermissions(PackageManager packageManager, ApplicationInfo applicationInfo, boolean grantedPermissionsOnly) throws PackageManager.NameNotFoundException {
        List<String> permissions = new ArrayList<>();
        PackageInfo packageInfo = getPackageInfo(packageManager, applicationInfo);
        String[] requestedPermissions = packageInfo.requestedPermissions;
        if (requestedPermissions == null) {
            return permissions;
        }
        for (int i = 0; i < requestedPermissions.length; i++) {
            String requestedPermission = requestedPermissions[i];
            if (!requestedPermission.startsWith("android.permission")) {
                continue;
            }
            boolean granted = (packageInfo.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0;
            if (grantedPermissionsOnly && !granted) {
                continue;
            }
            permissions.add(requestedPermission);
        }
        return permissions;
    }

    public static long getApkSize(ApplicationInfo applicationInfo) {
        return new File(applicationInfo.publicSourceDir).length();
    }

    public static String getApkSizeText(Context context, ApplicationInfo applicationInfo) {
        return Formatter.formatShortFileSize(context, getApkSize(applicationInfo));
    }

    public static StorageUsage getStorageUsage(Context context, ApplicationInfo applicationInfo) {
        StorageUsage storageUsage = new StorageUsage();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.w(TAG, "Unable to calculate storage usage (requires API " + Build.VERSION_CODES.O + ")");
            return storageUsage;
        }
        final StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        final StorageStatsManager storageStatsManager = (StorageStatsManager) context.getSystemService(Context.STORAGE_STATS_SERVICE);
        for (StorageVolume storageVolume : storageManager.getStorageVolumes()) {
            if (Environment.MEDIA_MOUNTED.equals(storageVolume.getState())) {
                String uuidStr = storageVolume.getUuid();
                UUID uuid;
                try {
                    uuid = uuidStr == null ? StorageManager.UUID_DEFAULT : UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "Could not parse UUID " + uuidStr + " for calculating storage usage");
                    continue;
                }
                try {
                    StorageStats storageStats = storageStatsManager.queryStatsForPackage(uuid, applicationInfo.packageName, myUserHandle());
                    storageUsage.increaseDataBytes(storageStats.getAppBytes());
                    storageUsage.increaseDataBytes(storageStats.getCacheBytes());
                    storageUsage.increaseDataBytes(storageStats.getDataBytes());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        storageUsage.increaseExternalCacheBytes(storageStats.getExternalCacheBytes());
                    }
                } catch (PackageManager.NameNotFoundException | IOException e) {
                    Log.e(TAG, "Unable to process storage usage for " + applicationInfo.packageName + " on " + uuid, e);
                }
            }
        }

        return storageUsage;
    }

    private static PackageInfo getPackageInfo(PackageManager packageManager, ApplicationInfo applicationInfo) throws PackageManager.NameNotFoundException {
        return packageManager.getPackageInfo(applicationInfo.packageName, PackageManager.GET_PERMISSIONS);
    }

    private static DateFormat getDateFormat() {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
    }

    public static class StorageUsage {
        private long appBytes;
        private long cacheBytes;
        private long dataBytes;
        private long externalCacheBytes;
        private long totalBytes;

        public StorageUsage() {}

        public StorageUsage(long appBytes, long cacheBytes, long dataBytes, long externalCacheBytes) {
            this.appBytes = appBytes;
            this.cacheBytes = cacheBytes;
            this.dataBytes = dataBytes;
            this.externalCacheBytes = externalCacheBytes;
            recalculateTotal();
        }

        public long getAppBytes() {
            return appBytes;
        }

        public void setAppBytes(long appBytes) {
            this.appBytes = appBytes;
            recalculateTotal();
        }

        public void incrementAppBytes(long appBytes) {
            this.appBytes += appBytes;
            recalculateTotal();
        }

        public long getCacheBytes() {
            return cacheBytes;
        }

        public void setCacheBytes(long cacheBytes) {
            this.cacheBytes = cacheBytes;
            recalculateTotal();
        }

        public void incrementCacheBytes(long cacheBytes) {
            this.cacheBytes += cacheBytes;
            recalculateTotal();
        }

        public long getDataBytes() {
            return dataBytes;
        }

        public void setDataBytes(long dataBytes) {
            this.dataBytes = dataBytes;
            recalculateTotal();
        }

        public void increaseDataBytes(long dataBytes) {
            this.dataBytes += dataBytes;
            recalculateTotal();
        }

        public long getExternalCacheBytes() {
            return externalCacheBytes;
        }

        public void setExternalCacheBytes(long externalCacheBytes) {
            this.externalCacheBytes = externalCacheBytes;
            recalculateTotal();
        }

        public void increaseExternalCacheBytes(long externalCacheBytes) {
            this.externalCacheBytes += externalCacheBytes;
            recalculateTotal();
        }

        public long getTotalBytes() {
            return totalBytes;
        }

        private void recalculateTotal() {
            totalBytes = appBytes + cacheBytes + dataBytes + externalCacheBytes;
        }
    }
}
