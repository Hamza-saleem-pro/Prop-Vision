package com.example.propvision

import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ExplorePropertiesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_explore_properties)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        val backBtn = findViewById<ImageView>(R.id.backBtn)
        val gridViewBtn = findViewById<ImageView>(R.id.gridViewBtn)
        val listViewBtn = findViewById<ImageView>(R.id.listViewBtn)
        val listView = findViewById<LinearLayout>(R.id.listViewLayout)
        val gridView = findViewById<GridLayout>(R.id.gridViewLayout)

        backBtn.setOnClickListener {
            finish()
        }

        gridViewBtn.setOnClickListener {
            // Switch to Grid View
            listView.visibility = View.GONE
            gridView.visibility = View.VISIBLE
            
            // Update UI for buttons
            gridViewBtn.setBackgroundResource(R.drawable.social_button_bg)
            gridViewBtn.backgroundTintList = getColorStateList(R.color.selected_bg) 
            gridViewBtn.setColorFilter(getColor(android.R.color.white))

            listViewBtn.setBackgroundResource(0)
            listViewBtn.setColorFilter(getColor(R.color.unselected_tint))
        }

        listViewBtn.setOnClickListener {
            // Switch to List View
            gridView.visibility = View.GONE
            listView.visibility = View.VISIBLE
            
            // Update UI for buttons
            listViewBtn.setBackgroundResource(R.drawable.social_button_bg)
            listViewBtn.backgroundTintList = getColorStateList(R.color.selected_bg)
            listViewBtn.setColorFilter(getColor(android.R.color.white))

            gridViewBtn.setBackgroundResource(0)
            gridViewBtn.setColorFilter(getColor(R.color.unselected_tint))
        }
    }
}
