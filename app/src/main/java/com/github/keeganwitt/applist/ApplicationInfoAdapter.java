package com.github.keeganwitt.applist;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.stream.Collectors;

import static com.github.keeganwitt.applist.ApplicationInfoUtils.getApkSizeText;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getEnabledText;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getFirstInstalledText;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getLastUpdatedText;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPackageInstaller;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPackageInstallerName;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getPermissions;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getStorageUsage;
import static com.github.keeganwitt.applist.ApplicationInfoUtils.getVersionText;

public class ApplicationInfoAdapter extends RecyclerView.Adapter<ApplicationInfoAdapter.ApplicationInfoViewHolder>  {
    private static final String TAG = ApplicationInfoAdapter.class.getSimpleName();
    private final Context context;
    private final OnClickListener onClickListener;
    private final PackageManager packageManager;
    private  AppInfoField appInfoField;
    private List<ApplicationInfo> appsList;

    public ApplicationInfoAdapter(Context context, OnClickListener onClickListener) {
        this.context = context;
        this.onClickListener = onClickListener;
        this.packageManager = context.getPackageManager();
    }

    @NonNull
    @Override
    public ApplicationInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.snippet_list_row, parent, false);
        return new ApplicationInfoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicationInfoViewHolder holder, int position) {
        ApplicationInfo applicationInfo = this.appsList.get(position);
        if (null != applicationInfo) {
            ImageView iconView = holder.iconView;
            TextView appNameView = holder.appNameView;
            TextView appInfoView = holder.appInfoView;
            TextView packageNameView = holder.packageNameView;

            iconView.setImageDrawable(applicationInfo.loadIcon(this.packageManager));
            packageNameView.setText(applicationInfo.packageName);
            appNameView.setText(applicationInfo.loadLabel(this.packageManager));

            try {
                if (this.appInfoField.equals(AppInfoField.APK_SIZE)) {
                    appInfoView.setText(getApkSizeText(this.context, applicationInfo));
                } else if (this.appInfoField.equals(AppInfoField.APP_SIZE)) {
                    appInfoView.setText(Formatter.formatShortFileSize(this.context, getStorageUsage(this.context, applicationInfo).getAppBytes()));
                } else if (this.appInfoField.equals(AppInfoField.CACHE_SIZE)) {
                    appInfoView.setText(Formatter.formatShortFileSize(this.context, getStorageUsage(this.context, applicationInfo).getCacheBytes()));
                } else if (this.appInfoField.equals(AppInfoField.DATA_SIZE)) {
                    appInfoView.setText(Formatter.formatShortFileSize(this.context, getStorageUsage(this.context, applicationInfo).getDataBytes()));
                } else if (this.appInfoField.equals(AppInfoField.ENABLED)) {
                    appInfoView.setText(getEnabledText(applicationInfo));
                } else if (this.appInfoField.equals(AppInfoField.EXTERNAL_CACHE_SIZE)) {
                    appInfoView.setText(Formatter.formatShortFileSize(this.context, getStorageUsage(this.context, applicationInfo).getExternalCacheBytes()));
                } else if (this.appInfoField.equals(AppInfoField.FIRST_INSTALLED)) {
                    appInfoView.setText(getFirstInstalledText(this.packageManager, applicationInfo));
                } else if (this.appInfoField.equals(AppInfoField.GRANTED_PERMISSIONS)) {
                    appInfoView.setText(getPermissions(this.packageManager, applicationInfo, true).stream().collect(Collectors.joining(", ")));
                } else if (this.appInfoField.equals(AppInfoField.LAST_UPDATED)) {
                    appInfoView.setText(getLastUpdatedText(this.packageManager, applicationInfo));
                } else if (this.appInfoField.equals(AppInfoField.MIN_SDK)) {
                    appInfoView.setText(String.valueOf(applicationInfo.minSdkVersion));
                } else if (this.appInfoField.equals(AppInfoField.PACKAGE_MANAGER)) {
                    appInfoView.setText(getPackageInstallerName(getPackageInstaller(this.packageManager, applicationInfo)));
                } else if (this.appInfoField.equals(AppInfoField.REQUESTED_PERMISSIONS)) {
                    appInfoView.setText(getPermissions(this.packageManager, applicationInfo, false).stream().collect(Collectors.joining(", ")));
                } else if (this.appInfoField.equals(AppInfoField.TARGET_SDK)) {
                    appInfoView.setText(String.valueOf(applicationInfo.targetSdkVersion));
                } else if (this.appInfoField.equals(AppInfoField.TOTAL_SIZE)) {
                    appInfoView.setText(Formatter.formatShortFileSize(this.context, getStorageUsage(this.context, applicationInfo).getTotalBytes()));
                } else if (this.appInfoField.equals(AppInfoField.VERSION)) {
                    appInfoView.setText(getVersionText(this.packageManager, applicationInfo));
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Unable to set requested text for " + this.appInfoField + " for app " + applicationInfo.packageName, e);
            }
        }
    }

    @Override
    public int getItemCount () {
        return ((null != this.appsList) ? this.appsList.size() : 0);
    }

    public void populateAppsList(List<ApplicationInfo> appsList, AppInfoField appInfoField) {
        this.appsList = appsList;
        this.appInfoField = appInfoField;
    }

    public interface OnClickListener {
        void onClick(int position);
    }

    public class ApplicationInfoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView iconView;
        TextView appNameView;
        TextView packageNameView;
        TextView appInfoView;

        public ApplicationInfoViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.app_icon);
            appNameView = itemView.findViewById(R.id.app_name);
            packageNameView = itemView.findViewById(R.id.package_name);
            appInfoView = itemView.findViewById(R.id.app_info);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getBindingAdapterPosition();
            onClickListener.onClick(position);
        }
    }
}
