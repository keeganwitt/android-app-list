package com.github.keeganwitt.applist

import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.github.keeganwitt.applist.AppAdapter.AppInfoViewHolder
import com.github.keeganwitt.applist.databinding.SnippetListRowBinding
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
        val binding = SnippetListRowBinding.inflate(inflater, parent, false)
        return AppInfoViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: AppInfoViewHolder,
        position: Int,
    ) {
        val item = currentList[position]
        val binding = holder.binding

        binding.appIcon.load(PackageIcon(item.packageName)) {
            placeholder(android.R.drawable.sym_def_app_icon)
            error(android.R.drawable.sym_def_app_icon)
            fallback(android.R.drawable.sym_def_app_icon)
        }
        binding.appLoadingProgress.visibility = if (item.isLoading) View.VISIBLE else View.GONE
        binding.appIcon.alpha = if (item.isLoading) 0.5f else 1.0f

        binding.packageName.text = item.packageName
        binding.appName.text = item.appName
        bindAppInfo(binding.appInfo, item)
        binding.appInfo.visibility = if (item.infoText.isBlank()) View.GONE else View.VISIBLE
    }

    private fun bindAppInfo(
        appInfo: TextView,
        item: AppItemUiModel,
    ) {
        val infoUrl = item.infoUrl
        if (infoUrl == null) {
            appInfo.text = item.infoText
            appInfo.movementMethod = null
            appInfo.linksClickable = false
            appInfo.isClickable = false
            return
        }

        val linkText =
            SpannableString(item.infoText).apply {
                setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            onClickListener.onStoreUrlClick(infoUrl)
                        }
                    },
                    0,
                    item.infoText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        appInfo.text = linkText
        appInfo.movementMethod = LinkMovementMethod.getInstance()
        appInfo.linksClickable = true
        appInfo.isClickable = true
    }

    interface OnClickListener {
        fun onClick(position: Int)

        fun onStoreUrlClick(url: String)
    }

    inner class AppInfoViewHolder(
        val binding: SnippetListRowBinding,
    ) : RecyclerView.ViewHolder(binding.root),
        View.OnClickListener {
        init {
            binding.root.setOnClickListener(this)
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
