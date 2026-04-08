package com.example.propvision

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import coil.load

class AddPropertyActivity : AppCompatActivity() {

    private var bedroomCount = 1
    private var bathroomCount = 1
    private var kitchenCount = 1
    private var selectedType: String = "House"

    private val selectedImages = mutableListOf<Uri>()
    private val MAX_IMAGES = 12

    private var selectedAddress: String? = null
    private var selectedLat: Double = 0.0
    private var selectedLng: Double = 0.0

    private lateinit var photoContainer: LinearLayout
    private lateinit var photoTitle: TextView
    private lateinit var addPhotoBtn: View
    private lateinit var txtSelectedLocation: TextView
    private lateinit var btnRemoveLocation: ImageView
    
    private lateinit var typeHouse: TextView
    private lateinit var typeApartment: TextView
    private lateinit var typeVilla: TextView
    private lateinit var typeFlat: TextView

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var driveLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var locationLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_property)

        photoContainer = findViewById(R.id.photoContainer)
        photoTitle = findViewById(R.id.photoTitle)
        addPhotoBtn = findViewById(R.id.addPhotoBtn)
        txtSelectedLocation = findViewById(R.id.txtSelectedLocation)
        btnRemoveLocation = findViewById(R.id.btnRemoveLocation)
        
        typeHouse = findViewById(R.id.typeHouse)
        typeApartment = findViewById(R.id.typeApartment)
        typeVilla = findViewById(R.id.typeVilla)
        typeFlat = findViewById(R.id.typeFlat)

        setupTypeSelection()
        setupLaunchers()
        setupCounters()
        setupNavigation()
        setupMapIntegration()
        setupPhotoUpload()

        findViewById<Button>(R.id.finishBtn).setOnClickListener {
            validateAndFinish()
        }
    }

    private fun setupTypeSelection() {
        val types = listOf(typeHouse, typeApartment, typeVilla, typeFlat)
        
        // Initial selection: House is dark, others are white
        selectType(typeHouse)

        types.forEach { textView ->
            textView.setOnClickListener {
                selectType(textView)
            }
        }
    }

    private fun selectType(textView: TextView) {
        val types = listOf(typeHouse, typeApartment, typeVilla, typeFlat)
        val selectedColor = ContextCompat.getColor(this, R.color.selected_bg)
        val unselectedColor = Color.WHITE
        val textColorUnselected = Color.parseColor("#2D3A5F")
        val textColorSelected = Color.WHITE

        types.forEach { 
            // Reset all to white background and dark text
            it.backgroundTintList = ColorStateList.valueOf(unselectedColor)
            it.setTextColor(textColorUnselected)
        }
        
        // Apply dark background and white text to selected button
        textView.backgroundTintList = ColorStateList.valueOf(selectedColor)
        textView.setTextColor(textColorSelected)
        selectedType = textView.text.toString()
    }

    private fun setupLaunchers() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Photo captured", Toast.LENGTH_SHORT).show()
            }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            addImages(uris)
        }

        driveLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            addImages(uris)
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }

        locationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                selectedAddress = data?.getStringExtra("address")
                selectedLat = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
                selectedLng = data?.getDoubleExtra("longitude", 0.0) ?: 0.0
                
                txtSelectedLocation.text = selectedAddress ?: "Location Selected"
                txtSelectedLocation.setTextColor(ContextCompat.getColor(this, R.color.black))
                btnRemoveLocation.visibility = View.VISIBLE
            }
        }
    }

    private fun addImages(uris: List<Uri>) {
        val remaining = MAX_IMAGES - selectedImages.size
        if (remaining <= 0) {
            Toast.makeText(this, "Maximum 12 images allowed", Toast.LENGTH_SHORT).show()
            return
        }

        val toAdd = uris.take(remaining)
        selectedImages.addAll(toAdd)
        updatePhotoUI()
    }

    private fun updatePhotoUI() {
        val childCount = photoContainer.childCount
        for (i in childCount - 1 downTo 0) {
            val view = photoContainer.getChildAt(i)
            if (view.id != R.id.addPhotoBtn) {
                photoContainer.removeViewAt(i)
            }
        }

        val inflater = LayoutInflater.from(this)
        selectedImages.forEachIndexed { index, uri ->
            val itemView = inflater.inflate(R.layout.item_property_image, photoContainer, false)
            val imageView = itemView.findViewById<ImageView>(R.id.ivPropertyPhoto)
            val deleteBtn = itemView.findViewById<ImageView>(R.id.btnDeletePhoto)

            imageView.load(uri)
            deleteBtn.setOnClickListener {
                selectedImages.removeAt(index)
                updatePhotoUI()
            }

            photoContainer.addView(itemView, photoContainer.childCount - 1)
        }

        photoTitle.text = "Add Photos (${selectedImages.size}/$MAX_IMAGES)"
        addPhotoBtn.visibility = if (selectedImages.size >= MAX_IMAGES) View.GONE else View.VISIBLE
    }

    private fun setupPhotoUpload() {
        addPhotoBtn.setOnClickListener {
            showPhotoOptionsDialog()
        }
    }

    private fun showPhotoOptionsDialog() {
        val options = arrayOf("Camera", "Gallery", "Drive")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Property Photos")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> checkCameraPermissionAndOpen()
                1 -> openGallery()
                2 -> openDrive()
            }
        }
        builder.show()
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun openDrive() {
        driveLauncher.launch(arrayOf("image/*"))
    }

    private fun setupCounters() {
        val txtBedroom = findViewById<TextView>(R.id.txtBedroomCount)
        findViewById<ImageView>(R.id.btnAddBedroom).setOnClickListener {
            bedroomCount++
            txtBedroom.text = bedroomCount.toString()
        }
        findViewById<ImageView>(R.id.btnMinusBedroom).setOnClickListener {
            if (bedroomCount > 0) {
                bedroomCount--
                txtBedroom.text = bedroomCount.toString()
            }
        }

        val txtBathroom = findViewById<TextView>(R.id.txtBathroomCount)
        findViewById<ImageView>(R.id.btnAddBathroom).setOnClickListener {
            bathroomCount++
            txtBathroom.text = bathroomCount.toString()
        }
        findViewById<ImageView>(R.id.btnMinusBathroom).setOnClickListener {
            if (bathroomCount > 0) {
                bathroomCount--
                txtBathroom.text = bathroomCount.toString()
            }
        }

        val txtKitchen = findViewById<TextView>(R.id.txtKitchenCount)
        findViewById<ImageView>(R.id.btnAddKitchen).setOnClickListener {
            kitchenCount++
            txtKitchen.text = kitchenCount.toString()
        }
        findViewById<ImageView>(R.id.btnMinusKitchen).setOnClickListener {
            if (kitchenCount > 0) {
                kitchenCount--
                txtKitchen.text = kitchenCount.toString()
            }
        }
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.backBtn).setOnClickListener {
            finish()
        }
    }

    private fun setupMapIntegration() {
        val openMapAction = View.OnClickListener {
            val intent = Intent(this, SelectLocationActivity::class.java)
            locationLauncher.launch(intent)
        }
        findViewById<View>(R.id.mapPlaceholder).setOnClickListener(openMapAction)
        findViewById<View>(R.id.openMapBtn).setOnClickListener(openMapAction)
        
        btnRemoveLocation.setOnClickListener {
            selectedAddress = null
            selectedLat = 0.0
            selectedLng = 0.0
            txtSelectedLocation.text = "No location selected"
            txtSelectedLocation.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            btnRemoveLocation.visibility = View.GONE
        }
    }

    private fun validateAndFinish() {
        val sellPrice = findViewById<EditText>(R.id.sellPriceInput).text.toString().trim()
        val rentPrice = findViewById<EditText>(R.id.rentPriceInput).text.toString().trim()

        if (sellPrice.isEmpty() && rentPrice.isEmpty()) {
            Toast.makeText(this, "Please enter at least one price (Sell or Rent)", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedAddress == null) {
            Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Please add at least one photo", Toast.LENGTH_SHORT).show()
            return
        }

        val property = Property(
            propertyType = selectedType,
            sellPrice = if (sellPrice.isNotEmpty()) sellPrice else null,
            rentPrice = if (rentPrice.isNotEmpty()) rentPrice else null,
            bedroomCount = bedroomCount,
            bathroomCount = bathroomCount,
            kitchenCount = kitchenCount,
            imageUris = selectedImages.map { it.toString() },
            address = selectedAddress!!,
            latitude = selectedLat,
            longitude = selectedLng
        )

        val resultIntent = Intent()
        resultIntent.putExtra("NEW_PROPERTY", property)
        setResult(Activity.RESULT_OK, resultIntent)
        
        Toast.makeText(this, "Property listed successfully!", Toast.LENGTH_LONG).show()
        finish()
    }
}
