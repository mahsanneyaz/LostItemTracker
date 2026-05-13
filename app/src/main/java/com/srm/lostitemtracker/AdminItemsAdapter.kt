package com.srm.lostitemtracker

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

// ── Data class — holds all item info needed for the detail dialog ─────────────

data class AdminItem(
    val id          : String,
    val itemName    : String,
    val category    : String,
    val location    : String,
    val date        : String,
    val description : String,
    val status      : String,
    val photoUrl    : String,
    val userId      : String,
    val imageLabels : List<String>,
    val isLost      : Boolean
)

// ── Adapter ───────────────────────────────────────────────────────────────────

class AdminItemsAdapter(
    private val onItemClick: (AdminItem) -> Unit
) : ListAdapter<AdminItem, AdminItemsAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AdminItem>() {
            override fun areItemsTheSame(a: AdminItem, b: AdminItem) = a.id == b.id
            override fun areContentsTheSame(a: AdminItem, b: AdminItem) = a == b
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvItemName  : TextView = view.findViewById(R.id.tvAdminItemName)
        val tvCategory  : TextView = view.findViewById(R.id.tvAdminCategory)
        val tvLocation  : TextView = view.findViewById(R.id.tvAdminLocation)
        val tvDate      : TextView = view.findViewById(R.id.tvAdminDate)
        val tvStatus    : TextView = view.findViewById(R.id.tvAdminStatus)
        val tvTypeBadge : TextView = view.findViewById(R.id.tvAdminTypeBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)

        holder.tvItemName.text = item.itemName
        holder.tvCategory.text = "Category: ${item.category}"
        holder.tvLocation.text = "Location: ${item.location}"
        holder.tvDate.text     = "Date: ${item.date}"

        // LOST / FOUND badge
        if (item.isLost) {
            holder.tvTypeBadge.text = "LOST"
            holder.tvTypeBadge.setBackgroundColor(Color.parseColor("#FFEBEE"))
            holder.tvTypeBadge.setTextColor(Color.parseColor("#D32F2F"))
        } else {
            holder.tvTypeBadge.text = "FOUND"
            holder.tvTypeBadge.setBackgroundColor(Color.parseColor("#E8F5E9"))
            holder.tvTypeBadge.setTextColor(Color.parseColor("#2E7D32"))
        }

        // Status badge
        when (item.status) {
            "open"     -> {
                holder.tvStatus.text = "Open"
                holder.tvStatus.setBackgroundColor(Color.parseColor("#E3F2FD"))
                holder.tvStatus.setTextColor(Color.parseColor("#1565C0"))
            }
            "matched"  -> {
                holder.tvStatus.text = "Matched"
                holder.tvStatus.setBackgroundColor(Color.parseColor("#FFF3E0"))
                holder.tvStatus.setTextColor(Color.parseColor("#E65100"))
            }
            "returned" -> {
                holder.tvStatus.text = "Returned"
                holder.tvStatus.setBackgroundColor(Color.parseColor("#E8F5E9"))
                holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
            }
            else -> holder.tvStatus.text = item.status
        }

        // Whole card is tappable — opens detail dialog
        holder.itemView.setOnClickListener { onItemClick(item) }
    }
}