package com.example.propvision

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    // Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInOptions: GoogleSignInOptions
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    // Facebook Login
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition for Android 12+
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if user is already signed in
        if (auth.currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginBtn = findViewById<Button>(R.id.loginBtn)

        // Initialize Google Sign-In options
        googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // Initialize Google Sign-In client
        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        // Register Google Sign-In activity result
        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleGoogleSignInResult(task)
            }
        }

        // Initialize Facebook Login button
        findViewById<View>(R.id.facebookBtn).setOnClickListener {
            loginWithFacebook()
        }

        // Initialize Google Sign-In button
        findViewById<View>(R.id.googleBtn).setOnClickListener {
            loginWithGoogle()
        }

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty()) {
                emailInput.error = "Email is required"
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.error = "Invalid email format"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordInput.error = "Password is required"
                return@setOnClickListener
            }

            // Start the login process
            attemptLogin(email, password)
        }

        findViewById<View>(R.id.register).setOnClickListener {
            val intent = Intent(this, CreateProfileActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.forgot).setOnClickListener {
            // Show reset password dialog
            showResetPasswordDialog()
        }
    }

    private fun attemptLogin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login successful
                    Toast.makeText(
                        this,
                        "Login Successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Navigate to HomeActivity
                    val intent = Intent(this, HomeActivity::class.java)
                    // Simple Admin Check logic
                    if (email == "admin@propvision.com") {
                        intent.putExtra("IS_ADMIN", true)
                    }
                    startActivity(intent)
                    finish() // Close SplashActivity so user can't return to it
                } else {
                    // Handle login failure with specific error messages
                    handleLoginError(task.exception, email)
                }
            }
    }

    private fun handleLoginError(exception: Exception?, email: String) {
        when (exception) {
            is FirebaseAuthInvalidUserException -> {
                // User account does not exist
                Toast.makeText(
                    this,
                    "Account not found. Please sign up first.",
                    Toast.LENGTH_LONG
                ).show()

                // Redirect to sign up with pre-filled email
                val intent = Intent(this, CreateProfileActivity::class.java)
                intent.putExtra("email", email)
                startActivity(intent)
            }
            is FirebaseAuthInvalidCredentialsException -> {
                // This means the password is wrong or email format is invalid
                if (exception.message?.contains("password", ignoreCase = true) == true ||
                    exception.message?.contains("wrong", ignoreCase = true) == true) {
                    // Wrong password
                    Toast.makeText(
                        this,
                        "Invalid password. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Invalid email format
                    Toast.makeText(
                        this,
                        "Enter a correct email or password.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            else -> {
                // Generic login error
                val errorMessage = exception?.message ?: "Unknown error occurred"

                // Try to provide a more specific message
                when {
                    errorMessage.contains("user", ignoreCase = true) ||
                    errorMessage.contains("not found", ignoreCase = true) -> {
                        Toast.makeText(
                            this,
                            "Account not found. Please sign up first.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Redirect to sign up
                        val intent = Intent(this, CreateProfileActivity::class.java)
                        intent.putExtra("email", email)
                        startActivity(intent)
                    }
                    errorMessage.contains("password", ignoreCase = true) ||
                    errorMessage.contains("credential", ignoreCase = true) -> {
                        Toast.makeText(
                            this,
                            "Invalid password. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {
                        Toast.makeText(
                            this,
                            "Enter a correct email or password.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
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
                            // Google Sign-In successful
                            Toast.makeText(
                                this,
                                "Google Sign-In Successful",
                                Toast.LENGTH_SHORT
                            ).show()

                            // Navigate to HomeActivity
                            val intent = Intent(this, HomeActivity::class.java)
                            // Simple Admin Check logic
                            if (account.email == "admin@propvision.com") {
                                intent.putExtra("IS_ADMIN", true)
                            }
                            startActivity(intent)
                            finish()
                        } else {
                            // Handle error
                            Toast.makeText(
                                this,
                                "Google Sign-In failed. Please try again.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }
        } catch (e: ApiException) {
            Toast.makeText(
                this,
                "Google Sign-In failed. Please try again.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun loginWithFacebook() {
        // Initialize Facebook Login button
        callbackManager = CallbackManager.Factory.create()

        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    handleFacebookAccessToken(result.accessToken)
                }

                override fun onCancel() {
                    // User cancelled the login
                    Toast.makeText(
                        this@SplashActivity,
                        "Facebook login cancelled",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(error: FacebookException) {
                    // Login error
                    Toast.makeText(
                        this@SplashActivity,
                        "Facebook login failed: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })

        // Perform the login
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
    }

    private fun handleFacebookAccessToken(token: AccessToken?) {
        val credential = FacebookAuthProvider.getCredential(token?.token!!)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Facebook Login successful
                    Toast.makeText(
                        this,
                        "Facebook Login Successful",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Navigate to HomeActivity
                    val intent = Intent(this, HomeActivity::class.java)
                    // Simple Admin Check logic
                    if (auth.currentUser?.email == "admin@propvision.com") {
                        intent.putExtra("IS_ADMIN", true)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    // Handle error
                    Toast.makeText(
                        this,
                        "Facebook Login failed. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
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

        // Set up the input field for email
        val input = EditText(this)
        input.hint = "Enter your email"
        builder.setView(input)

        builder.setPositiveButton("Send Reset Link") { dialog, which ->
            val email = input.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            // Send password reset email
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Reset link sent to your email",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Error: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun loginWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }
}
