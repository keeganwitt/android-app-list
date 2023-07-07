package com.github.keeganwitt.applist;

import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppInfoAdapter extends ListAdapter<AppInfo, AppInfoAdapter.AppInfoViewHolder> implements Filterable {
    private static final String TAG = AppInfoAdapter.class.getSimpleName();
    private final Context context;
    private final OnClickListener onClickListener;
    private final PackageManager packageManager;
    private final UsageStatsManager usageStatsManager;
    private final ApplicationInfoFilter applicationInfoFilter;
    private List<AppInfo> unfilteredList;

    public AppInfoAdapter(Context context, UsageStatsManager usageStatsManager, OnClickListener onClickListener) {
        super(new AsyncDifferConfig.Builder<>(new DiffCallback()).build());
        this.context = context;
        this.onClickListener = onClickListener;
        this.packageManager = context.getPackageManager();
        this.usageStatsManager = usageStatsManager;
        this.applicationInfoFilter = new ApplicationInfoFilter();
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
                appInfoView.setText(appInfo.getTextValue(context, packageManager, usageStatsManager));
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Unable to set requested text for " + appInfo.getAppInfoField() + " for app " + appInfo.getApplicationInfo().packageName, e);
            }
        }
    }

    @Override
    public Filter getFilter() {
        return applicationInfoFilter;
    }

    public void setUnfilteredList(List<AppInfo> appList) {
        this.unfilteredList = appList;
    }

    public interface OnClickListener {
        void onClick(int position);
    }

    @SuppressWarnings("RedundantSuppression")
    protected class AppInfoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
        ImageView iconView;
        @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
        TextView appNameView;
        @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
        TextView packageNameView;
        @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
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

    private static class DiffCallback extends DiffUtil.ItemCallback<AppInfo> {
        @Override
        public boolean areItemsTheSame(@NonNull AppInfo oldItem, @NonNull AppInfo newItem) {
            return oldItem.getApplicationInfo().packageName.equals(newItem.getApplicationInfo().packageName);
        }

        @Override
        public boolean areContentsTheSame(@NonNull AppInfo oldItem, @NonNull AppInfo newItem) {
            return oldItem.getAppInfoField().equals(newItem.getAppInfoField());
        }
    }

    class ApplicationInfoFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<AppInfo> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(unfilteredList);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (AppInfo item : AppInfoAdapter.this.getCurrentList()) {
                    String packageName = String.valueOf(item.getApplicationInfo().loadLabel(packageManager)).toLowerCase();
                    String textValue;
                    try {
                        textValue = item.getTextValue(context, packageManager, usageStatsManager);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e(TAG, "Unable to calculate text value for search", e);
                        textValue = "";
                    }
                    if (packageName.contains(filterPattern) || textValue.toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.count = filteredList.size();
            results.values = filteredList;
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            submitList((List<AppInfo>) results.values);
        }
    }
}
