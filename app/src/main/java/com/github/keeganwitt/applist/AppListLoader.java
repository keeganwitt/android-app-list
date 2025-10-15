package com.github.keeganwitt.applist;

import static com.github.keeganwitt.applist.ApplicationInfoUtils.getApkSize;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getFirstInstalled;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getLastUpdated;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getLastUsed;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPackageInstaller;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPackageInstallerName;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPermissions;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getStorageUsage;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getVersionText;
import static java.util.Comparator.comparing;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class AppListLoader {
    private static final String TAG = AppListLoader.class.getSimpleName();

    private final Context context;
    private final PackageManager packageManager;
    private final android.app.usage.UsageStatsManager usageStatsManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> future;

    public AppListLoader(Context context, PackageManager packageManager, android.app.usage.UsageStatsManager usageStatsManager) {
        this.context = context;
        this.packageManager = packageManager;
        this.usageStatsManager = usageStatsManager;
    }

    public void load(AppInfoField appInfoField, boolean showSystemApps, boolean descendingSortOrder, boolean reload, Callback callback) {
        if (future != null) {
            future.cancel(true);
        }
        future = executor.submit(() -> {
            List<ApplicationInfo> allInstalledApps;
            int flags = PackageManager.GET_META_DATA | PackageManager.MATCH_UNINSTALLED_PACKAGES;
            allInstalledApps = packageManager.getInstalledApplications(flags);
            List<AppInfo> appList;
            appList = filterUserOrSystemApps(allInstalledApps.stream()
                    .map(it -> new AppInfo(it, appInfoField))
                    .collect(Collectors.toList()), showSystemApps).stream()
                    .peek(it -> {
                        if (appInfoField.equals(AppInfoField.LAST_USED)) {
                            it.setLastUsed(getLastUsed(usageStatsManager, it.getApplicationInfo(), reload));
                        }
                    })
                    .sorted(determineComparator(packageManager, appInfoField))
                    .collect(Collectors.toList());
            if (descendingSortOrder) {
                Collections.reverse(appList);
            }
            callback.onLoaded(appList);
        });
    }

    private List<AppInfo> filterUserOrSystemApps(List<AppInfo> list, boolean showSystemApps) {
        ArrayList<AppInfo> appList = new ArrayList<>();
        for (AppInfo info : list) {
            try {
                boolean include = showSystemApps || isUserInstalledApp(info.getApplicationInfo());

                boolean isArchived = ApplicationInfoUtils.isAppArchived(info.getApplicationInfo());
                boolean hasLaunchIntent = packageManager.getLaunchIntentForPackage(info.getApplicationInfo().packageName) != null;

                if (include && (isArchived || hasLaunchIntent)) {
                    appList.add(info);
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to filter by user/system", e);
            }
        }

        return appList;
    }

    private boolean isUserInstalledApp(ApplicationInfo appInfo) {
        return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1;
    }

    private Comparator<AppInfo> determineComparator(PackageManager packageManager, AppInfoField appInfoField) {
        Collator collator = Collator.getInstance();
        Comparator<AppInfo> comparator = comparing(ai -> String.valueOf(ai.getApplicationInfo().loadLabel(packageManager)));
        if (appInfoField.equals(AppInfoField.APK_SIZE)) {
            comparator = comparing(ai -> getApkSize(ai.getApplicationInfo()));
        } else if (appInfoField.equals(AppInfoField.APP_SIZE)) {
            comparator = comparing(ai -> getStorageUsage(context, ai.getApplicationInfo()).getAppBytes());
        } else if (appInfoField.equals(AppInfoField.CACHE_SIZE)) {
            comparator = comparing(ai -> getStorageUsage(context, ai.getApplicationInfo()).getCacheBytes());
        } else if (appInfoField.equals(AppInfoField.DATA_SIZE)) {
            comparator = comparing(ai -> getStorageUsage(context, ai.getApplicationInfo()).getDataBytes());
        } else if (appInfoField.equals(AppInfoField.ENABLED)) {
            comparator = comparing(ai -> ApplicationInfoUtils.getEnabledText(context, ai.getApplicationInfo()));
        } else if (appInfoField.equals(AppInfoField.ARCHIVED)) {
            comparator = comparing(ai -> ApplicationInfoUtils.getAppIsArchivedText(context, ai.getApplicationInfo()));
        } else if (appInfoField.equals(AppInfoField.EXISTS_IN_APP_STORE)) {
            comparator = comparing(ai -> ApplicationInfoUtils.getExistsInAppStoreText(context, packageManager, ai.getApplicationInfo()));
        } else if (appInfoField.equals(AppInfoField.EXTERNAL_CACHE_SIZE)) {
            comparator = comparing(ai -> getStorageUsage(context, ai.getApplicationInfo()).getExternalCacheBytes());
        } else if (appInfoField.equals(AppInfoField.FIRST_INSTALLED)) {
            comparator = comparing(ai -> {
                try {
                    return getFirstInstalled(packageManager, ai.getApplicationInfo()).getTime();
                } catch (PackageManager.NameNotFoundException e) {
                    return 0L;
                }
            });
        } else if (appInfoField.equals(AppInfoField.LAST_UPDATED)) {
            comparator = comparing(ai -> {
                try {
                    return getLastUpdated(packageManager, ai.getApplicationInfo()).getTime();
                } catch (PackageManager.NameNotFoundException e) {
                    return 0L;
                }
            });
        } else if (appInfoField.equals(AppInfoField.LAST_USED)) {
            comparator = comparing(ai -> getLastUsed(usageStatsManager, ai.getApplicationInfo(), false).getTime());
        } else if (appInfoField.equals(AppInfoField.MIN_SDK)) {
            comparator = comparing(ai -> ai.getApplicationInfo().minSdkVersion);
        } else if (appInfoField.equals(AppInfoField.PACKAGE_MANAGER)) {
            comparator = comparing(ai -> getPackageInstallerName(getPackageInstaller(packageManager, ai.getApplicationInfo())));
        } else if (appInfoField.equals(AppInfoField.GRANTED_PERMISSIONS) || appInfoField.equals(AppInfoField.REQUESTED_PERMISSIONS)) {
            comparator = comparing(ai -> {
                try {
                    return getPermissions(packageManager, ai.getApplicationInfo(), appInfoField.equals(AppInfoField.GRANTED_PERMISSIONS)).size();
                } catch (NullPointerException | PackageManager.NameNotFoundException e) {
                    return 0;
                }
            });
        } else if (appInfoField.equals(AppInfoField.TARGET_SDK)) {
            comparator = comparing(ai -> ai.getApplicationInfo().targetSdkVersion);
        } else if (appInfoField.equals(AppInfoField.TOTAL_SIZE)) {
            comparator = comparing(ai -> getStorageUsage(context, ai.getApplicationInfo()).getTotalBytes());
        } else if (appInfoField.equals(AppInfoField.VERSION)) {
            comparator = comparing(ai -> {
                try {
                    return getVersionText(packageManager, ai.getApplicationInfo());
                } catch (PackageManager.NameNotFoundException e) {
                    return "";
                }
            });
        }
        comparator = comparator.thenComparing(ai -> String.valueOf(ai.getApplicationInfo().loadLabel(packageManager)), collator);
        return comparator;
    }

    public interface Callback {
        void onLoaded(List<AppInfo> appList);
    }
}
