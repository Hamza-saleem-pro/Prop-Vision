package com.example.propvision

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.text.Editable
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import android.widget.TextView
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.android.material.textfield.TextInputLayout

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var cbRememberMe: CheckBox
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var showPassword: TextView
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        showPassword = findViewById(R.id.showPassword)
        val loginBtn = findViewById<Button>(R.id.loginBtn)

        loadSavedCredentials()
        setupInlineValidation()

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            clearFieldErrors()

            if (email.isEmpty()) {
                emailLayout.error = "Email is required"
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Invalid email format"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordLayout.error = "Password is required"
                return@setOnClickListener
            }

            if (password.length < 6) {
                passwordLayout.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            attemptLogin(email, password)
        }

        findViewById<View>(R.id.register).setOnClickListener {
            startActivity(Intent(this, CreateProfileActivity::class.java))
        }

        findViewById<View>(R.id.forgot).setOnClickListener { showResetPasswordDialog() }

        showPassword.setOnClickListener { togglePasswordVisibility() }
    }

    private fun setupInlineValidation() {
        emailInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                emailLayout.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        passwordInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                passwordLayout.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun clearFieldErrors() {
        emailLayout.error = null
        passwordLayout.error = null
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        passwordInput.transformationMethod = if (isPasswordVisible) {
            HideReturnsTransformationMethod.getInstance()
        } else {
            PasswordTransformationMethod.getInstance()
        }
        passwordInput.setSelection(passwordInput.text?.length ?: 0)
        showPassword.text = if (isPasswordVisible) "Hide password" else "Show password"
    }

    private fun loadSavedCredentials() {
        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                this,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val savedEmail = sharedPreferences.getString("email", "")
            val savedPassword = sharedPreferences.getString("password", "")

            if (!savedEmail.isNullOrEmpty()) {
                emailInput.setText(savedEmail)
                cbRememberMe.isChecked = true
            }
            if (!savedPassword.isNullOrEmpty()) {
                passwordInput.setText(savedPassword)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveCredentials(email: String, password: String) {
        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                this,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            if (cbRememberMe.isChecked) {
                sharedPreferences.edit()
                    .putString("email", email)
                    .putString("password", password)
                    .apply()
            } else {
                sharedPreferences.edit().clear().apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun attemptLogin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    saveCredentials(email, password)
                    Toast.makeText(this, "Login Successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    handleLoginError(task.exception, email)
                }
            }
    }

    private fun handleLoginError(exception: Exception?, email: String) {
        when (exception) {
            is FirebaseAuthInvalidUserException -> {
                emailLayout.error = "Account not found. Please sign up first."
                Toast.makeText(this, "Account not found. Please sign up first.", Toast.LENGTH_LONG).show()
                val intent = Intent(this, CreateProfileActivity::class.java)
                intent.putExtra("email", email)
                startActivity(intent)
            }
            is FirebaseAuthInvalidCredentialsException -> {
                passwordLayout.error = "Invalid credentials. Please try again."
                Toast.makeText(this, "Invalid credentials. Please try again.", Toast.LENGTH_LONG).show()
            }
            else -> {
                passwordLayout.error = exception?.message ?: "Login failed"
                Toast.makeText(this, "Login failed: ${exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showResetPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reset Password")
        val input = EditText(this)
        input.hint = "Enter your email"
        builder.setView(input)
        builder.setPositiveButton("Send Reset Link") { _, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) Toast.makeText(this, "Reset link sent", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(this, "Error sending reset link", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
}
