package com.example.propvision

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class CreateProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_profile)

        val rootLayout = findViewById<View>(R.id.createProfileRoot)
        val header = findViewById<View>(R.id.header)
        val bottomAction = findViewById<View>(R.id.bottomAction)
        val nestedScrollView = findViewById<View>(R.id.nestedScrollView)

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            header.updatePadding(top = systemBars.top)
            bottomAction.updatePadding(bottom = systemBars.bottom)
            
            val density = resources.displayMetrics.density
            val totalBottomPadding = (96 * density).toInt() + systemBars.bottom
            nestedScrollView.updatePadding(bottom = totalBottomPadding)

            insets
        }

        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.skipBtn).setOnClickListener {
            startHomeActivity()
        }

        findViewById<View>(R.id.continueBtn).setOnClickListener {
            // Logic to save profile details can be added here
            startHomeActivity()
        }
    }

    private fun startHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
