package com.github.keeganwitt.applist;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.github.keeganwitt.applist.ApplicationInfoUtils.getApkSize;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getFirstInstalled;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getLastUpdated;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPackageInstaller;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPackageInstallerName;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPermissions;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getStorageUsage;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getVersionText;
import static java.util.Comparator.comparing;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, AppInfoAdapter.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private PackageManager packageManager;
    private final ExecutorService appListLoader = Executors.newSingleThreadExecutor();
    private List<AppInfoField> appInfoFields;
    private AppInfoField selectedAppInfoField;
    private AppInfoAdapter appInfoAdapter;
    private Spinner spinner;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        appInfoFields = Arrays.asList(AppInfoField.values());
        packageManager = getPackageManager();

        spinner = findViewById(R.id.spinner);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                appInfoFields.stream().map(AppInfoField::getDisplayName).toArray(String[]::new));
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(this);

        progressBar = findViewById(R.id.progress_bar);

        recyclerView = findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(layoutManager);
        appInfoAdapter = new AppInfoAdapter(MainActivity.this, this);
        recyclerView.setAdapter(appInfoAdapter);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadApplications(selectedAppInfoField, true);
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(int position) {
        AppInfo app = appInfoAdapter.getCurrentList().get(position);
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + app.getApplicationInfo().packageName));
        startActivity(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        loadSelection(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        loadSelection(0);
    }

    private void loadSelection(int position) {
        selectedAppInfoField = appInfoFields.get(position);
        if ((selectedAppInfoField.equals(AppInfoField.CACHE_SIZE)
                || selectedAppInfoField.equals(AppInfoField.DATA_SIZE)
                || selectedAppInfoField.equals(AppInfoField.EXTERNAL_CACHE_SIZE)
                || selectedAppInfoField.equals(AppInfoField.TOTAL_SIZE))
                && !hasUsageStatsPermission()) {
            requestUsageStatsPermission();
        }
        loadApplications(selectedAppInfoField, false);
    }

    private List<AppInfo> filterNonUserInstalledAppInfo(List<AppInfo> list, AppInfoField appInfoField) {
        return filterNonUserInstalledApplicationInfo(list.stream().map(AppInfo::getApplicationInfo).collect(Collectors.toList()), appInfoField);
    }

    private List<AppInfo> filterNonUserInstalledApplicationInfo(List<ApplicationInfo> list, AppInfoField appInfoField) {
        ArrayList<AppInfo> appList = new ArrayList<>();
        for (ApplicationInfo info : list) {
            try {
                // TODO: let user choose between system and user apps
                if (packageManager.getLaunchIntentForPackage(info.packageName) != null && isUserInstalledApp(info)) {
                    appList.add(new AppInfo(info, appInfoField));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return appList;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadApplications(AppInfoField appInfoField, boolean reload) {
        MainActivity.this.runOnUiThread(() -> {
            recyclerView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        });
        appListLoader.submit(() -> {
            List<AppInfo> appList;
            if (appInfoAdapter.getCurrentList().isEmpty() || reload) {
                appList = MainActivity.this.filterNonUserInstalledApplicationInfo(packageManager.getInstalledApplications(PackageManager.GET_META_DATA), appInfoField).stream()
                        .sorted(determineComparator(packageManager, appInfoField))
                        .collect(Collectors.toList());
            } else {
                appList = MainActivity.this.filterNonUserInstalledAppInfo(appInfoAdapter.getCurrentList(), appInfoField).stream()
                        .sorted(determineComparator(packageManager, appInfoField))
                        .collect(Collectors.toList());
            }
            MainActivity.this.runOnUiThread(() -> {
                appInfoAdapter.submitList(appList);
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            });
        });
    }

    private boolean isUserInstalledApp(ApplicationInfo appInfo) {
        return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1;
    }

    @SuppressWarnings("deprecation")
    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) MainActivity.this.getSystemService(Context.APP_OPS_SERVICE);
        int mode;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), MainActivity.this.getPackageName());
        } else {
            mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), MainActivity.this.getPackageName());
        }
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private Comparator<AppInfo> determineComparator(PackageManager packageManager, AppInfoField appInfoField) {
        Comparator<AppInfo> comparator = comparing(ai -> String.valueOf(ai.getApplicationInfo().loadLabel(packageManager)));
        if (appInfoField.equals(AppInfoField.APK_SIZE)) {
            comparator = comparing(ai -> -getApkSize(ai.getApplicationInfo()));
        } else if (appInfoField.equals(AppInfoField.APP_NAME)) {
            // do nothing, will be sorted by name at the bottom of this method
        } else if (appInfoField.equals(AppInfoField.APP_SIZE)) {
            comparator = comparing(ai -> -getStorageUsage(MainActivity.this, ai.getApplicationInfo()).getAppBytes());
        } else if (appInfoField.equals(AppInfoField.CACHE_SIZE)) {
            comparator = comparing(ai -> -getStorageUsage(MainActivity.this, ai.getApplicationInfo()).getCacheBytes());
        } else if (appInfoField.equals(AppInfoField.DATA_SIZE)) {
            comparator = comparing(ai -> -getStorageUsage(MainActivity.this, ai.getApplicationInfo()).getDataBytes());
        } else if (appInfoField.equals(AppInfoField.ENABLED)) {
            comparator = comparing(ai -> ApplicationInfoUtils.getEnabledText(ai.getApplicationInfo()));
        } else if (appInfoField.equals(AppInfoField.EXTERNAL_CACHE_SIZE)) {
            comparator = comparing(ai -> -getStorageUsage(MainActivity.this, ai.getApplicationInfo()).getExternalCacheBytes());
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
            comparator = comparing(ai -> Formatter.formatShortFileSize(MainActivity.this, getStorageUsage(MainActivity.this, ai.getApplicationInfo()).getTotalBytes()));
        } else if (appInfoField.equals(AppInfoField.VERSION)) {
            comparator = comparing(ai -> {
                try {
                    return getVersionText(packageManager, ai.getApplicationInfo());
                } catch (PackageManager.NameNotFoundException e) {
                    return "";
                }
            });
        }
        comparator = comparator.thenComparing(ai -> String.valueOf(ai.getApplicationInfo().loadLabel(packageManager)));
        return comparator;
    }
}
