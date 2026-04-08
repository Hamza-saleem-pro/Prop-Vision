package com.example.propvision

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class ExplorePropertiesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_explore_properties)

        findViewById<View>(R.id.backBtn).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.filterBtn).setOnClickListener {
            Toast.makeText(this, "Filter options coming soon", Toast.LENGTH_SHORT).show()
        }

        val gridViewBtn = findViewById<ImageView>(R.id.gridViewBtn)
        val listViewBtn = findViewById<ImageView>(R.id.listViewBtn)
        val listViewLayout = findViewById<View>(R.id.listViewLayout)
        val gridViewLayout = findViewById<View>(R.id.gridViewLayout)

        gridViewBtn.setOnClickListener {
            listViewLayout.visibility = View.GONE
            gridViewLayout.visibility = View.VISIBLE
            gridViewBtn.setBackgroundResource(R.drawable.social_button_bg) // Simplified toggle
            listViewBtn.background = null
        }

        listViewBtn.setOnClickListener {
            listViewLayout.visibility = View.VISIBLE
            gridViewLayout.visibility = View.GONE
            listViewBtn.setBackgroundResource(R.drawable.social_button_bg)
            gridViewBtn.background = null
        }
    }
}
