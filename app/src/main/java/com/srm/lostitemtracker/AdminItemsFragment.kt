package com.srm.lostitemtracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminItemsFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: AdminItemsAdapter

    private var isLost: Boolean = true
    private val collection get() = if (isLost) "lost_items" else "found_items"

    companion object {
        fun newInstance(isLost: Boolean): AdminItemsFragment {
            val f = AdminItemsFragment()
            f.arguments = Bundle().apply { putBoolean("isLost", isLost) }
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isLost = arguments?.getBoolean("isLost", true) ?: true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val root     = inflater.inflate(R.layout.fragment_admin_items, container, false)
        val rv       = root.findViewById<RecyclerView>(R.id.adminItemsRecycler)
        val progress = root.findViewById<ProgressBar>(R.id.adminItemsProgress)
        val tvEmpty  = root.findViewById<TextView>(R.id.tvAdminEmpty)

        adapter = AdminItemsAdapter { item -> showItemDetailDialog(item) }
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        loadItems(progress, tvEmpty)
        return root
    }

    fun loadItems(
        progress: ProgressBar? = null,
        tvEmpty : TextView?    = null
    ) {
        progress?.visibility = View.VISIBLE

        db.collection(collection)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snap ->
                progress?.visibility = View.GONE
                @Suppress("UNCHECKED_CAST")
                val items = snap.documents.map { doc ->
                    AdminItem(
                        id          = doc.id,
                        itemName    = doc.getString("itemName")    ?: "—",
                        category    = doc.getString("category")    ?: "—",
                        location    = doc.getString("location")    ?: "—",
                        date        = doc.getString("date")        ?: "—",
                        description = doc.getString("description") ?: "—",
                        status      = doc.getString("status")      ?: "open",
                        photoUrl    = doc.getString("photoUrl")    ?: "",
                        userId      = doc.getString("userId")      ?: "—",
                        imageLabels = (doc.get("imageLabels") as? List<String>) ?: emptyList(),
                        isLost      = this.isLost
                    )
                }
                if (items.isEmpty()) {
                    tvEmpty?.visibility = View.VISIBLE
                    tvEmpty?.text = "No ${if (isLost) "lost" else "found"} reports yet."
                } else {
                    tvEmpty?.visibility = View.GONE
                    adapter.submitList(items)
                }
            }
            .addOnFailureListener {
                progress?.visibility = View.GONE
                tvEmpty?.visibility  = View.VISIBLE
                tvEmpty?.text        = "Failed to load items."
            }
    }

    // ── Item Detail Dialog ─────────────────────────────────────────────────────

    fun showItemDetailDialog(item: AdminItem) {
        val ctx      = requireContext()
        val view     = LayoutInflater.from(ctx).inflate(R.layout.dialog_admin_item_detail, null)
        val dialog   = AlertDialog.Builder(ctx).setView(view).create()

        val ivPhoto       = view.findViewById<ImageView>(R.id.diItemPhoto)
        val tvName        = view.findViewById<TextView>(R.id.diItemName)
        val tvType        = view.findViewById<TextView>(R.id.diType)
        val tvStatus      = view.findViewById<TextView>(R.id.diStatus)
        val tvCat         = view.findViewById<TextView>(R.id.diCategory)
        val tvDesc        = view.findViewById<TextView>(R.id.diDescription)
        val tvLoc         = view.findViewById<TextView>(R.id.diLocation)
        val tvDate        = view.findViewById<TextView>(R.id.diDate)
        val tvLabels      = view.findViewById<TextView>(R.id.diImageLabels)
        val tvUserName    = view.findViewById<TextView>(R.id.diReportedByName)
        val tvUserEmail   = view.findViewById<TextView>(R.id.diReportedByEmail)
        val tvUserPhone   = view.findViewById<TextView>(R.id.diReportedByPhone)
        val btnReturned   = view.findViewById<Button>(R.id.diBtnMarkReturned)
        val btnDelete     = view.findViewById<Button>(R.id.diBtnDelete)
        val btnClose      = view.findViewById<Button>(R.id.diBtnClose)

        // Fill item fields
        tvName.text   = item.itemName
        tvType.text   = "Type: ${if (item.isLost) "LOST" else "FOUND"}"
        tvStatus.text = "Status: ${item.status}"
        tvCat.text    = "Category: ${item.category}"
        tvDesc.text   = "Description: ${item.description}"
        tvLoc.text    = "Location: ${item.location}"
        tvDate.text   = "Date: ${item.date}"
        tvLabels.text = if (item.imageLabels.isEmpty())
            "AI Labels: None (no photo or photo not analysed)"
        else
            "AI Labels: ${item.imageLabels.joinToString(", ")}"

        if (item.photoUrl.isNotEmpty()) {
            Glide.with(ctx).load(item.photoUrl).centerCrop().into(ivPhoto)
        }

        // Hide Mark Returned if already returned
        if (item.status == "returned") btnReturned.visibility = View.GONE

        // Fetch user who reported this item
        tvUserName.text  = "Loading..."
        tvUserEmail.text = ""
        tvUserPhone.text = ""

        db.collection("users").document(item.userId).get()
            .addOnSuccessListener { userDoc ->
                tvUserName.text  = userDoc.getString("name")  ?: "Unknown"
                tvUserEmail.text = "Email: ${userDoc.getString("email") ?: "—"}"
                tvUserPhone.text = "Phone: ${userDoc.getString("phone") ?: "Not provided"}"
            }
            .addOnFailureListener {
                tvUserName.text = "User not found"
            }

        btnReturned.setOnClickListener {
            AlertDialog.Builder(ctx)
                .setTitle("Mark as Returned")
                .setMessage("Mark this item as returned?")
                .setPositiveButton("Yes") { _, _ ->
                    db.collection(collection).document(item.id)
                        .update("status", "returned")
                        .addOnSuccessListener {
                            Toast.makeText(ctx, "Marked as returned.", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            loadItems()
                            (activity as? AdminDashboardActivity)?.refreshAnalytics()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(ctx)
                .setTitle("Delete Report")
                .setMessage("Permanently delete this report? This cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    db.collection(collection).document(item.id)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(ctx, "Report deleted.", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            loadItems()
                            (activity as? AdminDashboardActivity)?.refreshAnalytics()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}