package com.github.keeganwitt.applist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SummaryAdapter(private val summaryItems: List<SummaryItem>) :
    RecyclerView.Adapter<SummaryAdapter.SummaryViewHolder>() {

    class SummaryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.summary_title)
        val container: LinearLayout = view.findViewById(R.id.summary_details_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_summary_section, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        val item = summaryItems[position]
        holder.title.text = holder.itemView.context.getString(item.field.titleResId)

        holder.container.removeAllViews()

        item.buckets.forEach { (label, count) ->
            val row = LinearLayout(holder.itemView.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 4, 0, 4)
            }

            val labelView = TextView(holder.itemView.context).apply {
                text = label
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val countView = TextView(holder.itemView.context).apply {
                text = count.toString()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            row.addView(labelView)
            row.addView(countView)
            holder.container.addView(row)
        }
    }

    override fun getItemCount() = summaryItems.size
}
