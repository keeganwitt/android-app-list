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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static com.github.keeganwitt.applist.ApplicationInfoUtils.getApkSize;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getFirstInstalled;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getLastUpdated;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPackageInstaller;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPackageInstallerName;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPermissions;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getStorageUsage;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getVersionText;
import static java.util.Comparator.comparing;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, ApplicationInfoAdapter.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private PackageManager packageManager = null;
    private List<ApplicationInfo> appList = null;
    private List<AppInfoField> appInfoFields;
    private AppInfoField selectedAppInfoField;
    private ApplicationInfoAdapter applicationInfoAdapter;
    private Spinner spinner;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!hasUsageStatsPermission()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }

        setContentView(R.layout.activity_main);

        this.appInfoFields = Arrays.asList(AppInfoField.values());
        this.packageManager = getPackageManager();

        this.spinner = findViewById(R.id.spinner);
        this.spinner.setOnItemSelectedListener(this);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                this.appInfoFields.stream().map(AppInfoField::getDisplayName).toArray(String[]::new));
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.spinner.setAdapter(arrayAdapter);

        this.progressBar = findViewById(R.id.progress_bar);

        this.recyclerView = findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, 3);
        this.recyclerView.setLayoutManager(layoutManager);
        this.applicationInfoAdapter = new ApplicationInfoAdapter(MainActivity.this, this);
        this.recyclerView.setAdapter(this.applicationInfoAdapter);

        this.swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        this.swipeRefreshLayout.setOnRefreshListener(() -> {
            loadApplications(this.selectedAppInfoField, true);
            this.swipeRefreshLayout.setRefreshing(false);
        });
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(int position) {
        ApplicationInfo app = this.appList.get(position);
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + app.packageName));
        startActivity(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        this.recyclerView.setVisibility(View.GONE);
        this.progressBar.setVisibility(View.VISIBLE);
        this.selectedAppInfoField = this.appInfoFields.get(position);
        loadApplications(this.selectedAppInfoField, false);
        this.recyclerView.setVisibility(View.VISIBLE);
        this.progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        this.recyclerView.setVisibility(View.GONE);
        this.progressBar.setVisibility(View.VISIBLE);
        this.selectedAppInfoField = this.appInfoFields.get(0);
        loadApplications(this.selectedAppInfoField, false);
        this.recyclerView.setVisibility(View.VISIBLE);
        this.progressBar.setVisibility(View.GONE);
    }

    private List<ApplicationInfo> checkForLaunchIntent(List<ApplicationInfo> list) {
        ArrayList<ApplicationInfo> appList = new ArrayList<>();
        for (ApplicationInfo info : list) {
            try {
                // TODO: let user choose between system and user apps
                if (null != packageManager.getLaunchIntentForPackage(info.packageName) && isUserInstalledApp(info)) {
                    appList.add(info);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return appList;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadApplications(AppInfoField appInfoField, boolean reload) {
        if (this.appList == null || this.appList.isEmpty() || reload) {
            this.appList = checkForLaunchIntent(this.packageManager.getInstalledApplications(PackageManager.GET_META_DATA));
        }
        this.appList.sort(determineComparator(this.packageManager, appInfoField));
        this.applicationInfoAdapter.populateAppsList(this.appList, appInfoField);
        this.applicationInfoAdapter.notifyDataSetChanged();
    }

    private boolean isUserInstalledApp(ApplicationInfo appInfo) {
        return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1;
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) MainActivity.this.getSystemService(Context.APP_OPS_SERVICE);
        int mode;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            mode = appOps.unsafeCheckOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), MainActivity.this.getPackageName());
        } else {
            mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), MainActivity.this.getPackageName());
        }
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private Comparator<ApplicationInfo> determineComparator(PackageManager packageManager, AppInfoField appInfoField) {
        Comparator<ApplicationInfo> comparator = comparing(ai -> String.valueOf(ai.loadLabel(this.packageManager)));
        if (appInfoField.equals(AppInfoField.APK_SIZE)) {
            comparator = comparing(ai -> -getApkSize(MainActivity.this, ai));
        } else if (appInfoField.equals(AppInfoField.APP_SIZE)) {
            comparator = comparing(ai -> -getStorageUsage(MainActivity.this, ai).getAppBytes());
        } else if (appInfoField.equals(AppInfoField.CACHE_SIZE)) {
            comparator = comparing(ai -> -getStorageUsage(MainActivity.this, ai).getCacheBytes());
        } else if (appInfoField.equals(AppInfoField.DATA_SIZE)) {
            comparator = comparing(ai -> -getStorageUsage(MainActivity.this, ai).getDataBytes());
        } else if (appInfoField.equals(AppInfoField.ENABLED)) {
            comparator = comparing(ApplicationInfoUtils::getEnabledText);
        } else if (appInfoField.equals(AppInfoField.EXTERNAL_CACHE_SIZE)) {
            comparator = comparing(ai -> -getStorageUsage(MainActivity.this, ai).getExternalCacheBytes());
        } else if (appInfoField.equals(AppInfoField.FIRST_INSTALLED)) {
            comparator = comparing(ai -> {
                try {
                    return getFirstInstalled(this.packageManager, ai).getTime();
                } catch (PackageManager.NameNotFoundException e) {
                    return 0L;
                }
            });
        } else if (appInfoField.equals(AppInfoField.LAST_UPDATED)) {
            comparator = comparing(ai -> {
                try {
                    return getLastUpdated(this.packageManager, ai).getTime();
                } catch (PackageManager.NameNotFoundException e) {
                    return 0L;
                }
            });
        } else if (appInfoField.equals(AppInfoField.MIN_SDK)) {
            comparator = comparing(ai -> ai.minSdkVersion);
        } else if (appInfoField.equals(AppInfoField.PACKAGE_MANAGER)) {
            comparator = comparing(ai -> getPackageInstallerName(getPackageInstaller(this.packageManager, ai)));
        } else if (appInfoField.equals(AppInfoField.PERMISSIONS)) {
            comparator = comparing(ai -> {
                try {
                    return getPermissions(packageManager, ai).length;
                } catch (NullPointerException | PackageManager.NameNotFoundException e) {
                    return 0;
                }
            });
        } else if (appInfoField.equals(AppInfoField.TARGET_SDK)) {
            comparator = comparing(ai -> ai.targetSdkVersion);
        } else if (appInfoField.equals(AppInfoField.TOTAL_SIZE)) {
            comparator = comparing(ai -> Formatter.formatShortFileSize(MainActivity.this, getStorageUsage(MainActivity.this, ai).getTotalBytes()));
        } else if (appInfoField.equals(AppInfoField.VERSION)) {
            comparator = comparing(ai -> {
                try {
                    return getVersionText(this.packageManager, ai);
                } catch (PackageManager.NameNotFoundException e) {
                    return "";
                }
            });
        }
        comparator = comparator.thenComparing(ai -> String.valueOf(ai.loadLabel(this.packageManager)));
        return comparator;
    }
}
