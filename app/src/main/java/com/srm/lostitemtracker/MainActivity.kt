package com.srm.lostitemtracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            // No one is logged in — go to Login screen
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Someone is logged in — check their role in Firestore
        // before deciding which screen to open
        val uid = currentUser.uid
        val db  = FirebaseFirestore.getInstance()

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "user"

                if (role == "admin") {
                    // Admin — go to Admin Dashboard
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                } else {
                    // Regular user — go to regular Dashboard
                    startActivity(Intent(this, DashboardActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener {
                // If Firestore fails for any reason, fall back to regular Dashboard
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
    }
}