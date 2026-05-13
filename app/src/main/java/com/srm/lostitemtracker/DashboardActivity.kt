package com.srm.lostitemtracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var viewModel: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        val tvWelcome     = findViewById<TextView>(R.id.tvWelcome)
        val ivSearchIcon  = findViewById<ImageView>(R.id.ivSearchIcon)
        val ivProfileIcon = findViewById<ImageView>(R.id.ivProfileIcon)
        val btnLost       = findViewById<Button>(R.id.btnReportLost)
        val btnFound      = findViewById<Button>(R.id.btnReportFound)
        val btnMatches    = findViewById<Button>(R.id.btnViewMatches)
        val rvReports     = findViewById<RecyclerView>(R.id.rvMyReports)
        val tvNoReports   = findViewById<TextView>(R.id.tvNoReports)

        val adapter = ReportsAdapter(emptyList()) { viewModel.loadMyReports() }
        rvReports.layoutManager = LinearLayoutManager(this)
        rvReports.adapter = adapter

        viewModel.userName.observe(this) { name ->
            tvWelcome.text = "Hello, $name!"
        }

        viewModel.reports.observe(this) { list ->
            if (list.isEmpty()) {
                rvReports.visibility   = View.GONE
                tvNoReports.visibility = View.VISIBLE
            } else {
                rvReports.visibility   = View.VISIBLE
                tvNoReports.visibility = View.GONE
                adapter.updateList(list)
            }
        }

        ivSearchIcon.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        ivProfileIcon.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnLost.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java).apply {
                putExtra("IS_LOST", true)
            })
        }

        btnFound.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java).apply {
                putExtra("IS_LOST", false)
            })
        }

        btnMatches.setOnClickListener {
            startActivity(Intent(this, MatchesActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadMyReports()
        checkForAdminWarning()
    }

    private fun checkForAdminWarning() {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db  = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val warning = doc.getString("warning") ?: ""
                if (warning.isNotEmpty()) {
                    android.app.AlertDialog.Builder(this)
                        .setTitle("⚠️ Notice from Admin")
                        .setMessage(warning)
                        .setPositiveButton("OK") { _, _ ->
                            // Clear warning after user has seen it
                            db.collection("users").document(uid).update("warning", "")
                        }
                        .setCancelable(false)
                        .show()
                }
            }
    }
}

// ── Adapter ───────────────────────────────────────────────────────────────────

class ReportsAdapter(
    private var items    : List<DashboardViewModel.ReportItem>,
    private val onRefresh: () -> Unit
) : RecyclerView.Adapter<ReportsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto   : ImageView = view.findViewById(R.id.ivItemPhoto)
        val tvName    : TextView  = view.findViewById(R.id.tvItemName)
        val tvCategory: TextView  = view.findViewById(R.id.tvCategory)
        val tvLocation: TextView  = view.findViewById(R.id.tvLocation)
        val tvType    : TextView  = view.findViewById(R.id.tvType)
        val vColorBar : View      = view.findViewById(R.id.vColorBar)
        val tvDot1    : TextView  = view.findViewById(R.id.tvDot1)
        val tvDot2    : TextView  = view.findViewById(R.id.tvDot2)
        val tvDot3    : TextView  = view.findViewById(R.id.tvDot3)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item          = items[position]
        val isLost        = item.type == "LOST"
        val themeColor    = if (isLost) 0xFFba1a1a.toInt() else 0xFF006a35.toInt()
        val badgeColor    = if (isLost) 0xFFF44336.toInt() else 0xFF4CAF50.toInt()
        val inactiveColor = 0xFFCCCCCC.toInt()

        holder.tvName.text     = item.itemName
        holder.tvCategory.text = item.category
        holder.tvLocation.text = item.location
        holder.tvType.text     = item.type

        holder.vColorBar.setBackgroundColor(themeColor)
        holder.tvType.setBackgroundColor(badgeColor)

        // Colour the timeline dots based on current status
        when (item.status) {
            "open" -> {
                holder.tvDot1.setTextColor(themeColor)
                holder.tvDot2.setTextColor(inactiveColor)
                holder.tvDot3.setTextColor(inactiveColor)
            }
            "matched" -> {
                holder.tvDot1.setTextColor(themeColor)
                holder.tvDot2.setTextColor(themeColor)
                holder.tvDot3.setTextColor(inactiveColor)
            }
            else -> {
                // returned or expired — all three dots active
                holder.tvDot1.setTextColor(themeColor)
                holder.tvDot2.setTextColor(themeColor)
                holder.tvDot3.setTextColor(themeColor)
            }
        }

        // Load photo if available
        if (item.photoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.photoUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivPhoto)
        } else {
            holder.ivPhoto.setImageDrawable(null)
            holder.ivPhoto.setBackgroundColor(0xFFE0E0E0.toInt())
        }

        // Tap card to open detail bottom sheet
        holder.itemView.setOnClickListener {
            val ctx        = holder.itemView.context
            val repo       = ItemRepository()
            val collection = if (isLost) "lost_items" else "found_items"
            val db         = com.google.firebase.firestore.FirebaseFirestore.getInstance()

            db.collection(collection).document(item.id).get()
                .addOnSuccessListener { doc ->
                    val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(ctx)
                    val view  = LayoutInflater.from(ctx)
                        .inflate(R.layout.bottom_sheet_report_detail, null)
                    sheet.setContentView(view)

                    view.findViewById<TextView>(R.id.bsTitle).text =
                        if (isLost) "Lost Item Details" else "Found Item Details"
                    view.findViewById<TextView>(R.id.bsItemName).text    = "Item: ${doc.getString("itemName")}"
                    view.findViewById<TextView>(R.id.bsCategory).text    = "Category: ${doc.getString("category")}"
                    view.findViewById<TextView>(R.id.bsDescription).text = "Description: ${doc.getString("description")}"
                    view.findViewById<TextView>(R.id.bsLocation).text    = "Location: ${doc.getString("location")}"
                    view.findViewById<TextView>(R.id.bsDate).text        = "Date: ${doc.getString("date")}"
                    view.findViewById<TextView>(R.id.bsStatus).text      = "Status: ${doc.getString("status")}"

                    val photoUrl = doc.getString("photoUrl") ?: ""
                    val bsPhoto  = view.findViewById<ImageView>(R.id.bsPhoto)
                    if (photoUrl.isNotEmpty()) {
                        Glide.with(ctx).load(photoUrl).into(bsPhoto)
                    }

                    // QR Code button
                    view.findViewById<Button>(R.id.bsBtnQR).setOnClickListener {
                        val uid = com.google.firebase.auth.FirebaseAuth
                            .getInstance().currentUser?.uid ?: ""
                        db.collection("users").document(uid).get()
                            .addOnSuccessListener { userDoc ->
                                val intent = android.content.Intent(ctx, QRCodeActivity::class.java).apply {
                                    putExtra("itemName",  doc.getString("itemName")  ?: "")
                                    putExtra("category",  doc.getString("category")  ?: "")
                                    putExtra("location",  doc.getString("location")  ?: "")
                                    putExtra("date",      doc.getString("date")      ?: "")
                                    putExtra("type",      item.type)
                                    putExtra("reportId",  item.id)
                                    putExtra("userEmail", userDoc.getString("email") ?: "")
                                }
                                sheet.dismiss()
                                ctx.startActivity(intent)
                            }
                    }

                    // Delete button
                    view.findViewById<Button>(R.id.bsBtnDelete).setOnClickListener {
                        android.app.AlertDialog.Builder(ctx)
                            .setTitle("Delete Report")
                            .setMessage("Are you sure you want to delete this report?")
                            .setPositiveButton("Delete") { _, _ ->
                                MainScope().launch {
                                    val result = if (isLost) repo.deleteLostItem(item.id)
                                    else        repo.deleteFoundItem(item.id)
                                    result.onSuccess {
                                        sheet.dismiss()
                                        onRefresh()
                                        android.widget.Toast.makeText(
                                            ctx, "Report deleted",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }

                    sheet.show()
                }
        }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<DashboardViewModel.ReportItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}