package com.srm.lostitemtracker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardViewModel : ViewModel() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _reports  = MutableLiveData<List<ReportItem>>()
    val reports: LiveData<List<ReportItem>> = _reports

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    data class ReportItem(
        val id       : String,
        val itemName : String,
        val category : String,
        val location : String,
        val photoUrl : String,
        val type     : String,
        val status   : String,  // needed for the status timeline
        val date     : String   // needed for the QR code
    )

    init {
        loadUserName()
        loadMyReports()
    }

    private fun loadUserName() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                _userName.value = doc.getString("name") ?: "User"
            }
    }

    fun loadMyReports() {
        val uid = auth.currentUser?.uid ?: return
        val allReports = mutableListOf<ReportItem>()
        var doneCount  = 0

        db.collection("lost_items")
            .whereEqualTo("userId", uid).get()
            .addOnSuccessListener { snap ->
                for (doc in snap.documents) {
                    val item = doc.toObject(LostItem::class.java) ?: continue
                    allReports.add(ReportItem(
                        id       = doc.id,
                        itemName = item.itemName,
                        category = item.category,
                        location = item.location,
                        photoUrl = item.photoUrl,
                        type     = "LOST",
                        status   = item.status,
                        date     = item.date
                    ))
                }
                doneCount++
                if (doneCount == 2) _reports.value = allReports
            }

        db.collection("found_items")
            .whereEqualTo("userId", uid).get()
            .addOnSuccessListener { snap ->
                for (doc in snap.documents) {
                    val item = doc.toObject(FoundItem::class.java) ?: continue
                    allReports.add(ReportItem(
                        id       = doc.id,
                        itemName = item.itemName,
                        category = item.category,
                        location = item.location,
                        photoUrl = item.photoUrl,
                        type     = "FOUND",
                        status   = item.status,
                        date     = item.date
                    ))
                }
                doneCount++
                if (doneCount == 2) _reports.value = allReports
            }
    }
}