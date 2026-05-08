package com.example.propvision

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.text.Editable
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.TextView
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.result.contract.ActivityResultContracts
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.android.material.textfield.TextInputLayout

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var cbRememberMe: CheckBox
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var loginProgressBar: ProgressBar

    private lateinit var googleSignInClient: GoogleSignInClient
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val idToken = account.idToken
            if (idToken != null) {
                firebaseAuthWithGoogle(idToken)
            } else {
                Toast.makeText(this, "Google ID Token is null. Check Web Client ID in Firebase.", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            val message = when (e.statusCode) {
                7 -> "Network error. Check your connection."
                10 -> "Developer error. Check SHA-1 and Web Client ID."
                12500 -> "Sign-in failed. Ensure Google Play Services are updated."
                12501 -> "Sign-in canceled."
                else -> "Google sign in failed (${e.statusCode}): ${e.message}"
            }
            if (e.statusCode != 12501) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        loginProgressBar = findViewById(R.id.loginProgressBar)
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

        findViewById<View>(R.id.googleSignInBtn).setOnClickListener {
            if (getString(R.string.default_web_client_id) == "PASTE_YOUR_WEB_CLIENT_ID_HERE") {
                Toast.makeText(this, "Configuration Error: Please update Web Client ID in strings.xml", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }
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
        loginProgressBar.visibility = View.VISIBLE
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val otpCode = (100000..999999).random().toString()
                        
                        val db = FirebaseFirestore.getInstance()
                        val batch = db.batch()
                        
                        // 1. Save OTP for secure server-side verification
                        val otpData = mapOf(
                            "code" to otpCode,
                            "timestamp" to FieldValue.serverTimestamp(),
                            "email" to email,
                            "expiryTime" to System.currentTimeMillis() + 300000 // 5 minutes
                        )
                        val otpRef = db.collection("otps").document(user.uid)
                        batch.set(otpRef, otpData)
                        
                        // 2. Prepare Email for Trigger Email Extension
                        val emailData = mapOf(
                            "to" to listOf(email),
                            "message" to mapOf(
                                "subject" to "PropertyVision Verification Code",
                                "html" to """
                                    <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 30px; color: #333; background-color: #f9f9f9; border-radius: 10px; max-width: 600px; margin: auto; border: 1px solid #eee;">
                                        <div style="text-align: center; margin-bottom: 20px;">
                                            <h1 style="color: #234F68; margin: 0;">PropertyVision Pakistan</h1>
                                            <p style="color: #666; font-size: 14px;">Secure Property Management</p>
                                        </div>
                                        <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                                        <p style="font-size: 16px;">Hello,</p>
                                        <p style="font-size: 16px;">Your 6-digit verification code for PropertyVision is:</p>
                                        <div style="background-color: #ffffff; padding: 20px; text-align: center; border-radius: 8px; border: 2px dashed #234F68; margin: 25px 0;">
                                            <h1 style="color: #234F68; font-size: 40px; letter-spacing: 8px; margin: 0; font-family: monospace;">$otpCode</h1>
                                        </div>
                                        <p style="color: #d32f2f; font-weight: bold; font-size: 14px;">This code will expire in 5 minutes.</p>
                                        <p style="color: #666; font-size: 12px; margin-top: 20px;">For your security, do not share this OTP with anyone. If you didn't request this code, please ignore this email.</p>
                                        <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; text-align: center; font-size: 12px; color: #999;">
                                            &copy; 2025 PropertyVision Pakistan. All rights reserved.
                                        </div>
                                    </div>
                                """.trimIndent()
                            )
                        )
                        val mailRef = db.collection("mail").document()
                        batch.set(mailRef, emailData)
                        
                        batch.commit().addOnCompleteListener { batchTask ->
                            loginProgressBar.visibility = View.GONE
                            if (batchTask.isSuccessful) {
                                saveCredentials(email, password)
                                val intent = Intent(this, OtpVerificationActivity::class.java)
                                intent.putExtra("email", email)
                                startActivity(intent)
                            } else {
                                Toast.makeText(this, "Failed to send OTP: ${batchTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    loginProgressBar.visibility = View.GONE
                    handleLoginError(task.exception, email)
                }
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        loginProgressBar.visibility = View.VISIBLE
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                loginProgressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    // Google users are usually verified by default
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
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
