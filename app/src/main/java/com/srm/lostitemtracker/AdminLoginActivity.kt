package com.srm.lostitemtracker

import android.content.Intent
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

class AdminLoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvBack: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        etEmail     = findViewById(R.id.etAdminEmail)
        etPassword  = findViewById(R.id.etAdminPassword)
        btnLogin    = findViewById(R.id.btnAdminLogin)
        progressBar = findViewById(R.id.adminLoginProgress)
        tvBack      = findViewById(R.id.tvAdminBack)

        btnLogin.setOnClickListener { attemptAdminLogin() }
        tvBack.setOnClickListener  { finish() }
    }

    private fun attemptAdminLogin() {
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: run {
                    setLoading(false)
                    showError("Authentication failed.")
                    return@addOnSuccessListener
                }

                // After login, check if this account has role = "admin" in Firestore
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        setLoading(false)
                        val role = doc.getString("role") ?: "user"
                        if (role == "admin") {
                            startActivity(Intent(this, AdminDashboardActivity::class.java))
                            finish()
                        } else {
                            auth.signOut()
                            showError("Access denied. This account is not an admin.")
                        }
                    }
                    .addOnFailureListener {
                        setLoading(false)
                        auth.signOut()
                        showError("Could not verify admin role. Please try again.")
                    }
            }
            .addOnFailureListener {
                setLoading(false)
                showError("Invalid email or password.")
            }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled     = !loading
        btnLogin.text          = if (loading) "Verifying..." else "Login as Admin"
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}