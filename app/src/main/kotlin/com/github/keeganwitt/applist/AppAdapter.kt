package com.github.keeganwitt.applist

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.github.keeganwitt.applist.AppAdapter.AppInfoViewHolder
import com.github.keeganwitt.applist.utils.PackageIcon

class AppAdapter(
    private val onClickListener: OnClickListener,
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

        iconView.load(PackageIcon(item.packageName)) {
            placeholder(android.R.drawable.sym_def_app_icon)
            error(android.R.drawable.sym_def_app_icon)
            fallback(android.R.drawable.sym_def_app_icon)
        }

        packageNameView.text = item.packageName
        appNameView.text = item.appName
        appInfoView.movementMethod = LinkMovementMethod.getInstance()
        appInfoView.text = item.infoText
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

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val position = getBindingAdapterPosition()
            if (position != RecyclerView.NO_POSITION) {
                onClickListener.onClick(position)
            }
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
