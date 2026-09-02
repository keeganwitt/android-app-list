package com.github.keeganwitt.applist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.github.keeganwitt.applist.databinding.ItemExportAppBinding
import com.github.keeganwitt.applist.utils.PackageIcon

internal class ExportAppAdapter(
    private val onSelectionChanged: (String) -> Unit,
) : ListAdapter<ExportAppItemUiModel, ExportAppAdapter.ExportAppViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ExportAppViewHolder =
        ExportAppViewHolder(
            ItemExportAppBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )

    override fun onBindViewHolder(
        holder: ExportAppViewHolder,
        position: Int,
    ) {
        val item = getItem(position)
        val binding = holder.binding
        val context = binding.root.context
        val type =
            context.getString(
                if (item.isUserInstalled) R.string.export_user_app else R.string.export_system_app,
            )
        binding.appName.text = item.appName
        binding.packageName.text = item.packageName
        binding.appClassification.text =
            if (item.isArchived) {
                context.getString(R.string.export_app_classification, type, context.getString(R.string.archived))
            } else {
                type
            }
        binding.appIcon.load(PackageIcon(item.packageName)) {
            placeholder(android.R.drawable.sym_def_app_icon)
            error(android.R.drawable.sym_def_app_icon)
            fallback(android.R.drawable.sym_def_app_icon)
        }

        val selectionDescription =
            context.getString(
                if (item.isSelected) R.string.export_deselect_app else R.string.export_select_app,
                item.appName,
            )
        binding.appSelectionCheckbox.setOnClickListener(null)
        binding.appSelectionCheckbox.isChecked = item.isSelected
        binding.appSelectionCheckbox.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        binding.appSelectionCheckbox.isFocusable = false
        val accessibilityDescription =
            listOf(
                item.appName,
                item.packageName,
                type,
                if (item.isArchived) context.getString(R.string.archived) else null,
                selectionDescription,
            ).filterNotNull().joinToString(", ")
        binding.root.contentDescription = accessibilityDescription
        binding.root.isActivated = item.isSelected
        ViewCompat.setAccessibilityDelegate(
            binding.root,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat,
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.contentDescription = accessibilityDescription
                    info.isCheckable = true
                    info.isChecked = item.isSelected
                    info.addAction(
                        AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            AccessibilityNodeInfo.ACTION_CLICK,
                            selectionDescription,
                        ),
                    )
                }
            },
        )
        binding.root.setOnClickListener { onSelectionChanged(item.packageName) }
        binding.appSelectionCheckbox.setOnClickListener { onSelectionChanged(item.packageName) }
    }

    class ExportAppViewHolder(
        val binding: ItemExportAppBinding,
    ) : RecyclerView.ViewHolder(binding.root)

    private class DiffCallback : DiffUtil.ItemCallback<ExportAppItemUiModel>() {
        override fun areItemsTheSame(
            oldItem: ExportAppItemUiModel,
            newItem: ExportAppItemUiModel,
        ): Boolean = oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(
            oldItem: ExportAppItemUiModel,
            newItem: ExportAppItemUiModel,
        ): Boolean = oldItem == newItem
    }
}
