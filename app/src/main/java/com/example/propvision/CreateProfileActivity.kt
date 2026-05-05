package com.example.propvision

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.*

class CreateProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var ivProfilePic: ImageView
    private lateinit var etFullName: EditText
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etDOB: EditText
    private lateinit var spinnerCountry: AutoCompleteTextView
    private lateinit var spinnerCity: AutoCompleteTextView
    private lateinit var etBio: EditText
    private lateinit var btnContinue: Button
    private lateinit var backBtn: View

    private var selectedImageUri: Uri? = null
    private var photoFile: File? = null

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openCamera() else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    private val storagePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) openGallery() else Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoFile != null) {
            selectedImageUri = Uri.fromFile(photoFile)
            ivProfilePic.setImageURI(selectedImageUri)
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            ivProfilePic.setImageURI(selectedImageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        initializeViews()
        setupListeners()
        setupSpinners()

        intent.getStringExtra("email")?.let {
            etEmail.setText(it)
            etEmail.isEnabled = false
        }
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
        btnContinue = findViewById(R.id.btnContinue)
        backBtn = findViewById(R.id.backBtn)
    }

    private fun setupListeners() {
        backBtn.setOnClickListener { finish() }
        findViewById<View>(R.id.btnEditImage).setOnClickListener { showImagePickerDialog() }
        etDOB.setOnClickListener { showDatePicker() }
        btnContinue.setOnClickListener { if (validateAllFields()) registerUser() }
        findViewById<View>(R.id.tvLoginLink).setOnClickListener { finish() }
    }

    private fun setupSpinners() {
        val countries = CountryCityData.getCountries()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, countries)
        spinnerCountry.setAdapter(adapter)

        spinnerCountry.setOnItemClickListener { _, _, pos, _ ->
            val selectedCountry = countries[pos]
            val cities = CountryCityData.getCitiesByCountry(selectedCountry)
            val cityAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cities)
            spinnerCity.setAdapter(cityAdapter)
            spinnerCity.setText("", false)
        }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            etDOB.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Camera", "Gallery", "Cancel")
        AlertDialog.Builder(this).setTitle("Select Profile Image").setItems(options) { _, which ->
            when (which) {
                0 -> requestCameraPermission()
                1 -> requestStoragePermission()
            }
        }.show()
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) openCamera()
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun requestStoragePermission() {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) openGallery()
        else storagePermissionLauncher.launch(perm)
    }

    private fun openCamera() {
        photoFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "profile_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile!!)
        cameraLauncher.launch(uri)
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun validateAllFields(): Boolean {
        var valid = true
        if (etFullName.text.isNullOrBlank()) { etFullName.error = "Name required"; valid = false }
        if (etEmail.text.isNullOrBlank()) { etEmail.error = "Email required"; valid = false }
        if (etPassword.text.isNullOrBlank() || etPassword.text.length < 6) { etPassword.error = "Password min 6 chars"; valid = false }
        if (etPassword.text.toString() != etConfirmPassword.text.toString()) { etConfirmPassword.error = "Passwords mismatch"; valid = false }
        return valid
    }

    private fun registerUser() {
        val email = etEmail.text.toString().trim()
        val pass = etPassword.text.toString().trim()
        showLoadingIndicator(true)

        auth.createUserWithEmailAndPassword(email, pass).addOnSuccessListener { res ->
            res.user?.let { user ->
                if (selectedImageUri != null) uploadProfileImage(user.uid)
                else saveUserData(user.uid, "")
            }
        }.addOnFailureListener {
            showLoadingIndicator(false)
            Toast.makeText(this, "Registration failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadProfileImage(uid: String) {
        val ref = storage.reference.child("profile_images/$uid/profile.jpg")
        selectedImageUri?.let { uri ->
            ref.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) task.exception?.let { throw it }
                ref.downloadUrl
            }.addOnSuccessListener { downloadUri ->
                saveUserData(uid, downloadUri.toString())
            }.addOnFailureListener {
                showLoadingIndicator(false)
                Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserData(uid: String, url: String) {
        val data = hashMapOf(
            "userId" to uid,
            "fullName" to etFullName.text.toString().trim(),
            "email" to etEmail.text.toString().trim(),
            "phone" to etPhone.text.toString().trim(),
            "dateOfBirth" to etDOB.text.toString().trim(),
            "country" to spinnerCountry.text.toString().trim(),
            "city" to spinnerCity.text.toString().trim(),
            "bio" to etBio.text.toString().trim(),
            "profileImageUrl" to url,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(uid).set(data).addOnSuccessListener {
            showLoadingIndicator(false)
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }.addOnFailureListener {
            showLoadingIndicator(false)
            Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoadingIndicator(show: Boolean) {
        btnContinue.isEnabled = !show
        btnContinue.text = if (show) "Registering..." else "Continue"
    }
}
