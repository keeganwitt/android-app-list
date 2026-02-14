package com.github.keeganwitt.applist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SummaryAdapter(
    private val buckets: List<Pair<String, Int>>,
) : RecyclerView.Adapter<SummaryAdapter.SummaryViewHolder>() {
    class SummaryViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val label: TextView = view.findViewById(R.id.summary_bucket_label)
        val count: TextView = view.findViewById(R.id.summary_bucket_count)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): SummaryViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_summary_bucket, parent, false)
        return SummaryViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: SummaryViewHolder,
        position: Int,
    ) {
        val (label, count) = buckets[position]
        holder.label.text = label
        holder.count.text = count.toString()
    }

    override fun getItemCount() = buckets.size
}
