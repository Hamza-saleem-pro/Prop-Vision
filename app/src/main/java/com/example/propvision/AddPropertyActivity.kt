package com.example.propvision

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class AddPropertyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Enable Edge-to-Edge
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_property)

        val rootLayout = findViewById<View>(R.id.addPropertyRoot)
        val header = findViewById<View>(R.id.header)
        val bottomAction = findViewById<View>(R.id.bottomAction)
        val nestedScrollView = findViewById<View>(R.id.nestedScrollView)

        // 2. Handle System Window Insets for truly edge-to-edge content
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Adjust header to sit below the status bar
            header.updatePadding(top = systemBars.top)
            
            // Adjust floating button container to sit above navigation bar
            bottomAction.updatePadding(bottom = systemBars.bottom)
            
            // Add extra padding to scroll content so the last elements appear above the floating button
            // 56dp (button) + 40dp (container padding) + system navigation bar
            val density = resources.displayMetrics.density
            val totalBottomPadding = (96 * density).toInt() + systemBars.bottom
            nestedScrollView.updatePadding(bottom = totalBottomPadding)

            insets
        }

        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            finish()
        }
    }
}
