package com.example.propvision

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CreateProfileActivity : AppCompatActivity() {

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // UI components
    private lateinit var ivProfilePic: ImageView
    private lateinit var etFullName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etDOB: EditText
    private lateinit var spinnerCountry: Spinner
    private lateinit var spinnerCity: Spinner
    private lateinit var etBio: EditText
    private lateinit var btnEditImage: View
    private lateinit var btnContinue: Button
    private lateinit var backBtn: View

    // Data
    private var selectedImageUri: Uri? = null
    private var listOfCountries: List<String> = emptyList()
    private var selectedCountry: String = ""

    // Permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            Toast.makeText(this, getString(R.string.success_image_uploaded), Toast.LENGTH_SHORT).show()
            ivProfilePic.setImageURI(selectedImageUri)
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            Toast.makeText(this, getString(R.string.success_image_uploaded), Toast.LENGTH_SHORT).show()
            ivProfilePic.setImageURI(selectedImageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize UI components
        initializeViews()

        // Pre-fill email if passed from login screen
        val passedEmail = intent.getStringExtra("email")
        if (passedEmail != null) {
            etEmail.setText(passedEmail)
            etEmail.isEnabled = false
        }

        // Set up listeners
        setupListeners()

        // Set up dropdowns
        setupCountrySpinner()
    }

    private fun initializeViews() {
        ivProfilePic = findViewById(R.id.ivProfilePic)
        etFullName = findViewById(R.id.etFullName)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        etDOB = findViewById(R.id.etDOB)
        spinnerCountry = findViewById(R.id.spinnerCountry)
        spinnerCity = findViewById(R.id.spinnerCity)
        etBio = findViewById(R.id.etBio)
        btnEditImage = findViewById(R.id.btnEditImage)
        btnContinue = findViewById(R.id.btnContinue)
        backBtn = findViewById(R.id.backBtn)
    }

    private fun setupListeners() {
        backBtn.setOnClickListener {
            finish()
        }

        btnEditImage.setOnClickListener {
            showImagePickerDialog()
        }

        etDOB.setOnClickListener {
            showDatePicker()
        }

        spinnerCountry.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                selectedCountry = parent?.getItemAtPosition(position).toString()
                setupCitySpinner(selectedCountry)
            }

            override fun onNothingSelected(p0: android.widget.AdapterView<*>?) {}
        }

        btnContinue.setOnClickListener {
            if (validateAllFields()) {
                registerUser()
            }
        }
    }

    private fun setupCountrySpinner() {
        listOfCountries = CountryCityData.getCountries()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOfCountries)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCountry.adapter = adapter
    }

    private fun setupCitySpinner(country: String) {
        val cities = CountryCityData.getCitiesByCountry(country)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCity.adapter = adapter
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                etDOB.setText(formattedDate)
            },
            year,
            month,
            day
        )

        datePickerDialog.datePicker.maxDate = calendar.timeInMillis
        datePickerDialog.show()
    }

    private fun showImagePickerDialog() {
        val options = arrayOf(
            getString(R.string.camera),
            getString(R.string.gallery),
            getString(R.string.cancel)
        )

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_profile_image))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        requestCameraPermission()
                    }
                    1 -> {
                        requestStoragePermission()
                    }
                    2 -> {
                        // Cancel
                    }
                }
            }
            .show()
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            storagePermissionLauncher.launch(permission)
        }
    }

    private fun openCamera() {
        val photoFile = File.createTempFile("IMG_", ".jpg")
        selectedImageUri = Uri.fromFile(photoFile)
        cameraLauncher.launch(selectedImageUri)
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun validateAllFields(): Boolean {
        clearErrors()

        var isValid = true

        // Validate full name
        if (etFullName.text.toString().trim().isEmpty()) {
            etFullName.error = getString(R.string.error_full_name_required)
            isValid = false
        }

        // Validate phone
        val phone = etPhone.text.toString().trim()
        if (phone.isEmpty()) {
            etPhone.error = getString(R.string.error_phone_required)
            isValid = false
        } else if (!isValidPhone(phone)) {
            etPhone.error = getString(R.string.error_phone_invalid)
            isValid = false
        }

        // Validate email
        val email = etEmail.text.toString().trim()
        if (email.isEmpty()) {
            etEmail.error = getString(R.string.error_email_required)
            isValid = false
        } else if (!isValidEmail(email)) {
            etEmail.error = getString(R.string.error_email_invalid)
            isValid = false
        }

        // Validate password
        val password = etPassword.text.toString().trim()
        if (password.isEmpty()) {
            etPassword.error = getString(R.string.error_password_required)
            isValid = false
        } else if (password.length < 6) {
            etPassword.error = getString(R.string.error_password_short)
            isValid = false
        }

        // Validate confirm password
        val confirmPassword = etConfirmPassword.text.toString().trim()
        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error = getString(R.string.error_confirm_password_required)
            isValid = false
        } else if (password != confirmPassword) {
            etConfirmPassword.error = getString(R.string.error_passwords_mismatch)
            isValid = false
        }

        // Validate DOB
        if (etDOB.text.toString().trim().isEmpty()) {
            etDOB.error = getString(R.string.error_dob_required)
            isValid = false
        }

        // Validate country
        if (spinnerCountry.selectedItem == null || spinnerCountry.selectedItem.toString().isEmpty()) {
            Toast.makeText(this, getString(R.string.error_country_required), Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Validate city
        if (spinnerCity.selectedItem == null || spinnerCity.selectedItem.toString().isEmpty()) {
            Toast.makeText(this, getString(R.string.error_city_required), Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun clearErrors() {
        etFullName.error = null
        etPhone.error = null
        etEmail.error = null
        etPassword.error = null
        etConfirmPassword.error = null
        etDOB.error = null
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.length >= 10 && phone.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }
    }

    private fun registerUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        showLoadingIndicator(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    // Send email verification
                    user.sendEmailVerification()

                    // Upload profile image if selected
                    if (selectedImageUri != null) {
                        uploadProfileImage(user.uid)
                    } else {
                        saveUserDataToFirestore(user.uid, "")
                    }
                } else {
                    showLoadingIndicator(false)
                    Toast.makeText(this, "Failed to create account", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                showLoadingIndicator(false)
                Toast.makeText(
                    this,
                    "Registration failed: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun uploadProfileImage(userId: String) {
        if (selectedImageUri == null) {
            saveUserDataToFirestore(userId, "")
            return
        }

        val storageRef = storage.reference.child("profile_images/$userId.jpg")

        storageRef.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveUserDataToFirestore(userId, uri.toString())
                }
            }
            .addOnFailureListener { exception ->
                showLoadingIndicator(false)
                Toast.makeText(
                    this,
                    "Image upload failed: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun saveUserDataToFirestore(userId: String, imageUrl: String) {
        val userMap = hashMapOf(
            "userId" to userId,
            "fullName" to etFullName.text.toString().trim(),
            "email" to etEmail.text.toString().trim(),
            "phone" to etPhone.text.toString().trim(),
            "dateOfBirth" to etDOB.text.toString().trim(),
            "country" to spinnerCountry.selectedItem.toString(),
            "city" to spinnerCity.selectedItem.toString(),
            "bio" to etBio.text.toString().trim(),
            "profileImageUrl" to imageUrl,
            "emailVerified" to false,
            "phoneVerified" to false,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .set(userMap)
            .addOnSuccessListener {
                showLoadingIndicator(false)
                Toast.makeText(this, getString(R.string.success_account_created), Toast.LENGTH_SHORT).show()
                Toast.makeText(this, getString(R.string.success_email_verification_sent), Toast.LENGTH_SHORT).show()

                // Navigate to home after successful signup
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
            .addOnFailureListener { exception ->
                showLoadingIndicator(false)
                Toast.makeText(
                    this,
                    "Failed to save profile: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showLoadingIndicator(show: Boolean) {
        btnContinue.isEnabled = !show
        btnContinue.text = if (show) "Creating Account..." else getString(R.string.continue_btn)
    }
}
