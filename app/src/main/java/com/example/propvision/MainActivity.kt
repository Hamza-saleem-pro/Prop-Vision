package com.example.propvision

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Show the beautiful splash screen (activity_main) first.
        // The user clicks "let's start" to move to the login (SplashActivity).
        findViewById<Button>(R.id.startBtn).setOnClickListener {
            val intent = Intent(this, SplashActivity::class.java)
            startActivity(intent)
            // finish() // We can finish this so they don't come back to the splash after starting
        }
    }
}
