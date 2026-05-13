package com.srm.lostitemtracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var tvOpenCount      : TextView
    private lateinit var tvMatchedCount   : TextView
    private lateinit var tvReturnedCount  : TextView
    private lateinit var tvAdminLogout    : ImageView
    private lateinit var tabLayout        : TabLayout
    private lateinit var viewPager        : ViewPager2
    private lateinit var usersContainer   : android.widget.FrameLayout
    private lateinit var settingsContainer: android.widget.FrameLayout
    private lateinit var bottomNav        : BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        db   = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        tvOpenCount       = findViewById(R.id.tvOpenCount)
        tvMatchedCount    = findViewById(R.id.tvMatchedCount)
        tvReturnedCount   = findViewById(R.id.tvReturnedCount)
        tvAdminLogout     = findViewById(R.id.tvAdminLogout)
        tabLayout         = findViewById(R.id.adminTabLayout)
        viewPager         = findViewById(R.id.adminViewPager)
        usersContainer    = findViewById(R.id.adminUsersContainer)
        settingsContainer = findViewById(R.id.adminSettingsContainer)
        bottomNav         = findViewById(R.id.adminBottomNav)

        setupTabs()
        loadAnalytics()
        setupBottomNav()

        // The top-bar logout icon — same confirmation as Settings logout
        tvAdminLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout from Admin Portal?")
                .setPositiveButton("Logout") { _, _ ->
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupTabs() {
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment =
                if (position == 0) AdminItemsFragment.newInstance(isLost = true)
                else AdminItemsFragment.newInstance(isLost = false)
        }
        viewPager.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "LOST ITEMS" else "FOUND ITEMS"
        }.attach()
    }

    private fun setupBottomNav() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_items -> {
                    // Show items (tabs + viewpager), hide others
                    tabLayout.visibility         = View.VISIBLE
                    viewPager.visibility         = View.VISIBLE
                    usersContainer.visibility    = View.GONE
                    settingsContainer.visibility = View.GONE
                    true
                }

                R.id.nav_users -> {
                    // Show users list, hide others
                    tabLayout.visibility         = View.GONE
                    viewPager.visibility         = View.GONE
                    usersContainer.visibility    = View.VISIBLE
                    settingsContainer.visibility = View.GONE
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.adminUsersContainer, AdminUsersFragment())
                        .commit()
                    true
                }

                R.id.nav_settings -> {
                    // Show settings (rerun + logout), hide others
                    tabLayout.visibility         = View.GONE
                    viewPager.visibility         = View.GONE
                    usersContainer.visibility    = View.GONE
                    settingsContainer.visibility = View.VISIBLE
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.adminSettingsContainer, AdminSettingsFragment())
                        .commit()
                    true
                }

                else -> false
            }
        }
    }

    private fun loadAnalytics() {
        var openCount    = 0
        var matchedCount = 0
        var returnedCount = 0
        var pending      = 2

        for (col in listOf("lost_items", "found_items")) {
            db.collection(col).get()
                .addOnSuccessListener { snap ->
                    for (doc in snap.documents) {
                        when (doc.getString("status")) {
                            "open"     -> openCount++
                            "matched"  -> matchedCount++
                            "returned" -> returnedCount++
                        }
                    }
                    pending--
                    if (pending == 0) updateAnalyticsUI(openCount, matchedCount, returnedCount)
                }
                .addOnFailureListener {
                    pending--
                    if (pending == 0) updateAnalyticsUI(openCount, matchedCount, returnedCount)
                }
        }
    }

    private fun updateAnalyticsUI(open: Int, matched: Int, returned: Int) {
        tvOpenCount.text    = open.toString()
        tvMatchedCount.text = matched.toString()
        tvReturnedCount.text = returned.toString()
    }

    // Called by fragments after any action that changes item status
    fun refreshAnalytics() {
        tvOpenCount.text    = "…"
        tvMatchedCount.text = "…"
        tvReturnedCount.text = "…"
        loadAnalytics()
    }
}