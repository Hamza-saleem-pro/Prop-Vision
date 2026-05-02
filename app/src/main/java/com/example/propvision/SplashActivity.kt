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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.material.textfield.TextInputLayout

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    // Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInOptions: GoogleSignInOptions
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    // Facebook Login
    private lateinit var callbackManager: CallbackManager

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var cbRememberMe: CheckBox
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var showPassword: TextView
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() // Disable Android 12 automatic splash to show custom splash first
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()

        // Requirement: Require login whenever app is completely closed from stack.
        // We ensure we don't auto-redirect to Home even if auth.currentUser is not null.
        // Instead, we just keep them on this screen to re-authenticate.

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        cbRememberMe = findViewById(R.id.cbRememberMe)
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        showPassword = findViewById(R.id.showPassword)
        val loginBtn = findViewById<Button>(R.id.loginBtn)

        loadSavedCredentials()
        setupInlineValidation()

        googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleGoogleSignInResult(task)
            }
        }

        findViewById<View>(R.id.facebookBtn).setOnClickListener { loginWithFacebook() }
        findViewById<View>(R.id.googleBtn).setOnClickListener { loginWithGoogle() }

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
                    if (email == "admin@propvision.com") {
                        intent.putExtra("IS_ADMIN", true)
                    }
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

    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Google Sign-In Successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loginWithFacebook() {
        callbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) { handleFacebookAccessToken(result.accessToken) }
            override fun onCancel() { Toast.makeText(this@SplashActivity, "Facebook login cancelled", Toast.LENGTH_SHORT).show() }
            override fun onError(error: FacebookException) { Toast.makeText(this@SplashActivity, "Facebook login failed", Toast.LENGTH_LONG).show() }
        })
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
    }

    private fun handleFacebookAccessToken(token: AccessToken?) {
        val credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Facebook Login Successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Facebook Login failed.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
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

    private fun loginWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }
}
