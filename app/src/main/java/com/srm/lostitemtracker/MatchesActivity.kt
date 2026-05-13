package com.srm.lostitemtracker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MatchesActivity : AppCompatActivity() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: MatchesAdapter
    private val matchList = mutableListOf<MatchDisplayItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matches)

        val tvBack    = findViewById<TextView>(R.id.tvBack)
        val rvMatches = findViewById<RecyclerView>(R.id.rvMatches)
        val tvNone    = findViewById<TextView>(R.id.tvNoMatches)

        tvBack.setOnClickListener { finish() }

        val uid = auth.currentUser?.uid ?: return

        adapter = MatchesAdapter(
            items     = matchList,
            onContact = { item -> showContactPopup(item) },
            onConfirm = { item -> confirmMatch(item, rvMatches, tvNone, uid) },
            onReject  = { item -> rejectMatch(item, rvMatches, tvNone, uid) }
        )
        rvMatches.layoutManager = LinearLayoutManager(this)
        rvMatches.adapter = adapter

        loadMatches(uid, rvMatches, tvNone)
    }

    private fun loadMatches(uid: String, rv: RecyclerView, tvNone: TextView) {
        db.collection("matches").whereEqualTo("lostUserId", uid).get()
            .addOnSuccessListener { lostSnap ->
                db.collection("matches").whereEqualTo("foundUserId", uid).get()
                    .addOnSuccessListener { foundSnap ->

                        val allDocs = (lostSnap.documents + foundSnap.documents)
                            .distinctBy { it.id }

                        if (allDocs.isEmpty()) {
                            rv.visibility    = View.GONE
                            tvNone.visibility = View.VISIBLE
                            return@addOnSuccessListener
                        }

                        matchList.clear()
                        var loaded = 0

                        for (doc in allDocs) {
                            val match = doc.toObject(Match::class.java) ?: continue

                            db.collection("lost_items").document(match.lostItemId).get()
                                .addOnSuccessListener { lostDoc ->
                                    val lostName = lostDoc.getString("itemName") ?: "Unknown"
                                    db.collection("found_items").document(match.foundItemId).get()
                                        .addOnSuccessListener { foundDoc ->
                                            val foundName = foundDoc.getString("itemName") ?: "Unknown"
                                            val otherUserId = if (match.lostUserId == uid)
                                                match.foundUserId else match.lostUserId
                                            db.collection("users").document(otherUserId).get()
                                                .addOnSuccessListener { userDoc ->
                                                    matchList.add(MatchDisplayItem(
                                                        matchId        = match.id,
                                                        lostItemId     = match.lostItemId,
                                                        foundItemId    = match.foundItemId,
                                                        lostName       = lostName,
                                                        foundName      = foundName,
                                                        score          = match.score,
                                                        matchStatus    = match.status,
                                                        otherUserName  = userDoc.getString("name")  ?: "Unknown",
                                                        otherUserEmail = userDoc.getString("email") ?: "",
                                                        otherUserPhone = userDoc.getString("phone") ?: ""
                                                    ))
                                                    loaded++
                                                    if (loaded == allDocs.size) {
                                                        rv.visibility    = View.VISIBLE
                                                        tvNone.visibility = View.GONE
                                                        adapter.notifyDataSetChanged()
                                                    }
                                                }
                                        }
                                }
                        }
                    }
            }
    }

    private fun showContactPopup(item: MatchDisplayItem) {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_contact, null)

        val tvName      = dialogView.findViewById<TextView>(R.id.tvContactName)
        val tvEmail     = dialogView.findViewById<TextView>(R.id.tvContactEmail)
        val tvPhone     = dialogView.findViewById<TextView>(R.id.tvContactPhone)
        val ivCopyName  = dialogView.findViewById<ImageView>(R.id.ivCopyName)
        val ivCopyEmail = dialogView.findViewById<ImageView>(R.id.ivCopyEmail)
        val ivCopyPhone = dialogView.findViewById<ImageView>(R.id.ivCopyPhone)
        val btnEmail    = dialogView.findViewById<Button>(R.id.btnOpenEmailApp)
        val btnClose    = dialogView.findViewById<Button>(R.id.btnCloseContact)

        val phone = if (item.otherUserPhone.isNotEmpty()) item.otherUserPhone else "Not provided"
        tvName.text  = item.otherUserName
        tvEmail.text = item.otherUserEmail
        tvPhone.text = phone

        fun copyToClipboard(label: String, text: String) {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText(label, text))
            Toast.makeText(this, "$label copied!", Toast.LENGTH_SHORT).show()
        }

        ivCopyName.setOnClickListener  { copyToClipboard("Name",  item.otherUserName) }
        ivCopyEmail.setOnClickListener { copyToClipboard("Email", item.otherUserEmail) }
        ivCopyPhone.setOnClickListener { copyToClipboard("Phone", phone) }

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()

        // FIX: using ACTION_SEND with message/rfc822 type shows all email
        // apps (Gmail, Outlook, etc.) via the system chooser
        // The old approach using ACTION_SENDTO with mailto: failed on some
        // devices that had no default email handler registered
        btnEmail.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL,   arrayOf(item.otherUserEmail))
                putExtra(Intent.EXTRA_SUBJECT, "Lost Item Tracker — Match Found")
                putExtra(Intent.EXTRA_TEXT,
                    "Hi ${item.otherUserName},\n\nI think we have a match on " +
                            "Lost Item Tracker.\n\nLost item: ${item.lostName}\n" +
                            "Found item: ${item.foundName}\n\n" +
                            "Please reply to arrange the return.")
            }
            startActivity(Intent.createChooser(emailIntent, "Choose email app"))
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun confirmMatch(
        item: MatchDisplayItem, rv: RecyclerView, tvNone: TextView, uid: String
    ) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Match")
            .setMessage("Are you sure this is your item? Both items will be marked as Returned.")
            .setPositiveButton("Confirm") { _, _ ->
                db.collection("matches").document(item.matchId)
                    .update("status", "confirmed")
                    .addOnSuccessListener {
                        db.collection("lost_items").document(item.lostItemId)
                            .update("status", "returned")
                        db.collection("found_items").document(item.foundItemId)
                            .update("status", "returned")
                        Toast.makeText(
                            this, "Match confirmed! Items marked as returned.",
                            Toast.LENGTH_SHORT
                        ).show()
                        offerReceipt(item)
                        loadMatches(uid, rv, tvNone)
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this, "Failed to confirm. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun offerReceipt(item: MatchDisplayItem) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { myDoc ->
                val myName  = myDoc.getString("name")  ?: "Unknown"
                val myEmail = myDoc.getString("email") ?: ""

                AlertDialog.Builder(this)
                    .setTitle("Generate Receipt?")
                    .setMessage("Would you like to generate a handover receipt for this match?")
                    .setPositiveButton("Yes, Generate") { _, _ ->
                        PdfReceiptHelper.generateAndShare(
                            context       = this,
                            matchId       = item.matchId,
                            lostItemName  = item.lostName,
                            foundItemName = item.foundName,
                            lostUserName  = myName,
                            lostEmail     = myEmail,
                            foundUserName = item.otherUserName,
                            foundEmail    = item.otherUserEmail,
                            score         = item.score
                        )
                    }
                    .setNegativeButton("No Thanks", null)
                    .show()
            }
    }

    private fun rejectMatch(
        item: MatchDisplayItem, rv: RecyclerView, tvNone: TextView, uid: String
    ) {
        AlertDialog.Builder(this)
            .setTitle("Reject Match")
            .setMessage("Are you sure this is not your item?")
            .setPositiveButton("Reject") { _, _ ->
                db.collection("matches").document(item.matchId)
                    .update("status", "rejected")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Match rejected.", Toast.LENGTH_SHORT).show()
                        loadMatches(uid, rv, tvNone)
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this, "Failed to reject. Try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    data class MatchDisplayItem(
        val matchId        : String,
        val lostItemId     : String,
        val foundItemId    : String,
        val lostName       : String,
        val foundName      : String,
        val score          : Int,
        val matchStatus    : String,
        val otherUserName  : String,
        val otherUserEmail : String,
        val otherUserPhone : String
    )
}

class MatchesAdapter(
    private val items     : List<MatchesActivity.MatchDisplayItem>,
    private val onContact : (MatchesActivity.MatchDisplayItem) -> Unit,
    private val onConfirm : (MatchesActivity.MatchDisplayItem) -> Unit,
    private val onReject  : (MatchesActivity.MatchDisplayItem) -> Unit
) : RecyclerView.Adapter<MatchesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvScore       : TextView     = view.findViewById(R.id.tvScore)
        val tvLostName    : TextView     = view.findViewById(R.id.tvLostItemName)
        val tvFoundName   : TextView     = view.findViewById(R.id.tvFoundItemName)
        val btnContact    : Button       = view.findViewById(R.id.btnContact)
        val llMatchActions: LinearLayout = view.findViewById(R.id.llMatchActions)
        val btnConfirm    : Button       = view.findViewById(R.id.btnConfirmMatch)
        val btnReject     : Button       = view.findViewById(R.id.btnRejectMatch)
        val tvMatchStatus : TextView     = view.findViewById(R.id.tvMatchStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvLostName.text  = item.lostName
        holder.tvFoundName.text = item.foundName
        holder.tvScore.text     = "${item.score}% Match"
        holder.tvScore.setBackgroundColor(
            if (item.score >= 70) 0xFF006a35.toInt() else 0xFFFF9800.toInt()
        )

        when (item.matchStatus) {
            "pending" -> {
                holder.llMatchActions.visibility = View.VISIBLE
                holder.tvMatchStatus.visibility  = View.GONE
            }
            "confirmed" -> {
                holder.llMatchActions.visibility = View.GONE
                holder.tvMatchStatus.visibility  = View.VISIBLE
                holder.tvMatchStatus.text        = "✓ Confirmed — Item Returned"
                holder.tvMatchStatus.setTextColor(0xFF006a35.toInt())
            }
            "rejected" -> {
                holder.llMatchActions.visibility = View.GONE
                holder.tvMatchStatus.visibility  = View.VISIBLE
                holder.tvMatchStatus.text        = "✗ Match Rejected"
                holder.tvMatchStatus.setTextColor(0xFFba1a1a.toInt())
            }
        }

        holder.btnContact.setOnClickListener { onContact(item) }
        holder.btnConfirm.setOnClickListener { onConfirm(item) }
        holder.btnReject.setOnClickListener  { onReject(item) }
    }

    override fun getItemCount() = items.size
}