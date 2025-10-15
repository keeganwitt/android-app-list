package com.github.keeganwitt.applist;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.Collator;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, AppInfoAdapter.OnClickListener {
    private List<AppInfoField> appInfoFields;
    private AppInfoField selectedAppInfoField;
    private boolean descendingSortOrder;
    private AppInfoAdapter appInfoAdapter;
    private Spinner spinner;
    private ToggleButton toggleButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private boolean showSystemApps = false;
    private AppExporter appExporter;
    private AppListViewModel appListViewModel;

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

        progressBar = findViewById(R.id.progress_bar);
        recyclerView = findViewById(R.id.recycler_view);
        spinner = findViewById(R.id.spinner);
        toggleButton = findViewById(R.id.toggleButton);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        appInfoAdapter = new AppInfoAdapter(this, (android.app.usage.UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE), this);
        RecyclerView.LayoutManager layoutManager = new GridAutofitLayoutManager(this, 450);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(appInfoAdapter);

        appExporter = new AppExporter(this, appInfoAdapter, getPackageManager(), (android.app.usage.UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE));

        appListViewModel = new ViewModelProvider(this).get(AppListViewModel.class);
        observeViewModel();

        String[] appInfoFieldStrings = new String[]{
                getString(R.string.appInfoField_apkSize),
                getString(R.string.appInfoField_appSize),
                getString(R.string.appInfoField_archived),
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
        Collator collator = Collator.getInstance();
        Arrays.sort(appInfoFieldStrings, collator::compare);
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

        toggleButton.setOnCheckedChangeListener((compoundButton, b) -> {
            descendingSortOrder = !descendingSortOrder;
            loadApplications(false);
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadApplications(true);
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void observeViewModel() {
        appListViewModel.getAppList().observe(this, appList -> {
            appInfoAdapter.setUnfilteredList(appList);
            appInfoAdapter.submitList(appList);
        });

        appListViewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_menu, menu);

        MenuItem systemAppToggleItem = menu.findItem(R.id.systemAppToggle);
        systemAppToggleItem.setChecked(showSystemApps);

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
        //noinspection unchecked
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
        loadApplications(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.export) {
            appExporter.export(selectedAppInfoField);
            return true;
        } else if (itemId == R.id.systemAppToggle) {
            boolean isChecked = !item.isChecked();
            showSystemApps = isChecked;
            item.setChecked(isChecked);
            updateSystemAppToggleIcon(item);
            loadApplications(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateSystemAppToggleIcon(MenuItem item) {
        if (item.isChecked()) {
            item.setIcon(R.drawable.ic_system_apps_on);
        } else {
            item.setIcon(R.drawable.ic_system_apps_off);
        }
    }

    private void loadSelection(int position) {
        selectedAppInfoField = appInfoFields.get(position);
        if ((selectedAppInfoField.equals(AppInfoField.CACHE_SIZE)
                || selectedAppInfoField.equals(AppInfoField.DATA_SIZE)
                || selectedAppInfoField.equals(AppInfoField.EXTERNAL_CACHE_SIZE)
                || selectedAppInfoField.equals(AppInfoField.TOTAL_SIZE)
                || selectedAppInfoField.equals(AppInfoField.LAST_USED))
                && !hasUsageStatsPermission()) {
            requestUsageStatsPermission();
        }
        loadApplications(false);
    }

    private void loadApplications(boolean reload) {
        if (selectedAppInfoField != null) {
            appListViewModel.loadApps(selectedAppInfoField, showSystemApps, descendingSortOrder, reload);
        }
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
}
