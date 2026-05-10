package com.example.propvision

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.text.TextUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import coil.load
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

class UserProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    private lateinit var ivProfilePic: ImageView
    private lateinit var tvFullName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvCountryCity: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvDOB: TextView
    private lateinit var backBtn: ImageView
    private lateinit var storage: FirebaseStorage
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadProfileImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize UI components
        initializeViews()
        setupNavigation()

        backBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Load user profile
        loadUserProfile()

        // Edit profile button (if present in layout)
        // Allow changing profile picture by tapping it
        ivProfilePic.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<View>(R.id.editProfileBtn)?.setOnClickListener {
            openEditProfileDialog()
        }

        findViewById<View>(R.id.btnLogout).setOnClickListener {
            logoutUser()
        }
    }

    private fun logoutUser() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                
                // Clear login session
                val sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                sharedPreferences.edit().putBoolean("isLoggedIn", false).apply()

                val intent = Intent(this, SplashActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun initializeViews() {
        backBtn = findViewById(R.id.backBtn)
        ivProfilePic = findViewById(R.id.ivProfilePic)
        tvFullName = findViewById(R.id.tvFullName)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        tvCountryCity = findViewById(R.id.tvCountryCity)
        tvBio = findViewById(R.id.tvBio)
        tvDOB = findViewById(R.id.tvDOB)
    }

    @Suppress("SetTextI18n")
    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val fullName = document.getString("fullName") ?: "N/A"
                        val email = document.getString("email") ?: "N/A"
                        val phone = document.getString("phone") ?: "N/A"
                        val country = document.getString("country") ?: ""
                        val city = document.getString("city") ?: ""
                        val dob = document.getString("dateOfBirth") ?: "N/A"
                        val bio = document.getString("bio") ?: "N/A"
                        val profileImageUrl = document.getString("profileImageUrl")

                        tvFullName.text = fullName
                        tvEmail.text = email
                        tvPhone.text = phone
                        
                        val location = TextUtils.join(", ", listOf(city, country).filter { it.isNotBlank() })
                        tvCountryCity.text = location

                        tvBio.text = bio.ifBlank { getString(R.string.no_bio_available) }
                        tvDOB.text = getString(R.string.profile_dob_value, dob)

                        if (!profileImageUrl.isNullOrEmpty()) {
                            ivProfilePic.load(profileImageUrl) {
                                crossfade(true)
                                placeholder(R.drawable.ic_avatar)
                                error(R.drawable.ic_avatar)
                                listener(
                                    onError = { _, _ ->
                                        ivProfilePic.setImageResource(R.drawable.ic_avatar)
                                    }
                                )
                            }
                        } else {
                            ivProfilePic.setImageResource(R.drawable.ic_avatar)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    android.util.Log.e("UserProfileActivity", "Error loading profile", exception)
                }
        }
    }

    private fun uploadProfileImage(uri: Uri) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        Toast.makeText(this, "Updating profile picture...", Toast.LENGTH_SHORT).show()

        val storageRef = storage.reference.child(ProfileImageStoragePaths.profileImagePath(userId))

        storageRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->

                val profileData = mapOf(
                    "profileImageUrl" to downloadUri.toString()
                )

                db.collection("users")
                    .document(userId)
                    .set(profileData, SetOptions.merge())
                    .addOnSuccessListener {

                        ivProfilePic.load(downloadUri.toString()) {
                            crossfade(true)
                            placeholder(R.drawable.ic_avatar)
                            error(R.drawable.ic_avatar)
                        }

                        Toast.makeText(
                            this,
                            "Profile picture updated",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Image uploaded, but profile save failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        loadUserProfile()
                    }
            }
            .addOnFailureListener { e ->

                Toast.makeText(
                    this,
                    "Upload failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                ivProfilePic.setImageResource(R.drawable.ic_avatar)
                loadUserProfile()
            }
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.nav_home).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }

        findViewById<View>(R.id.nav_explore).setOnClickListener {
            startActivity(Intent(this, ExplorePropertiesActivity::class.java))
        }

        findViewById<View>(R.id.nav_add).setOnClickListener {
            startActivity(Intent(this, AddPropertyActivity::class.java))
        }

        findViewById<View>(R.id.nav_market).setOnClickListener {
            startActivity(Intent(this, MyAdsActivity::class.java))
        }
    }

    private fun openEditProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val etFullName = dialogView.findViewById<EditText>(R.id.etFullName)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val etCountry = dialogView.findViewById<EditText>(R.id.etCountry)
        val etCity = dialogView.findViewById<EditText>(R.id.etCity)
        val etDOB = dialogView.findViewById<EditText>(R.id.etDOB)
        val etBio = dialogView.findViewById<EditText>(R.id.etBio)

        // Prefill with current values
        etFullName.setText(tvFullName.text)
        etPhone.setText(tvPhone.text)
        val parts = tvCountryCity.text.split(",")
        if (parts.size >= 2) {
            etCity.setText(parts[0].trim())
            etCountry.setText(parts[1].trim())
        }
        etDOB.setText(tvDOB.text)
        etBio.setText(tvBio.text)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Profile")
        builder.setView(dialogView)
        builder.setPositiveButton("Save") { _, _ ->
            val updates = hashMapOf<String, Any?>()
            updates["fullName"] = etFullName.text.toString().trim()
            updates["phone"] = etPhone.text.toString().trim()
            updates["country"] = etCountry.text.toString().trim()
            updates["city"] = etCity.text.toString().trim()
            updates["dateOfBirth"] = etDOB.text.toString().trim()
            updates["bio"] = etBio.text.toString().trim()

            val currentUser = auth.currentUser
            if (currentUser != null) {
                db.collection("users").document(currentUser.uid)
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener {
                        loadUserProfile()
                        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
}

