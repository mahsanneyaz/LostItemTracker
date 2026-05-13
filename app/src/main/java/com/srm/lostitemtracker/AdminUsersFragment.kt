package com.srm.lostitemtracker

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

data class UserItem(
    val uid      : String,
    val name     : String,
    val email    : String,
    val phone    : String,
    val role     : String,
    val createdAt: String,
    val blocked  : Boolean
)

class AdminUsersFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var rv      : RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty : TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_admin_users, container, false)
        rv       = root.findViewById(R.id.rvUsers)
        progress = root.findViewById(R.id.usersProgress)
        tvEmpty  = root.findViewById(R.id.tvUsersEmpty)
        loadUsers()
        return root
    }

    private fun loadUsers() {
        progress.visibility = View.VISIBLE
        tvEmpty.visibility  = View.GONE

        db.collection("users").get()
            .addOnSuccessListener { snap ->
                progress.visibility = View.GONE
                if (snap.isEmpty) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text       = "No users found."
                    return@addOnSuccessListener
                }

                val users = snap.documents.mapNotNull { doc ->
                    val ts = doc.getTimestamp("createdAt")
                    val dateStr = if (ts != null)
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(ts.toDate())
                    else "Not available"

                    UserItem(
                        uid       = doc.id,
                        name      = doc.getString("name")    ?: "—",
                        email     = doc.getString("email")   ?: "—",
                        phone     = doc.getString("phone")   ?: "",
                        role      = doc.getString("role")    ?: "user",
                        createdAt = dateStr,
                        blocked   = doc.getBoolean("blocked") ?: false
                    )
                }

                rv.layoutManager = LinearLayoutManager(requireContext())
                rv.adapter = UsersAdapter(users) { user -> showUserDetailDialog(user) }
            }
            .addOnFailureListener { e ->
                progress.visibility = View.GONE
                tvEmpty.visibility  = View.VISIBLE
                tvEmpty.text        = "Failed: ${e.message}"
            }
    }

    // ── User Detail Dialog ──────────────────────────────────────────────────────

    private fun showUserDetailDialog(user: UserItem) {
        val ctx = requireContext()

        // Load user's items first, then show the dialog
        val allItems = mutableListOf<AdminItem>()
        var loaded   = 0

        fun onBothLoaded() {
            val view   = LayoutInflater.from(ctx).inflate(R.layout.dialog_admin_user_detail, null)
            val dialog = AlertDialog.Builder(ctx).setView(view).create()

            // Fill user info
            view.findViewById<TextView>(R.id.duUserName).text = user.name
            view.findViewById<TextView>(R.id.duUserEmail).text     = "Email: ${user.email}"
            view.findViewById<TextView>(R.id.duUserPhone).text     =
                "Phone: ${if (user.phone.isNotEmpty()) user.phone else "Not provided"}"
            view.findViewById<TextView>(R.id.duUserRole).text      = "Role: ${user.role.uppercase()}"
            view.findViewById<TextView>(R.id.duUserCreatedAt).text = "Joined: ${user.createdAt}"

            val tvBlocked = view.findViewById<TextView>(R.id.duUserBlocked)
            if (user.blocked) {
                tvBlocked.text       = "⚠️ This account is BLOCKED"
                tvBlocked.visibility = View.VISIBLE
            } else {
                tvBlocked.visibility = View.GONE
            }

            // Populate items list dynamically
            val llItems = view.findViewById<LinearLayout>(R.id.llUserItemsList)
            if (allItems.isEmpty()) {
                val tv = TextView(ctx)
                tv.text      = "No items reported by this user."
                tv.textSize  = 13f
                tv.setTextColor(0xFF888888.toInt())
                llItems.addView(tv)
            } else {
                for (item in allItems) {
                    val tv = TextView(ctx)
                    tv.text    = "• ${item.itemName}  (${if (item.isLost) "LOST" else "FOUND"}) — ${item.status}"
                    tv.textSize = 13f
                    tv.setTextColor(0xFF191C1E.toInt())
                    tv.setTypeface(null, Typeface.NORMAL)
                    tv.setPadding(0, 10, 0, 10)
                    // Tapping opens the item detail dialog
                    tv.setOnClickListener { showItemDetailDialog(item) }
                    llItems.addView(tv)

                    // Thin divider between items
                    val divider = View(ctx)
                    divider.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    )
                    divider.setBackgroundColor(0xFFEEEEEE.toInt())
                    llItems.addView(divider)
                }
            }

            // Block / Unblock button
            val btnBlock = view.findViewById<Button>(R.id.duBtnBlockUser)
            btnBlock.text = if (user.blocked) "Unblock User" else "Block User"
            btnBlock.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (user.blocked) 0xFF006a35.toInt() else 0xFFba1a1a.toInt()
            )
            btnBlock.setOnClickListener {
                val newBlocked = !user.blocked
                db.collection("users").document(user.uid)
                    .update("blocked", newBlocked)
                    .addOnSuccessListener {
                        Toast.makeText(ctx,
                            if (newBlocked) "User blocked." else "User unblocked.",
                            Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadUsers()
                    }
                    .addOnFailureListener {
                        Toast.makeText(ctx, "Failed. Try again.", Toast.LENGTH_SHORT).show()
                    }
            }

            // Send Warning button
            val btnWarn = view.findViewById<Button>(R.id.duBtnSendWarning)
            btnWarn.setOnClickListener {
                val et = EditText(ctx)
                et.hint = "Type warning message here..."
                et.minLines = 3
                AlertDialog.Builder(ctx)
                    .setTitle("Send Warning to ${user.name}")
                    .setMessage("The user will see this message next time they open the app.")
                    .setView(et)
                    .setPositiveButton("Send Warning") { _, _ ->
                        val msg = et.text.toString().trim()
                        if (msg.isEmpty()) {
                            Toast.makeText(ctx, "Warning message cannot be empty.", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        db.collection("users").document(user.uid)
                            .update("warning", msg)
                            .addOnSuccessListener {
                                Toast.makeText(ctx,
                                    "Warning sent. User will see it when they open the app.",
                                    Toast.LENGTH_LONG).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(ctx, "Failed to send warning.", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            // Delete user button
            val btnDelete = view.findViewById<Button>(R.id.duBtnDeleteUser)
            btnDelete.setOnClickListener {
                AlertDialog.Builder(ctx)
                    .setTitle("Delete User Data")
                    .setMessage(
                        "This will delete ${user.name}'s data from Firestore and block their account.\n\n" +
                                "Their item reports will remain in the system.\n\n" +
                                "Note: Their Firebase login account still exists but login will be prevented."
                    )
                    .setPositiveButton("Delete") { _, _ ->
                        // Block first, then delete Firestore document
                        db.collection("users").document(user.uid)
                            .update("blocked", true)
                            .addOnSuccessListener {
                                db.collection("users").document(user.uid)
                                    .delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(ctx,
                                            "User data deleted and account blocked.",
                                            Toast.LENGTH_LONG).show()
                                        dialog.dismiss()
                                        loadUsers()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(ctx, "Failed to delete.", Toast.LENGTH_SHORT).show()
                                    }
                            }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            view.findViewById<Button>(R.id.duBtnClose).setOnClickListener { dialog.dismiss() }
            dialog.show()
        }

        // Fetch user's lost items
        db.collection("lost_items").whereEqualTo("userId", user.uid).get()
            .addOnSuccessListener { snap ->
                @Suppress("UNCHECKED_CAST")
                for (doc in snap.documents) {
                    allItems.add(AdminItem(
                        id          = doc.id,
                        itemName    = doc.getString("itemName")    ?: "—",
                        category    = doc.getString("category")    ?: "—",
                        location    = doc.getString("location")    ?: "—",
                        date        = doc.getString("date")        ?: "—",
                        description = doc.getString("description") ?: "—",
                        status      = doc.getString("status")      ?: "open",
                        photoUrl    = doc.getString("photoUrl")    ?: "",
                        userId      = user.uid,
                        imageLabels = (doc.get("imageLabels") as? List<String>) ?: emptyList(),
                        isLost      = true
                    ))
                }
                loaded++
                if (loaded == 2) onBothLoaded()
            }
            .addOnFailureListener { loaded++; if (loaded == 2) onBothLoaded() }

        // Fetch user's found items
        db.collection("found_items").whereEqualTo("userId", user.uid).get()
            .addOnSuccessListener { snap ->
                @Suppress("UNCHECKED_CAST")
                for (doc in snap.documents) {
                    allItems.add(AdminItem(
                        id          = doc.id,
                        itemName    = doc.getString("itemName")    ?: "—",
                        category    = doc.getString("category")    ?: "—",
                        location    = doc.getString("location")    ?: "—",
                        date        = doc.getString("date")        ?: "—",
                        description = doc.getString("description") ?: "—",
                        status      = doc.getString("status")      ?: "open",
                        photoUrl    = doc.getString("photoUrl")    ?: "",
                        userId      = user.uid,
                        imageLabels = (doc.get("imageLabels") as? List<String>) ?: emptyList(),
                        isLost      = false
                    ))
                }
                loaded++
                if (loaded == 2) onBothLoaded()
            }
            .addOnFailureListener { loaded++; if (loaded == 2) onBothLoaded() }
    }

    // ── Item Detail Dialog (same as in AdminItemsFragment) ─────────────────────

    private fun showItemDetailDialog(item: AdminItem) {
        val ctx    = requireContext()
        val view   = LayoutInflater.from(ctx).inflate(R.layout.dialog_admin_item_detail, null)
        val dialog = AlertDialog.Builder(ctx).setView(view).create()
        val collection = if (item.isLost) "lost_items" else "found_items"

        val ivPhoto     = view.findViewById<ImageView>(R.id.diItemPhoto)
        val tvName      = view.findViewById<TextView>(R.id.diItemName)
        val tvType      = view.findViewById<TextView>(R.id.diType)
        val tvStatus    = view.findViewById<TextView>(R.id.diStatus)
        val tvCat       = view.findViewById<TextView>(R.id.diCategory)
        val tvDesc      = view.findViewById<TextView>(R.id.diDescription)
        val tvLoc       = view.findViewById<TextView>(R.id.diLocation)
        val tvDate      = view.findViewById<TextView>(R.id.diDate)
        val tvLabels    = view.findViewById<TextView>(R.id.diImageLabels)
        val tvUserName  = view.findViewById<TextView>(R.id.diReportedByName)
        val tvUserEmail = view.findViewById<TextView>(R.id.diReportedByEmail)
        val tvUserPhone = view.findViewById<TextView>(R.id.diReportedByPhone)
        val btnReturned = view.findViewById<Button>(R.id.diBtnMarkReturned)
        val btnDelete   = view.findViewById<Button>(R.id.diBtnDelete)
        val btnClose    = view.findViewById<Button>(R.id.diBtnClose)

        tvName.text   = item.itemName
        tvType.text   = "Type: ${if (item.isLost) "LOST" else "FOUND"}"
        tvStatus.text = "Status: ${item.status}"
        tvCat.text    = "Category: ${item.category}"
        tvDesc.text   = "Description: ${item.description}"
        tvLoc.text    = "Location: ${item.location}"
        tvDate.text   = "Date: ${item.date}"
        tvLabels.text = if (item.imageLabels.isEmpty())
            "AI Labels: None"
        else
            "AI Labels: ${item.imageLabels.joinToString(", ")}"

        if (item.photoUrl.isNotEmpty()) {
            Glide.with(ctx).load(item.photoUrl).centerCrop().into(ivPhoto)
        }

        if (item.status == "returned") btnReturned.visibility = View.GONE

        tvUserName.text  = "Loading..."
        tvUserEmail.text = ""
        tvUserPhone.text = ""

        db.collection("users").document(item.userId).get()
            .addOnSuccessListener { userDoc ->
                tvUserName.text  = userDoc.getString("name")  ?: "Unknown"
                tvUserEmail.text = "Email: ${userDoc.getString("email") ?: "—"}"
                tvUserPhone.text = "Phone: ${userDoc.getString("phone") ?: "Not provided"}"
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
                            (activity as? AdminDashboardActivity)?.refreshAnalytics()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(ctx)
                .setTitle("Delete Report")
                .setMessage("Permanently delete this report?")
                .setPositiveButton("Delete") { _, _ ->
                    db.collection(collection).document(item.id)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(ctx, "Report deleted.", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
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

// ── Users Adapter ─────────────────────────────────────────────────────────────

class UsersAdapter(
    private val items  : List<UserItem>,
    private val onClick: (UserItem) -> Unit
) : RecyclerView.Adapter<UsersAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName : TextView = view.findViewById(R.id.tvUserName)
        val tvEmail: TextView = view.findViewById(R.id.tvUserEmail)
        val tvPhone: TextView = view.findViewById(R.id.tvUserPhone)
        val tvRole : TextView = view.findViewById(R.id.tvUserRole)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user_card, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val u = items[position]
        holder.tvName.text  = if (u.blocked) "⚠️ ${u.name}" else u.name
        holder.tvEmail.text = u.email
        holder.tvPhone.text = if (u.phone.isNotEmpty()) u.phone else "No phone"
        holder.tvRole.text  = u.role.uppercase()

        val roleColor = if (u.role == "admin") 0xFF006099.toInt() else 0xFF707882.toInt()
        holder.tvRole.setBackgroundColor(roleColor)
        holder.tvRole.setTextColor(0xFFFFFFFF.toInt())
        holder.tvRole.setPadding(12, 4, 12, 4)

        holder.itemView.setOnClickListener { onClick(u) }
    }
}