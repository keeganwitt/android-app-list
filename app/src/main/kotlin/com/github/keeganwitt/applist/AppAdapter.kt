package com.github.keeganwitt.applist

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.keeganwitt.applist.AppAdapter.AppInfoViewHolder
import com.github.keeganwitt.applist.utils.IconLoader
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AppAdapter(
    private val lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    private val onClickListener: OnClickListener,
    private val iconLoader: IconLoader,
) : ListAdapter<AppItemUiModel, AppInfoViewHolder>(
        AsyncDifferConfig.Builder(DiffCallback()).build(),
    ) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): AppInfoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.snippet_list_row, parent, false)
        return AppInfoViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: AppInfoViewHolder,
        position: Int,
    ) {
        val item = currentList[position]
        val iconView = holder.iconView
        val appNameView = holder.appNameView
        val appInfoView = holder.appInfoView
        val packageNameView = holder.packageNameView

        // Cancel any existing load job for this view holder
        holder.iconLoadJob?.cancel()

        // Clear previous icon to avoid flickering old images
        iconView.setImageDrawable(null)

        packageNameView.text = item.packageName
        appNameView.text = item.appName
        appInfoView.movementMethod = LinkMovementMethod.getInstance()
        appInfoView.text = item.infoText

        // Load icon asynchronously
        holder.iconLoadJob =
            lifecycleOwner.lifecycleScope.launch {
                val icon = iconLoader.loadIcon(item.packageName)
                if (icon != null) {
                    iconView.setImageDrawable(icon)
                } else {
                    // Set a default icon if needed, or leave null/transparent
                    iconView.setImageResource(android.R.drawable.sym_def_app_icon)
                }
            }
    }

    interface OnClickListener {
        fun onClick(position: Int)
    }

    inner class AppInfoViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var iconView: ImageView = itemView.findViewById(R.id.app_icon)
        var appNameView: TextView = itemView.findViewById(R.id.app_name)
        var packageNameView: TextView = itemView.findViewById(R.id.package_name)
        var appInfoView: TextView = itemView.findViewById(R.id.app_info)
        var iconLoadJob: Job? = null

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val position = getBindingAdapterPosition()
            onClickListener.onClick(position)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<AppItemUiModel>() {
        override fun areItemsTheSame(
            oldItem: AppItemUiModel,
            newItem: AppItemUiModel,
        ): Boolean = oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(
            oldItem: AppItemUiModel,
            newItem: AppItemUiModel,
        ): Boolean = oldItem == newItem
    }
}
