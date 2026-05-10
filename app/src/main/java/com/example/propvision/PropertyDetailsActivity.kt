package com.example.propvision

import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.location.Location
import android.content.pm.PackageManager
import com.google.firebase.firestore.FirebaseFirestore
import androidx.viewpager2.widget.ViewPager2
import android.app.AlertDialog
import android.widget.RatingBar
import com.google.firebase.auth.FirebaseAuth
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import coil.load

class PropertyDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        setContentView(R.layout.activity_property_details)

        val property = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("PROPERTY", Property::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("PROPERTY") as? Property
        }

        if (property == null) {
            finish()
            return
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val vpPropertyImages = findViewById<ViewPager2>(R.id.vpPropertyImages)
        val tvDetailName = findViewById<TextView>(R.id.tvDetailName)
        val tvDetailLocation = findViewById<TextView>(R.id.tvDetailLocation)
        val tvDetailPrice = findViewById<TextView>(R.id.tvDetailPrice)
        val tvTypeTag = findViewById<TextView>(R.id.tvTypeTag)
        val tvDetailBed = findViewById<TextView>(R.id.tvDetailBed)
        val tvDetailBath = findViewById<TextView>(R.id.tvDetailBath)
        val tvDetailFullAddress = findViewById<TextView>(R.id.tvDetailFullAddress)
        val rbAverageRating = findViewById<RatingBar>(R.id.rbAverageRating)
        val tvRatingCount = findViewById<TextView>(R.id.tvRatingCount)
        val btnRate = findViewById<TextView>(R.id.btnRate)
        val tvDetailDescription = findViewById<TextView>(R.id.tvDetailDescription)
        val tvOwnerEmail = findViewById<TextView>(R.id.tvOwnerEmail)
        val tvOwnerPhone = findViewById<TextView>(R.id.tvOwnerPhone)
        val tvOwnerNameDetail = findViewById<TextView>(R.id.tvOwnerNameDetail)

        tvDetailName.text = property.propertyType 
        tvDetailLocation.text = property.address
        tvDetailFullAddress.text = property.address
        tvTypeTag.text = property.propertyType
        tvDetailBed.text = "${property.bedroomCount} Bedroom"
        tvDetailBath.text = "${property.bathroomCount} Bathroom"
        tvDetailDescription.text = property.description ?: "No description available."
        tvOwnerEmail.text = property.ownerEmail ?: "N/A"
        tvOwnerPhone.text = property.ownerPhone ?: "N/A"
        tvOwnerNameDetail.text = property.ownerName ?: "Property Owner"

        val priceText = when {
            property.rentPrice != null -> "Rs. ${property.rentPrice}"
            property.sellPrice != null -> "Rs. ${property.sellPrice}"
            else -> "N/A"
        }
        tvDetailPrice.text = priceText

        // Setup swipeable image pager
        val imagesForPager = if (property.imageUris.isNotEmpty()) property.imageUris else listOf()
        if (imagesForPager.isNotEmpty()) {
            vpPropertyImages.adapter = ImagePagerAdapter(this, imagesForPager)
        }

        // Load image gallery thumbnails
        loadImageGallery(property)

        val ivMapPreview = findViewById<ImageView>(R.id.ivMapPreview)
        val tvOpenOnMap = findViewById<TextView>(R.id.tvOpenOnMap)

        // Load static map preview if coordinates available
        if (property.latitude != 0.0 && property.longitude != 0.0) {
            try {
                val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                val apiKey = ai.metaData.getString("com.google.android.geo.API_KEY")
                val mapUrl = "https://maps.googleapis.com/maps/api/staticmap?center=${property.latitude},${property.longitude}" +
                        "&zoom=15&size=600x300&maptype=roadmap&markers=color:red%7C${property.latitude},${property.longitude}&key=$apiKey"
                ivMapPreview.load(mapUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_location_city)
                    error(R.drawable.ic_location_city)
                }
            } catch (e: Exception) {
                // fallback to simple icon if something fails
                ivMapPreview.setImageResource(R.drawable.ic_location_city)
            }
        } else {
            ivMapPreview.setImageResource(R.drawable.ic_location_city)
        }

        findViewById<View>(R.id.mapPreviewCard).setOnClickListener {
            if (property.latitude == 0.0 || property.longitude == 0.0) {
                Toast.makeText(
                    this,
                    "Location not available for this property. Please contact the owner.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val geoUri = Uri.parse("geo:${property.latitude},${property.longitude}?q=${property.latitude},${property.longitude}(${Uri.encode(property.address)})")
            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, geoUri).apply {
                setPackage("com.google.android.apps.maps")
            }

            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val fallbackIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, geoUri)
                if (fallbackIntent.resolveActivity(packageManager) != null) {
                    startActivity(fallbackIntent)
                } else {
                    Toast.makeText(this, "No maps app found", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Populate nearby properties (within 10km)
        loadNearbyProperties(property)

        // Load rating info and wire up Rate button
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        if (!property.id.isNullOrEmpty()) {
            db.collection("properties").document(property.id!!).get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val avg = doc.getDouble("avgRating") ?: 0.0
                        val count = (doc.getLong("ratingCount") ?: 0L).toInt()
                        rbAverageRating.rating = avg.toFloat()
                        tvRatingCount.text = "($count)"
                    }
                }
        }

        btnRate.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_rate_property, null)
            val ratingBar = dialogView.findViewById<RatingBar>(R.id.dialogRatingBar)
            val builder = AlertDialog.Builder(this)
                .setTitle("Rate this property")
                .setView(dialogView)
                .setPositiveButton("Submit") { _, _ ->
                    val value = ratingBar.rating.toDouble()
                    val currentUser = auth.currentUser
                    if (currentUser == null) {
                        Toast.makeText(this, "Please sign in to submit a rating.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    if (property.id.isNullOrEmpty()) {
                        Toast.makeText(this, "Property identifier missing.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    val ratingsRef = db.collection("properties").document(property.id!!).collection("ratings")
                    val userRatingDoc = ratingsRef.document(currentUser.uid)
                    userRatingDoc.set(mapOf("value" to value))
                        .addOnSuccessListener {
                            ratingsRef.get().addOnSuccessListener { snap ->
                                var sum = 0.0
                                for (d in snap.documents) {
                                    sum += (d.getDouble("value") ?: 0.0)
                                }
                                val newCount = snap.size()
                                val newAvg = if (newCount > 0) sum / newCount else 0.0
                                db.collection("properties").document(property.id!!)
                                    .update(mapOf("avgRating" to newAvg, "ratingCount" to newCount))
                                    .addOnSuccessListener {
                                        rbAverageRating.rating = newAvg.toFloat()
                                        tvRatingCount.text = "($newCount)"
                                        Toast.makeText(this, "Thank you for your rating!", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to submit rating: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Cancel", null)
            builder.show()
        }
    }

    private fun loadNearbyProperties(property: Property) {
        val nearbyContainer = findViewById<LinearLayout>(R.id.nearbyContainer)
        nearbyContainer.removeAllViews()

        val db = FirebaseFirestore.getInstance()
        db.collection("properties").get()
            .addOnSuccessListener { snapshot ->
                val inflater = layoutInflater
                for (doc in snapshot.documents) {
                    try {
                        val id = doc.id
                        if (id == property.id) continue
                        val lat = doc.getDouble("latitude") ?: 0.0
                        val lng = doc.getDouble("longitude") ?: 0.0
                        if (lat == 0.0 && lng == 0.0) continue

                        val results = FloatArray(1)
                        Location.distanceBetween(property.latitude, property.longitude, lat, lng, results)
                        val distanceMeters = results[0]
                        // include within 10km
                        if (distanceMeters <= 10000f) {
                            val propType = doc.getString("propertyType") ?: "Property"
                            val sell = doc.getString("sellPrice")
                            val rent = doc.getString("rentPrice")
                            val images = doc.get("imageUris") as? List<String> ?: emptyList()
                            val address = doc.getString("address") ?: ""

                            val view = inflater.inflate(R.layout.item_property_grid, nearbyContainer, false)
                            val iv = view.findViewById<ImageView>(R.id.propertyImage)
                            val nameTv = view.findViewById<TextView>(R.id.propertyName)
                            val priceTv = view.findViewById<TextView>(R.id.propertyPrice)
                            val locationTv = view.findViewById<TextView>(R.id.propertyLocation)

                            if (!images.isNullOrEmpty()) {
                                try {
                                    iv.load(images[0]) { placeholder(R.drawable.feature_estate1); error(R.drawable.feature_estate1) }
                                } catch (e: Exception) {
                                    iv.setImageResource(R.drawable.feature_estate1)
                                }
                            } else {
                                iv.setImageResource(R.drawable.feature_estate1)
                            }

                            nameTv.text = propType
                            priceTv.text = when {
                                !rent.isNullOrEmpty() -> "$ $rent/month"
                                !sell.isNullOrEmpty() -> "$ $sell"
                                else -> ""
                            }
                            locationTv.text = address

                            view.setOnClickListener {
                                // open property details for the selected nearby item
                                val p = Property(propType, sell, rent, (doc.getLong("bedroomCount")?:1L).toInt(), (doc.getLong("bathroomCount")?:1L).toInt(), (doc.getLong("kitchenCount")?:1L).toInt(), images, address, lat, lng, id, doc.getString("ownerId"))
                                val intent = android.content.Intent(this, PropertyDetailsActivity::class.java)
                                intent.putExtra("PROPERTY", p)
                                startActivity(intent)
                            }

                            nearbyContainer.addView(view)
                        }
                    } catch (e: Exception) {
                        // ignore malformed doc
                    }
                }
            }
            .addOnFailureListener {
                // ignore failures silently
            }
    }

    private fun loadImageGallery(property: Property) {
        val galleryContainer = findViewById<LinearLayout>(R.id.imageThumbnailsContainer)
        galleryContainer.removeAllViews()

        if (property.imageUris.size <= 1) {
            findViewById<View>(R.id.imageGallery).visibility = View.GONE
            return
        }

        property.imageUris.forEachIndexed { idx, imageUri ->
            val imgView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    100.dpToPx(),
                    100.dpToPx()
                ).apply {
                    setMargins(8.dpToPx(), 0, 8.dpToPx(), 0)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 12.dpToPx().toFloat()
                }
            }

            try {
                imgView.load(Uri.parse(imageUri)) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                }
            } catch (e: Exception) {
                imgView.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            imgView.setOnClickListener {
                val vp = findViewById<ViewPager2>(R.id.vpPropertyImages)
                vp.currentItem = idx
            }

            galleryContainer.addView(imgView)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }
}
