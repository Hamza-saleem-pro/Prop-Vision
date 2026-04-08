package com.example.propvision

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginBtn = findViewById<Button>(R.id.loginBtn)

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty()) {
                emailInput.error = "Email is required"
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Invalid email format"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordInput.error = "Password is required"
                return@setOnClickListener
            }

            // Simple Admin Login logic
            if (email == "admin@propvision.com" && password == "admin123") {
                Toast.makeText(this, "Admin Login Successful", Toast.LENGTH_SHORT).show()
                // Navigate to Admin Dashboard (Home Activity for now, but with admin flag)
                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("IS_ADMIN", true)
                startActivity(intent)
                finish()
            } else {
                // Regular User Login
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        }
        
        findViewById<android.view.View>(R.id.register).setOnClickListener {
            startActivity(Intent(this, CreateProfileActivity::class.java))
        }
    }
}
