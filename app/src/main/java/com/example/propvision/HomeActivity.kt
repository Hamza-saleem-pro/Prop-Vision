package com.example.propvision

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        val rootLayout = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        // Setup Search icon navigation
        findViewById<ImageView>(R.id.nav_explore).setOnClickListener {
            val intent = Intent(this, ExplorePropertiesActivity::class.java)
            startActivity(intent)
        }

        // Setup Add Property navigation
        findViewById<FrameLayout>(R.id.nav_add).setOnClickListener {
            val intent = Intent(this, AddPropertyActivity::class.java)
            startActivity(intent)
        }

        // Setup Profile navigation
        findViewById<ImageView>(R.id.nav_profile).setOnClickListener {
            val intent = Intent(this, CreateProfileActivity::class.java)
            startActivity(intent)
        }
    }
}
