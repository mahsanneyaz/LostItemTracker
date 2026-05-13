package com.srm.lostitemtracker

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val ivLogo        = findViewById<ImageView>(R.id.ivSplashLogo)
        val tvTitle       = findViewById<TextView>(R.id.tvSplashTitle)
        val tvUniversity  = findViewById<TextView>(R.id.tvSplashUniversity)

        // Load animations
        val zoomIn   = AnimationUtils.loadAnimation(this, R.anim.zoom_in)
        val fadeInUp = AnimationUtils.loadAnimation(this, R.anim.fade_in_up)

        // Start animations
        ivLogo.startAnimation(zoomIn)
        tvTitle.startAnimation(fadeInUp)
        tvUniversity.startAnimation(fadeInUp)

        // After 2 seconds navigate to the correct screen
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, 2000)
    }

    private fun navigateToNextScreen() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // User is already logged in — check role and route accordingly
        FirebaseFirestore.getInstance()
            .collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role") ?: "user"
                if (role == "admin") {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                } else {
                    startActivity(Intent(this, DashboardActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener {
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
    }
}