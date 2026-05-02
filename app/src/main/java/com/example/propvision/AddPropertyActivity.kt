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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var isEditing = false
    private var editingPropertyId: String? = null
    private val existingImageUrls = mutableListOf<String>()

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

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Check if opened for editing
        val editId = intent.getStringExtra("EDIT_PROPERTY_ID")
        if (!editId.isNullOrEmpty()) {
            isEditing = true
            editingPropertyId = editId
            loadPropertyForEdit(editId)
        }

        findViewById<Button>(R.id.finishBtn).setOnClickListener {
            validateAndFinish()
        }
    }

    private fun setupTypeSelection() {
        val types = listOf(typeHouse, typeApartment, typeVilla, typeFlat)
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
            it.backgroundTintList = ColorStateList.valueOf(unselectedColor)
            it.setTextColor(textColorUnselected)
        }
        
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

    private fun loadPropertyForEdit(propertyId: String) {
        db.collection("properties").document(propertyId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val type = doc.getString("propertyType") ?: "House"
                    when (type.lowercase()) {
                        "house" -> selectType(typeHouse)
                        "apartment" -> selectType(typeApartment)
                        "villa" -> selectType(typeVilla)
                        "flat" -> selectType(typeFlat)
                        else -> selectType(typeHouse)
                    }

                    findViewById<EditText>(R.id.sellPriceInput).setText(doc.getString("sellPrice"))
                    findViewById<EditText>(R.id.rentPriceInput).setText(doc.getString("rentPrice"))
                    val b = (doc.getLong("bedroomCount") ?: 1L).toInt()
                    val ba = (doc.getLong("bathroomCount") ?: 1L).toInt()
                    val k = (doc.getLong("kitchenCount") ?: 1L).toInt()
                    bedroomCount = b
                    bathroomCount = ba
                    kitchenCount = k
                    findViewById<TextView>(R.id.txtBedroomCount).text = b.toString()
                    findViewById<TextView>(R.id.txtBathroomCount).text = ba.toString()
                    findViewById<TextView>(R.id.txtKitchenCount).text = k.toString()

                    val images = doc.get("imageUris") as? List<String> ?: emptyList()
                    existingImageUrls.clear()
                    existingImageUrls.addAll(images)
                    // add existing image URLs to selectedImages as Uri.parse so they show in UI
                    selectedImages.clear()
                    images.forEach { url -> selectedImages.add(Uri.parse(url)) }
                    updatePhotoUI()

                    selectedAddress = doc.getString("address")
                    selectedLat = doc.getDouble("latitude") ?: 0.0
                    selectedLng = doc.getDouble("longitude") ?: 0.0
                    txtSelectedLocation.text = selectedAddress ?: "No location selected"
                    btnRemoveLocation.visibility = if (selectedAddress != null) View.VISIBLE else View.GONE
                }
            }
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

        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Please add at least one photo", Toast.LENGTH_SHORT).show()
            return
        }

        if (sellPrice.isEmpty() && rentPrice.isEmpty()) {
            Toast.makeText(this, "Please enter at least one price (Sell or Rent)", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedAddress == null) {
            Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show()
            return
        }


        // Proceed to upload images (if any new) and save property to Firestore
        uploadImagesAndSaveProperty(sellPrice, rentPrice)
    }

    private fun uploadImagesAndSaveProperty(sellPrice: String, rentPrice: String) {
        val userId = auth.currentUser?.uid ?: "anonymous"
        val uploadTasks = mutableListOf<com.google.android.gms.tasks.Task<Uri>>()
        val finalImageUrls = mutableListOf<String>()

        // For each selectedImages Uri: if it's an http(s) URL, keep it; otherwise upload to Firebase Storage
        if (selectedImages.isEmpty() && existingImageUrls.isNotEmpty()) {
            // user didn't change images during edit - preserve existing URLs
            finalImageUrls.addAll(existingImageUrls)
        } else {
            selectedImages.forEachIndexed { index, uri ->
            val s = uri.toString()
            if (s.startsWith("http://") || s.startsWith("https://")) {
                finalImageUrls.add(s)
            } else {
                // Upload local/content URI
                val refPath = "properties/$userId/${System.currentTimeMillis()}_$index.jpg"
                val storageRef: StorageReference = storage.reference.child(refPath)
                val uploadTask = storageRef.putFile(uri).continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    storageRef.downloadUrl
                }
                uploadTasks.add(uploadTask)
                uploadTask.addOnSuccessListener { dl ->
                    finalImageUrls.add(dl.toString())
                }
            }
            }
        }

        // When all uploadTasks complete, save property
        if (uploadTasks.isEmpty()) {
            // No uploads, save directly
            savePropertyToFirestore(userId, sellPrice, rentPrice, finalImageUrls)
        } else {
            com.google.android.gms.tasks.Tasks.whenAllComplete(uploadTasks)
                .addOnSuccessListener {
                    savePropertyToFirestore(userId, sellPrice, rentPrice, finalImageUrls)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to upload images: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun savePropertyToFirestore(userId: String, sellPrice: String, rentPrice: String, imageUrls: List<String>) {
        val data = hashMapOf<String, Any?>(
            "propertyType" to selectedType,
            "sellPrice" to if (sellPrice.isNotEmpty()) sellPrice else null,
            "rentPrice" to if (rentPrice.isNotEmpty()) rentPrice else null,
            "bedroomCount" to bedroomCount,
            "bathroomCount" to bathroomCount,
            "kitchenCount" to kitchenCount,
            "imageUris" to imageUrls,
            "address" to selectedAddress,
            "latitude" to selectedLat,
            "longitude" to selectedLng,
            "ownerId" to userId,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        if (isEditing && !editingPropertyId.isNullOrEmpty()) {
            db.collection("properties").document(editingPropertyId!!)
                .set(data)
                .addOnSuccessListener {
                    NotificationRepository.addNotification(Notification("Listing Updated", "Your listing has been updated", SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())))
                    Toast.makeText(this, "Property updated successfully!", Toast.LENGTH_LONG).show()
                    val resultIntent = Intent()
                    resultIntent.putExtra("UPDATED_PROPERTY_ID", editingPropertyId)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update property: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            db.collection("properties").add(data)
                .addOnSuccessListener { docRef ->
                    NotificationRepository.addNotification(Notification("Listing Live", "Your $selectedType in $selectedAddress is now listed and visible to buyers!", SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())))
                    Toast.makeText(this, "Property listed successfully!", Toast.LENGTH_LONG).show()
                    val resultIntent = Intent()
                    resultIntent.putExtra("NEW_PROPERTY_ID", docRef.id)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save property: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
