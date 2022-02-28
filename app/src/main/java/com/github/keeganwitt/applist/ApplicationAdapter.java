package com.github.keeganwitt.applist;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import static com.github.keeganwitt.applist.ApplicationInfoUtils.getApkSizeText;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getEnabledText;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getFirstInstalledText;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getLastUpdatedText;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPackageInstaller;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPackageInstallerName;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPermissionsText;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getStorageUsage;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getVersionText;

public class ApplicationAdapter extends ArrayAdapter<ApplicationInfo> {
    private static final String TAG = ApplicationAdapter.class.getSimpleName();
    private final List<ApplicationInfo> appsList;
    private final Context context;
    private final PackageManager packageManager;
    private final AppInfoField appInfoField;

    public ApplicationAdapter(Context context, int textViewResourceId, List<ApplicationInfo> appsList, AppInfoField appInfoField) {
        super(context, textViewResourceId, appsList);
        this.context = context;
        this.appsList = appsList;
        this.appInfoField = appInfoField;
        this.packageManager = context.getPackageManager();
    }

    @Override
    public int getCount() {
        return ((null != this.appsList) ? this.appsList.size() : 0);
    }

    @Override
    public ApplicationInfo getItem(int position) {
        return ((null != this.appsList) ? this.appsList.get(position) : null);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (null == view) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.snippet_list_row, null);
        }

        ApplicationInfo applicationInfo = this.appsList.get(position);
        if (null != applicationInfo) {
            ImageView iconView = view.findViewById(R.id.app_icon);
            TextView appNameView = view.findViewById(R.id.app_name);
            TextView appInfoView = view.findViewById(R.id.app_info);

            iconView.setImageDrawable(applicationInfo.loadIcon(this.packageManager));
            appNameView.setText(applicationInfo.loadLabel(this.packageManager));

            try {
                if (this.appInfoField.equals(AppInfoField.PACKAGE_NAME)) {
                    appInfoView.setText(applicationInfo.packageName);
                } else if (this.appInfoField.equals(AppInfoField.PACKAGE_MANAGER)) {
                    appInfoView.setText(getPackageInstallerName(getPackageInstaller(this.packageManager, applicationInfo)));
                } else if (this.appInfoField.equals(AppInfoField.ENABLED)) {
                    appInfoView.setText(getEnabledText(applicationInfo));
                } else if (this.appInfoField.equals(AppInfoField.MIN_SDK)) {
                    appInfoView.setText(applicationInfo.minSdkVersion);
                } else if (this.appInfoField.equals(AppInfoField.TARGET_SDK)) {
                    appInfoView.setText(applicationInfo.targetSdkVersion);
                } else if (this.appInfoField.equals(AppInfoField.APK_SIZE)) {
                    appInfoView.setText("APK size " + getApkSizeText(this.context, applicationInfo));
                } else if (this.appInfoField.equals(AppInfoField.APP_SIZE)) {
                    ApplicationInfoUtils.StorageUsage storageUsage = getStorageUsage(this.context, applicationInfo);
                    appInfoView.setText("App bytes " + Formatter.formatShortFileSize(this.context, storageUsage.getAppBytes()));
                } else if (this.appInfoField.equals(AppInfoField.CACHE_SIZE)) {
                    ApplicationInfoUtils.StorageUsage storageUsage = getStorageUsage(this.context, applicationInfo);
                    appInfoView.setText("Cache bytes " + Formatter.formatShortFileSize(this.context, storageUsage.getCacheBytes()));
                } else if (this.appInfoField.equals(AppInfoField.DATA_SIZE)) {
                    ApplicationInfoUtils.StorageUsage storageUsage = getStorageUsage(this.context, applicationInfo);
                    appInfoView.setText("Data bytes " + Formatter.formatShortFileSize(this.context, storageUsage.getDataBytes()));
                } else if (this.appInfoField.equals(AppInfoField.EXTERNAL_CACHE_SIZE)) {
                    ApplicationInfoUtils.StorageUsage storageUsage = getStorageUsage(this.context, applicationInfo);
                    appInfoView.setText("External cache bytes " + Formatter.formatShortFileSize(this.context, storageUsage.getExternalCacheBytes()));
                } else if (this.appInfoField.equals(AppInfoField.TOTAL_SIZE)) {
                    ApplicationInfoUtils.StorageUsage storageUsage = getStorageUsage(this.context, applicationInfo);
                    appInfoView.setText("Total bytes " + Formatter.formatShortFileSize(this.context, storageUsage.getTotalBytes()));
                } else if (this.appInfoField.equals(AppInfoField.VERSION)) {
                    appInfoView.setText(getVersionText(this.packageManager, applicationInfo));
                } else if (this.appInfoField.equals(AppInfoField.PERMISSIONS)) {
                    appInfoView.setText(getPermissionsText(this.packageManager, applicationInfo));
                } else if (this.appInfoField.equals(AppInfoField.FIRST_INSTALLED)) {
                    appInfoView.setText(getFirstInstalledText(this.packageManager, applicationInfo));
                } else if (this.appInfoField.equals(AppInfoField.LAST_UPDATED)) {
                    appInfoView.setText(getLastUpdatedText(this.packageManager, applicationInfo));
                }
            } catch (PackageManager.NameNotFoundException e) {
                if (convertView != null) {
                    Toast.makeText(convertView.getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Log.e(TAG, "Unable to set requested text", e);
                }
            }
        }

        return view;
    }
}
