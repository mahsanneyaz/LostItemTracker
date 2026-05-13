package com.srm.lostitemtracker

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: AuthViewModel
    private lateinit var db: FirebaseFirestore
    private lateinit var progress: ProgressBar
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        db        = FirebaseFirestore.getInstance()

        val etEmail          = findViewById<EditText>(R.id.etEmail)
        val etPassword       = findViewById<EditText>(R.id.etPassword)
        val tvRegister       = findViewById<TextView>(R.id.tvGoToRegister)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        progress             = findViewById(R.id.progressBar)
        btnLogin             = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Check fields are not empty
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check email format is valid
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check internet connection
            if (!isNetworkAvailable()) {
                Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            progress.visibility = View.VISIBLE
            btnLogin.isEnabled  = false
            viewModel.login(email, password)
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        viewModel.loginResult.observe(this) { result ->

            if (result.isSuccess) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid

                if (uid == null) {
                    progress.visibility = View.GONE
                    btnLogin.isEnabled  = true
                    Toast.makeText(this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show()
                    return@observe
                }

                // Save FCM token so notifications can reach this device
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    db.collection("users").document(uid).update("fcmToken", token)
                }

                // Check role to decide which screen to open
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        progress.visibility = View.GONE

                        // Check if admin has blocked this account
                        val blocked = doc.getBoolean("blocked") ?: false
                        if (blocked) {
                            btnLogin.isEnabled = true
                            FirebaseAuth.getInstance().signOut()
                            Toast.makeText(
                                this,
                                "Your account has been blocked by the administrator.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@addOnSuccessListener
                        }

                        val role = doc.getString("role") ?: "user"

                        if (role == "admin") {
                            Toast.makeText(this, "Welcome, Admin!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, AdminDashboardActivity::class.java))
                        } else {
                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, DashboardActivity::class.java))
                        }
                        finish()
                    }
                    .addOnFailureListener {
                        progress.visibility = View.GONE
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    }

            } else {
                progress.visibility = View.GONE
                btnLogin.isEnabled  = true
                val errorMessage = result.exceptionOrNull()?.message ?: "Login failed"
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Returns true if the device has an active internet connection
    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val cap = cm.getNetworkCapabilities(network) ?: return false
            cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }
}