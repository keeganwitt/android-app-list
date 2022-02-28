package com.github.keeganwitt.applist;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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

import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPackageInstaller;

public class MainActivity extends ListActivity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private PackageManager packageManager = null;
    private List<ApplicationInfo> appList = null;
    private ApplicationAdapter listAdaptor = null;
    private List<AppInfoField> appInfoFields;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        this.appInfoFields = Arrays.asList(AppInfoField.values());
        this.packageManager = getPackageManager();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        private Context context;
        private AppInfoField appInfoField;

        public LoadApplications(Context context, AppInfoField appInfoField) {
            this.context = context;
            this.appInfoField = appInfoField;
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            // TODO: sort by selected field
            installedApplications.sort(Comparator
                    .comparing((ApplicationInfo ai) -> String.valueOf(getPackageInstaller(packageManager, ai)))
                    .thenComparing(ai -> String.valueOf(ai.loadLabel(packageManager)))
            );
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
}
