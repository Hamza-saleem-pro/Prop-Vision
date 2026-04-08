package com.example.propvision

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        val isAdmin = intent.getBooleanExtra("IS_ADMIN", false)
        if (isAdmin) {
            findViewById<TextView>(R.id.greeting).text = "Admin Dashboard"
            findViewById<TextView>(R.id.discoverTitle).text = "Manage All Properties"
            Toast.makeText(this, "Welcome to Admin Mode", Toast.LENGTH_LONG).show()
        }

        // Bottom Navigation linking
        findViewById<View>(R.id.nav_explore).setOnClickListener {
            startActivity(Intent(this, ExplorePropertiesActivity::class.java))
        }

        findViewById<View>(R.id.nav_add).setOnClickListener {
            startActivity(Intent(this, AddPropertyActivity::class.java))
        }

        findViewById<View>(R.id.nav_market).setOnClickListener {
            startActivity(Intent(this, MyAdsActivity::class.java))
        }

        findViewById<View>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, CreateProfileActivity::class.java))
        }

        // Search Bar Logic (Simple Toast for now)
        findViewById<View>(R.id.searchBar).setOnClickListener {
            Toast.makeText(this, "Search functionality coming soon", Toast.LENGTH_SHORT).show()
        }
    }
}
