package com.srm.lostitemtracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminSettingsFragment : Fragment() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_admin_settings, container, false)

        val btnRerun  = root.findViewById<Button>(R.id.settingsBtnRerun)
        val btnLogout = root.findViewById<Button>(R.id.settingsBtnLogout)

        // Re-run matching button
        btnRerun.setOnClickListener {
            btnRerun.isEnabled = false
            btnRerun.text      = "Scanning..."

            db.collection("lost_items").whereEqualTo("status", "open").get()
                .addOnSuccessListener { lostSnap ->
                    val lostItems = lostSnap.documents.mapNotNull {
                        @Suppress("UNCHECKED_CAST")
                        LostItem(
                            id          = it.id,
                            userId      = it.getString("userId")      ?: "",
                            itemName    = it.getString("itemName")    ?: "",
                            category    = it.getString("category")    ?: "",
                            description = it.getString("description") ?: "",
                            location    = it.getString("location")    ?: "",
                            date        = it.getString("date")        ?: "",
                            photoUrl    = it.getString("photoUrl")    ?: "",
                            status      = it.getString("status")      ?: "open",
                            createdAt   = it.getLong("createdAt")     ?: 0L,
                            imageLabels = (it.get("imageLabels") as? List<String>) ?: emptyList()
                        )
                    }

                    db.collection("found_items").whereEqualTo("status", "open").get()
                        .addOnSuccessListener { foundSnap ->
                            val foundItems = foundSnap.documents.mapNotNull {
                                @Suppress("UNCHECKED_CAST")
                                FoundItem(
                                    id          = it.id,
                                    userId      = it.getString("userId")      ?: "",
                                    itemName    = it.getString("itemName")    ?: "",
                                    category    = it.getString("category")    ?: "",
                                    description = it.getString("description") ?: "",
                                    location    = it.getString("location")    ?: "",
                                    date        = it.getString("date")        ?: "",
                                    photoUrl    = it.getString("photoUrl")    ?: "",
                                    status      = it.getString("status")      ?: "open",
                                    createdAt   = it.getLong("createdAt")     ?: 0L,
                                    imageLabels = (it.get("imageLabels") as? List<String>) ?: emptyList()
                                )
                            }

                            val totalPairs = lostItems.size * foundItems.size

                            if (totalPairs == 0) {
                                btnRerun.isEnabled = true
                                btnRerun.text      = "Re-run Matching on All Items"
                                Toast.makeText(requireContext(),
                                    "No open items to match.",
                                    Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }

                            var matchesCreated = 0
                            var processed      = 0

                            for (lost in lostItems) {
                                for (found in foundItems) {
                                    val score = MatchingEngine.score(lost, found)
                                    if (score >= 50) {
                                        val matchId = "${lost.id}_${found.id}"
                                        val match   = Match(
                                            id          = matchId,
                                            lostItemId  = lost.id,
                                            foundItemId = found.id,
                                            lostUserId  = lost.userId,
                                            foundUserId = found.userId,
                                            score       = score,
                                            status      = "pending",
                                            createdAt   = System.currentTimeMillis()
                                        )
                                        db.collection("matches").document(matchId).set(match)
                                            .addOnSuccessListener {
                                                db.collection("lost_items")
                                                    .document(lost.id).update("status", "matched")
                                                db.collection("found_items")
                                                    .document(found.id).update("status", "matched")
                                                matchesCreated++
                                            }
                                    }
                                    processed++
                                    if (processed == totalPairs) {
                                        btnRerun.isEnabled = true
                                        btnRerun.text      = "Re-run Matching on All Items"
                                        (activity as? AdminDashboardActivity)?.refreshAnalytics()
                                        Toast.makeText(requireContext(),
                                            "Done. $matchesCreated match(es) created.",
                                            Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                        .addOnFailureListener {
                            btnRerun.isEnabled = true
                            btnRerun.text      = "Re-run Matching on All Items"
                        }
                }
                .addOnFailureListener {
                    btnRerun.isEnabled = true
                    btnRerun.text      = "Re-run Matching on All Items"
                }
        }

        // Logout button
        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout from Admin Portal?")
                .setPositiveButton("Logout") { _, _ ->
                    auth.signOut()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    activity?.finishAffinity()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        return root
    }
}