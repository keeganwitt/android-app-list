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

import android.app.AppOpsManager;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, AppInfoAdapter.OnClickListener {
    @SuppressWarnings("unused")
    private static final String TAG = MainActivity.class.getSimpleName();
    private PackageManager packageManager;
    private UsageStatsManager usageStatsManager;
    private final ExecutorService appListLoader = Executors.newSingleThreadExecutor();
    private Future<?> loaderTask;
    private List<AppInfoField> appInfoFields;
    private AppInfoField selectedAppInfoField;
    private boolean descendingSortOrder;
    private AppInfoAdapter appInfoAdapter;
    @SuppressWarnings("FieldCanBeLocal")
    private Spinner spinner;
    private ToggleButton toggleButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private boolean showSystemApps = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        appInfoFields = Arrays.asList(AppInfoField.values());
        packageManager = getPackageManager();
        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        spinner = findViewById(R.id.spinner);
        String[] appInfoFieldStrings = new String[]{
                getString(R.string.appInfoField_apkSize),
                getString(R.string.appInfoField_appSize),
                getString(R.string.appInfoField_cacheSize),
                getString(R.string.appInfoField_dataSize),
                getString(R.string.appInfoField_enabled),
                getString(R.string.appInfoField_exists_in_app_store),
                getString(R.string.appInfoField_externalCacheSize),
                getString(R.string.appInfoField_firstInstalled),
                getString(R.string.appInfoField_grantedPermissions),
                getString(R.string.appInfoField_lastUpdated),
                getString(R.string.appInfoField_lastUsed),
                getString(R.string.appInfoField_minSdk),
                getString(R.string.appInfoField_packageManager),
                getString(R.string.appInfoField_requestedPermissions),
                getString(R.string.appInfoField_targetSdk),
                getString(R.string.appInfoField_totalSize),
                getString(R.string.appInfoField_version),
        };
        Arrays.sort(appInfoFieldStrings);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, appInfoFieldStrings);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);

        // Set initial selection to "Version"
        int versionIndex = -1;
        for (int i = 0; i < appInfoFieldStrings.length; i++) {
            if (appInfoFieldStrings[i].equals(getString(R.string.appInfoField_version))) {
                versionIndex = i;
                break;
            }
        }
        if (versionIndex != -1) {
            spinner.setSelection(versionIndex);
        }
        spinner.setOnItemSelectedListener(this);

        toggleButton = findViewById(R.id.toggleButton);
        toggleButton.setOnCheckedChangeListener((compoundButton, b) -> {
            descendingSortOrder = !descendingSortOrder;
            loadApplications(selectedAppInfoField, false);
        });

        progressBar = findViewById(R.id.progress_bar);

        recyclerView = findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new GridAutofitLayoutManager(this, 450);
        recyclerView.setLayoutManager(layoutManager);
        appInfoAdapter = new AppInfoAdapter(MainActivity.this, usageStatsManager, this);
        recyclerView.setAdapter(appInfoAdapter);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadApplications(selectedAppInfoField, true);
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_menu, menu);

        MenuItem systemAppToggleItem = menu.findItem(R.id.systemAppToggle);
        SwitchMaterial switchMaterial = (SwitchMaterial) systemAppToggleItem.getActionView();
        Objects.requireNonNull(switchMaterial).setOnClickListener(v -> {
            showSystemApps = !showSystemApps;
            loadApplications(selectedAppInfoField, true);
        });

        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        Objects.requireNonNull(searchView).setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                appInfoAdapter.getFilter().filter(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String query) {
                appInfoAdapter.getFilter().filter(query);
                return true;
            }
        });

        return true;
    }

    @Override
    public void onClick(int position) {
        AppInfo app = appInfoAdapter.getCurrentList().get(position);
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + app.getApplicationInfo().packageName));
        startActivity(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        loadSelection(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Find the index of "Version" in the spinner's adapter
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
        int versionIndex = -1;
        String versionText = getString(R.string.appInfoField_version);
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(versionText)) {
                versionIndex = i;
                break;
            }
        }
        if (versionIndex != -1) {
            spinner.setSelection(versionIndex);
            loadSelection(versionIndex);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // reload true in case application was changed (permission changed, enabled, uninstalled, etc) from application info
        // TODO: reload only selected item instead of entire list
        loadApplications(selectedAppInfoField, true);
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

    private List<AppInfo> filterUserOrSystemApps(List<AppInfo> list) {
        ArrayList<AppInfo> appList = new ArrayList<>();
        for (AppInfo info : list) {
            try {
                if (packageManager.getLaunchIntentForPackage(info.getApplicationInfo().packageName) != null
                        && ((!showSystemApps && isUserInstalledApp(info.getApplicationInfo()))
                        || (showSystemApps && !isUserInstalledApp(info.getApplicationInfo())))) {
                    appList.add(info);
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to filter by user/system", e);
            }
        }

        return appList;
    }

    private void loadApplications(AppInfoField appInfoField, boolean reload) {
        recyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        if (loaderTask != null) {
            loaderTask.cancel(true);
        }
        loaderTask = appListLoader.submit(() -> {
            List<AppInfo> appList;
            appList = MainActivity.this.filterUserOrSystemApps(packageManager.getInstalledApplications(PackageManager.GET_META_DATA).stream()
                            .map(it -> new AppInfo(it, appInfoField))
                            .collect(Collectors.toList())).stream()
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
            appInfoAdapter.setUnfilteredList(appList);
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

    @SuppressWarnings("StatementWithEmptyBody")
    private Comparator<AppInfo> determineComparator(PackageManager packageManager, AppInfoField appInfoField) {
        Comparator<AppInfo> comparator = comparing(ai -> String.valueOf(ai.getApplicationInfo().loadLabel(packageManager)));
        if (appInfoField.equals(AppInfoField.APK_SIZE)) {
            comparator = comparing(ai -> getApkSize(ai.getApplicationInfo()));
        } else if (appInfoField.equals(AppInfoField.APP_SIZE)) {
            comparator = comparing(ai -> getStorageUsage(MainActivity.this, ai.getApplicationInfo()).getAppBytes());
        } else if (appInfoField.equals(AppInfoField.CACHE_SIZE)) {
            comparator = comparing(ai -> getStorageUsage(MainActivity.this, ai.getApplicationInfo()).getCacheBytes());
        } else if (appInfoField.equals(AppInfoField.DATA_SIZE)) {
            comparator = comparing(ai -> getStorageUsage(MainActivity.this, ai.getApplicationInfo()).getDataBytes());
        } else if (appInfoField.equals(AppInfoField.ENABLED)) {
            comparator = comparing(ai -> ApplicationInfoUtils.getEnabledText(MainActivity.this, ai.getApplicationInfo()));
        } else if (appInfoField.equals(AppInfoField.EXISTS_IN_APP_STORE)) {
            comparator = comparing(ai -> ApplicationInfoUtils.getExistsInAppStoreText(MainActivity.this, packageManager, ai.getApplicationInfo()));
        } else if (appInfoField.equals(AppInfoField.EXTERNAL_CACHE_SIZE)) {
            comparator = comparing(ai -> getStorageUsage(MainActivity.this, ai.getApplicationInfo()).getExternalCacheBytes());
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
            comparator = comparing(ai -> getStorageUsage(MainActivity.this, ai.getApplicationInfo()).getTotalBytes());
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
