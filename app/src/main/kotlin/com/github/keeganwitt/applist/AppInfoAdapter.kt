package com.github.keeganwitt.applist

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.keeganwitt.applist.AppInfoAdapter.AppInfoViewHolder
import java.util.Locale

class AppInfoAdapter(
    private val context: Context,
    private val usageStatsManager: UsageStatsManager,
    private val onClickListener: OnClickListener
) : ListAdapter<AppInfo, AppInfoViewHolder>(
    AsyncDifferConfig.Builder(DiffCallback()).build()
), Filterable {
    private val packageManager: PackageManager = context.packageManager
    private val applicationInfoFilter: ApplicationInfoFilter
    private var unfilteredList: List<AppInfo>? = null

    init {
        this.applicationInfoFilter = ApplicationInfoFilter()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppInfoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.snippet_list_row, parent, false)
        return AppInfoViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppInfoViewHolder, position: Int) {
        val appInfo = currentList[position]
        val iconView = holder.iconView
        val appNameView = holder.appNameView
        val appInfoView = holder.appInfoView
        val packageNameView = holder.packageNameView

        iconView.setImageDrawable(appInfo.applicationInfo.loadIcon(packageManager))
        packageNameView.text = appInfo.applicationInfo.packageName
        appNameView.text = appInfo.applicationInfo.loadLabel(packageManager)

        try {
            appInfoView.text = appInfo.getTextValue(
                context,
                packageManager,
                usageStatsManager
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(
                TAG,
                "Unable to set requested text for " + appInfo.appInfoField + " for app " + appInfo.applicationInfo.packageName,
                e
            )
        }

    }

    override fun getFilter(): Filter {
        return applicationInfoFilter
    }

    fun setUnfilteredList(appList: List<AppInfo>?) {
        this.unfilteredList = appList
    }

    interface OnClickListener {
        fun onClick(position: Int)
    }

    inner class AppInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var iconView: ImageView = itemView.findViewById(R.id.app_icon)
        var appNameView: TextView = itemView.findViewById(R.id.app_name)
        var packageNameView: TextView = itemView.findViewById(R.id.package_name)
        var appInfoView: TextView = itemView.findViewById(R.id.app_info)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val position = getBindingAdapterPosition()
            onClickListener.onClick(position)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.applicationInfo.packageName == newItem.applicationInfo.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.appInfoField == newItem.appInfoField
        }
    }

    internal inner class ApplicationInfoFilter : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filteredList: MutableList<AppInfo> = ArrayList()
            if (constraint == null || constraint.isEmpty()) {
                filteredList.addAll(unfilteredList ?: emptyList())
            } else {
                val filterPattern =
                    constraint.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }
                for (item in this@AppInfoAdapter.currentList) {
                    val packageName = item.applicationInfo.loadLabel(packageManager).toString()
                        .lowercase(Locale.getDefault())
                    var textValue: String?
                    try {
                        textValue = item.getTextValue(context, packageManager, usageStatsManager)
                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.e(TAG, "Unable to calculate text value for search", e)
                        textValue = ""
                    }
                    if (packageName.contains(filterPattern) || textValue!!.lowercase(Locale.getDefault())
                            .contains(filterPattern)
                    ) {
                        filteredList.add(item)
                    }
                }
            }
            val results = FilterResults()
            results.count = filteredList.size
            results.values = filteredList
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            @Suppress("UNCHECKED_CAST")
            submitList(results.values as List<AppInfo>)
        }
    }

    companion object {
        private val TAG: String = AppInfoAdapter::class.java.getSimpleName()
    }
}
