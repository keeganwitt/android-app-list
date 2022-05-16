package com.github.keeganwitt.applist;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

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

public class AppInfoAdapter extends ListAdapter<AppInfo, AppInfoAdapter.AppInfoViewHolder> {
    private static final String TAG = AppInfoAdapter.class.getSimpleName();
    private final Context context;
    private final OnClickListener onClickListener;
    private final PackageManager packageManager;

    public AppInfoAdapter(Context context, OnClickListener onClickListener) {
        super(new AsyncDifferConfig.Builder<>(new DiffCallback()).build());
        this.context = context;
        this.onClickListener = onClickListener;
        this.packageManager = context.getPackageManager();
    }

    @NonNull
    @Override
    public AppInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.snippet_list_row, parent, false);
        return new AppInfoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppInfoViewHolder holder, int position) {
        AppInfo appInfo = getCurrentList().get(position);
        if (null != appInfo) {
            ImageView iconView = holder.iconView;
            TextView appNameView = holder.appNameView;
            TextView appInfoView = holder.appInfoView;
            TextView packageNameView = holder.packageNameView;

            iconView.setImageDrawable(appInfo.getApplicationInfo().loadIcon(packageManager));
            packageNameView.setText(appInfo.getApplicationInfo().packageName);
            appNameView.setText(appInfo.getApplicationInfo().loadLabel(packageManager));

            try {
                if (appInfo.getAppInfoField().equals(AppInfoField.APK_SIZE)) {
                    appInfoView.setText(getApkSizeText(context, appInfo.getApplicationInfo()));
                } else if (appInfo.getAppInfoField().equals(AppInfoField.APP_SIZE)) {
                    appInfoView.setText(Formatter.formatShortFileSize(context, getStorageUsage(context, appInfo.getApplicationInfo()).getAppBytes()));
                } else if (appInfo.getAppInfoField().equals(AppInfoField.CACHE_SIZE)) {
                    appInfoView.setText(Formatter.formatShortFileSize(context, getStorageUsage(context, appInfo.getApplicationInfo()).getCacheBytes()));
                } else if (appInfo.getAppInfoField().equals(AppInfoField.DATA_SIZE)) {
                    appInfoView.setText(Formatter.formatShortFileSize(context, getStorageUsage(context, appInfo.getApplicationInfo()).getDataBytes()));
                } else if (appInfo.getAppInfoField().equals(AppInfoField.ENABLED)) {
                    appInfoView.setText(getEnabledText(appInfo.getApplicationInfo()));
                } else if (appInfo.getAppInfoField().equals(AppInfoField.EXTERNAL_CACHE_SIZE)) {
                    appInfoView.setText(Formatter.formatShortFileSize(context, getStorageUsage(context, appInfo.getApplicationInfo()).getExternalCacheBytes()));
                } else if (appInfo.getAppInfoField().equals(AppInfoField.FIRST_INSTALLED)) {
                    appInfoView.setText(getFirstInstalledText(packageManager, appInfo.getApplicationInfo()));
                } else if (appInfo.getAppInfoField().equals(AppInfoField.GRANTED_PERMISSIONS)) {
                    appInfoView.setText(getPermissions(packageManager, appInfo.getApplicationInfo(), true).stream().collect(Collectors.joining(", ")));
                } else if (appInfo.getAppInfoField().equals(AppInfoField.LAST_UPDATED)) {
                    appInfoView.setText(getLastUpdatedText(packageManager, appInfo.getApplicationInfo()));
                } else if (appInfo.getAppInfoField().equals(AppInfoField.MIN_SDK)) {
                    appInfoView.setText(String.valueOf(appInfo.getApplicationInfo().minSdkVersion));
                } else if (appInfo.getAppInfoField().equals(AppInfoField.PACKAGE_MANAGER)) {
                    appInfoView.setText(getPackageInstallerName(getPackageInstaller(packageManager, appInfo.getApplicationInfo())));
                } else if (appInfo.getAppInfoField().equals(AppInfoField.REQUESTED_PERMISSIONS)) {
                    appInfoView.setText(getPermissions(packageManager, appInfo.getApplicationInfo(), false).stream().collect(Collectors.joining(", ")));
                } else if (appInfo.getAppInfoField().equals(AppInfoField.TARGET_SDK)) {
                    appInfoView.setText(String.valueOf(appInfo.getApplicationInfo().targetSdkVersion));
                } else if (appInfo.getAppInfoField().equals(AppInfoField.TOTAL_SIZE)) {
                    appInfoView.setText(Formatter.formatShortFileSize(context, getStorageUsage(context, appInfo.getApplicationInfo()).getTotalBytes()));
                } else if (appInfo.getAppInfoField().equals(AppInfoField.VERSION)) {
                    appInfoView.setText(getVersionText(packageManager, appInfo.getApplicationInfo()));
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Unable to set requested text for " + appInfo.getAppInfoField() + " for app " + appInfo.getApplicationInfo().packageName, e);
            }
        }
    }

    public interface OnClickListener {
        void onClick(int position);
    }

    public class AppInfoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView iconView;
        TextView appNameView;
        TextView packageNameView;
        TextView appInfoView;

        public AppInfoViewHolder(@NonNull View itemView) {
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

    public static class DiffCallback extends DiffUtil.ItemCallback<AppInfo> {
        @Override
        public boolean areItemsTheSame(@NonNull AppInfo oldItem, @NonNull AppInfo newItem) {
            return oldItem.getApplicationInfo().packageName.equals(newItem.getApplicationInfo().packageName);
        }

        @Override
        public boolean areContentsTheSame(@NonNull AppInfo oldItem, @NonNull AppInfo newItem) {
            return oldItem.getApplicationInfo().packageName.equals(newItem.getApplicationInfo().packageName)
                    && oldItem.getAppInfoField().equals(newItem.getAppInfoField());
        }
    }
}
