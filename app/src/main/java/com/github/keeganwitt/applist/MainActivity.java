package com.github.keeganwitt.applist;

import android.app.AppOpsManager;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

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

public class MainActivity extends ListActivity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private PackageManager packageManager = null;
    private List<ApplicationInfo> appList = null;
    private ApplicationAdapter listAdaptor = null;
    private List<AppInfoField> appInfoFields;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!hasUsageStatsPermission(MainActivity.this)) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }

        this.appInfoFields = Arrays.asList(AppInfoField.values());
        this.packageManager = getPackageManager();

        Spinner spin = findViewById(R.id.spinner);
        spin.setOnItemSelectedListener(this);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, this.appInfoFields.stream().map(AppInfoField::getDisplayName).toArray(String[]::new));
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(arrayAdapter);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        ApplicationInfo app = appList.get(position);
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + app.packageName));
        startActivity(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        new LoadApplications(MainActivity.this, this.appInfoFields.get(position)).execute();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

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

    private class LoadApplications extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progress = null;
        private final Context context;
        private final AppInfoField appInfoField;
        private final PackageManager packageManager;

        public LoadApplications(Context context, AppInfoField appInfoField) {
            this.context = context;
            this.appInfoField = appInfoField;
            this.packageManager = this.context.getPackageManager();
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            Comparator<ApplicationInfo> comparator;
            boolean sortedByPackageLabel = false;
            if (this.appInfoField.equals(AppInfoField.APK_SIZE)) {
                comparator = comparing(ai -> getApkSize(this.context, ai));
            } else if (this.appInfoField.equals(AppInfoField.APP_SIZE)) {
                comparator = comparing(ai -> getStorageUsage(this.context, ai).getAppBytes());
            } else if (this.appInfoField.equals(AppInfoField.CACHE_SIZE)) {
                comparator = comparing(ai -> Formatter.formatShortFileSize(this.context, getStorageUsage(this.context, ai).getCacheBytes()));
            } else if (this.appInfoField.equals(AppInfoField.DATA_SIZE)) {
                comparator = comparing(ai -> Formatter.formatShortFileSize(this.context, getStorageUsage(this.context, ai).getDataBytes()));
            } else if (this.appInfoField.equals(AppInfoField.ENABLED)) {
                comparator = comparing(ApplicationInfoUtils::getEnabledText);
            } else if (this.appInfoField.equals(AppInfoField.EXTERNAL_CACHE_SIZE)) {
                comparator = comparing(ai -> Formatter.formatShortFileSize(this.context, getStorageUsage(this.context, ai).getExternalCacheBytes()));
            } else if (this.appInfoField.equals(AppInfoField.FIRST_INSTALLED)) {
                comparator = comparing(ai -> {
                    try {
                        return getFirstInstalled(this.packageManager, ai).getTime();
                    } catch (PackageManager.NameNotFoundException e) {
                        return 0L;
                    }
                });
            } else if (this.appInfoField.equals(AppInfoField.LAST_UPDATED)) {
                comparator = comparing(ai -> {
                    try {
                        return getLastUpdated(this.packageManager, ai).getTime();
                    } catch (PackageManager.NameNotFoundException e) {
                        return 0L;
                    }
                });
            } else if (this.appInfoField.equals(AppInfoField.MIN_SDK)) {
                comparator = comparing(ai -> ai.minSdkVersion);
            } else if (this.appInfoField.equals(AppInfoField.PACKAGE_MANAGER)) {
                comparator = comparing(ai -> getPackageInstallerName(getPackageInstaller(this.packageManager, ai)));
            } else if (this.appInfoField.equals(AppInfoField.PACKAGE_NAME)) {
                comparator = comparing(ai -> ai.packageName);
            } else if (this.appInfoField.equals(AppInfoField.PERMISSIONS)) {
                comparator = comparing(ai -> {
                    try {
                        return getPermissions(this.packageManager, ai).length;
                    } catch (PackageManager.NameNotFoundException e) {
                        return 0;
                    }
                });
            } else if (this.appInfoField.equals(AppInfoField.TARGET_SDK)) {
                comparator = comparing(ai -> ai.targetSdkVersion);
            } else if (this.appInfoField.equals(AppInfoField.TOTAL_SIZE)) {
                comparator = comparing(ai -> Formatter.formatShortFileSize(this.context, getStorageUsage(this.context, ai).getTotalBytes()));
            } else if (this.appInfoField.equals(AppInfoField.VERSION)) {
                comparator = comparing(ai -> {
                    try {
                        return getVersionText(this.packageManager, ai);
                    } catch (PackageManager.NameNotFoundException e) {
                        return "";
                    }
                });
            } else {
                comparator = comparing(ai -> String.valueOf(ai.loadLabel(packageManager)));
                sortedByPackageLabel = true;
            }
            if (!sortedByPackageLabel) {
                comparator = comparator.thenComparing(ai -> String.valueOf(ai.loadLabel(packageManager)));
            }
            installedApplications.sort(comparator);
            appList = checkForLaunchIntent(installedApplications);
            listAdaptor = new ApplicationAdapter(context, R.layout.snippet_list_row, appList, this.appInfoField);

            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(Void result) {
            setListAdapter(listAdaptor);
            progress.dismiss();
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(MainActivity.this, null, "Loading application info...");
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }

    private boolean isUserInstalledApp(ApplicationInfo appInfo) {
        return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1;
    }

    private boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
}
