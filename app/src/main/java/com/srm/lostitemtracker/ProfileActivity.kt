package com.srm.lostitemtracker

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.EditText
import android.widget.ImageView
import android.content.Intent

class ProfileActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        db = FirebaseFirestore.getInstance()

        val tvBack    = findViewById<ImageView>(R.id.tvProfileBack)
        val tvEmail   = findViewById<TextView>(R.id.tvProfileEmail)
        val etName    = findViewById<EditText>(R.id.etProfileName)
        val etPhone   = findViewById<EditText>(R.id.etProfilePhone)
        val btnSave   = findViewById<Button>(R.id.btnSaveProfile)
        val btnLogout        = findViewById<Button>(R.id.btnProfileLogout)
        val btnDeleteAccount = findViewById<Button>(R.id.btnProfileDeleteAccount)
        val progress  = findViewById<ProgressBar>(R.id.profileProgress)

        // Back arrow — close this screen
        tvBack.setOnClickListener { finish() }

        // Get the currently logged-in user's ID
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load the user's current data from Firestore
        progress.visibility = View.VISIBLE
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                progress.visibility = View.GONE

                // Fill in the fields with current values
                tvEmail.text         = doc.getString("email") ?: ""
                etName.setText(doc.getString("name") ?: "")
                etPhone.setText(doc.getString("phone") ?: "")
            }
            .addOnFailureListener {
                progress.visibility = View.GONE
                Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show()
            }

        // Save button — save name and phone back to Firestore
        btnSave.setOnClickListener {
            val newName  = etName.text.toString().trim()
            val newPhone = etPhone.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progress.visibility = View.VISIBLE
            btnSave.isEnabled   = false

            // Update only the name and phone fields — do not touch other fields
            val updates = mapOf(
                "name"  to newName,
                "phone" to newPhone
            )

            db.collection("users").document(uid).update(updates)
                .addOnSuccessListener {
                    progress.visibility = View.GONE
                    btnSave.isEnabled   = true
                    Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    progress.visibility = View.GONE
                    btnSave.isEnabled   = true
                    Toast.makeText(this, "Failed to save: ${error.message}", Toast.LENGTH_LONG).show()
                }
        }
        btnLogout.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnDeleteAccount.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("This will permanently delete your account. Are you sure?")
                .setPositiveButton("Delete Account") { _, _ ->
                    val user = FirebaseAuth.getInstance().currentUser
                    user?.delete()
                        ?.addOnSuccessListener {
                            Toast.makeText(this, "Account deleted.", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        }
                        ?.addOnFailureListener {
                            Toast.makeText(this,
                                "Please log out and log in again before deleting.",
                                Toast.LENGTH_LONG).show()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}