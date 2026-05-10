package com.example.propvision

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

/**
 * PROFESSIONAL EMAIL OTP VERIFICATION
 * 
 * This activity handles the validation of the 6-digit OTP sent to the user's email.
 * It uses Firebase Firestore to verify the code and handles expiry logic.
 */
class OtpVerificationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    private lateinit var otpInput: TextInputEditText
    private lateinit var verifyBtn: MaterialButton
    private lateinit var tvMessage: TextView
    private lateinit var tvResendTimer: TextView
    private lateinit var btnResend: TextView
    private lateinit var progressBar: ProgressBar
    
    private var resendTimer: CountDownTimer? = null
    private var email: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        email = intent.getStringExtra("email") ?: auth.currentUser?.email ?: ""
        
        initializeViews()
        startResendTimer()

        findViewById<View>(R.id.backBtn).setOnClickListener { finish() }

        verifyBtn.setOnClickListener {
            verifyOtp()
        }

        btnResend.setOnClickListener {
            generateNewOtp()
        }
    }

    private fun initializeViews() {
        otpInput = findViewById(R.id.otpInput)
        verifyBtn = findViewById(R.id.verifyBtn)
        tvMessage = findViewById(R.id.tvMessage)
        tvResendTimer = findViewById(R.id.tvResendTimer)
        btnResend = findViewById(R.id.btnResend)
        progressBar = findViewById(R.id.progressBar)

        tvMessage.text = "Please enter the 6-digit code sent to $email"
    }

    private fun verifyOtp() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }

        val enteredOtp = otpInput.text.toString().trim()
        if (enteredOtp.length != 6) {
            otpInput.error = "Enter a valid 6-digit OTP"
            return
        }

        val user = auth.currentUser ?: return
        
        progressBar.visibility = View.VISIBLE
        verifyBtn.isEnabled = false

        // Securely fetch OTP from Firestore
        db.collection("otps").document(user.uid).get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE
                verifyBtn.isEnabled = true
                
                if (document != null && document.exists()) {
                    val actualOtp = document.getString("code")
                    val expiryTime = document.getLong("expiryTime") ?: 0L
                    val currentTime = System.currentTimeMillis()
                    
                    // 1. Check for Expiry (5 Minutes)
                    if (currentTime > expiryTime) {
                        Toast.makeText(this, "OTP Expired. Please click Resend.", Toast.LENGTH_LONG).show()
                        db.collection("otps").document(user.uid).delete()
                        return@addOnSuccessListener
                    }

                    // 2. Check if OTP matches
                    if (actualOtp == enteredOtp) {
                        // Success - Clear OTP and navigate to Home
                        db.collection("otps").document(user.uid).delete()
                        
                        // Save login session
                        val sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                        sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()
                        
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, HomeActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Enter a valid OTP", Toast.LENGTH_LONG).show()
                        // Redirect to Login after failure for security as requested
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            auth.signOut()
                            val loginIntent = Intent(this, SplashActivity::class.java)
                            loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(loginIntent)
                            finish()
                        }, 4000)
                    }
                } else {
                    Toast.makeText(this, "Invalid request. Please login again.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                verifyBtn.isEnabled = true
                Toast.makeText(this, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun generateNewOtp() {
        val user = auth.currentUser ?: return
        progressBar.visibility = View.VISIBLE
        
        val newOtp = (100000..999999).random().toString()
        
        val batch = db.batch()
        
        // 1. Store OTP for verification with 5-min expiry
        val otpData = mapOf(
            "code" to newOtp,
            "timestamp" to FieldValue.serverTimestamp(),
            "email" to email,
            "expiryTime" to System.currentTimeMillis() + 300000
        )
        val otpRef = db.collection("otps").document(user.uid)
        batch.set(otpRef, otpData)
        
        // 2. Add document to 'mail' collection for Trigger Email Extension
        val emailData = mapOf(
            "to" to listOf(email),
            "message" to mapOf(
                "subject" to "PropertyVision Verification Code",
                "html" to """
                    <div style="font-family: Arial, sans-serif; padding: 20px; color: #333; background-color: #f4f4f4; border-radius: 10px;">
                        <div style="text-align: center; margin-bottom: 20px;">
                            <h2 style="color: #234F68;">PropertyVision Pakistan</h2>
                        </div>
                        <div style="background-color: #ffffff; padding: 30px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                            <p>Hello,</p>
                            <p>To continue your login, please use the following 6-digit verification code:</p>
                            <div style="text-align: center; margin: 30px 0;">
                                <span style="font-size: 36px; font-weight: bold; color: #234F68; letter-spacing: 10px; padding: 15px; background: #e8f0fe; border-radius: 5px;">$newOtp</span>
                            </div>
                            <p style="color: #666;">This code will expire in <b>5 minutes</b>.</p>
                            <p style="color: #d32f2f; font-size: 13px; margin-top: 20px;"><b>Security Warning:</b> Do not share this OTP with anyone, including individuals claiming to be from PropertyVision.</p>
                        </div>
                        <p style="text-align: center; font-size: 11px; color: #999; margin-top: 20px;">&copy; 2025 PropertyVision. Secure Property Management Solutions.</p>
                    </div>
                """.trimIndent()
            )
        )
        val mailRef = db.collection("mail").document()
        batch.set(mailRef, emailData)
        
        batch.commit().addOnCompleteListener { task ->
            progressBar.visibility = View.GONE
            if (task.isSuccessful) {
                Toast.makeText(this, "New OTP sent to $email", Toast.LENGTH_SHORT).show()
                startResendTimer()
            } else {
                Toast.makeText(this, "Failed to resend: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startResendTimer() {
        btnResend.visibility = View.GONE
        tvResendTimer.visibility = View.VISIBLE
        
        resendTimer?.cancel()
        resendTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvResendTimer.text = "Resend in ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                tvResendTimer.visibility = View.GONE
                btnResend.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
    }
}
