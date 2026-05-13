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
import android.widget.ImageView
import android.widget.EditText

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val etEmail   = findViewById<EditText>(R.id.etForgotEmail)
        val btnSend   = findViewById<Button>(R.id.btnSendReset)
        val tvBack    = findViewById<ImageView>(R.id.tvForgotBack)
        val progress  = findViewById<ProgressBar>(R.id.forgotProgress)

        // Back arrow — just close this screen and return to Login
        tvBack.setOnClickListener { finish() }

        btnSend.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading spinner and disable button while sending
            progress.visibility = View.VISIBLE
            btnSend.isEnabled   = false

            // Firebase sends a password reset email automatically
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    progress.visibility = View.GONE
                    Toast.makeText(
                        this,
                        "Reset email sent! Check your inbox.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish() // Go back to Login after sending
                }
                .addOnFailureListener { error ->
                    progress.visibility = View.GONE
                    btnSend.isEnabled   = true
                    Toast.makeText(
                        this,
                        "Failed: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}