package com.example.propvision

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class CreateProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_profile)

        val fullNameInput = findViewById<EditText>(R.id.fullNameInput)
        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val cityInput = findViewById<EditText>(R.id.cityInput)

        findViewById<View>(R.id.backBtn).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.skipBtn).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.continueBtn).setOnClickListener {
            if (validateInputs(fullNameInput, phoneInput, emailInput, cityInput)) {
                Toast.makeText(this, "Profile created successfully", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        }
    }

    private fun validateInputs(name: EditText, phone: EditText, email: EditText, city: EditText): Boolean {
        if (name.text.toString().trim().isEmpty()) {
            name.error = "Name is required"
            return false
        }
        if (phone.text.toString().trim().isEmpty()) {
            phone.error = "Phone number is required"
            return false
        }
        val emailStr = email.text.toString().trim()
        if (emailStr.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailStr).matches()) {
            email.error = "Valid email is required"
            return false
        }
        if (city.text.toString().trim().isEmpty()) {
            city.error = "City is required"
            return false
        }
        return true
    }
}
