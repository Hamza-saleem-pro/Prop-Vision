package com.example.propvision

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import coil.load
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var newListingsContainer: LinearLayout
    private lateinit var tvSelectedLocation: TextView
    private lateinit var newListingsLabel: TextView
    private lateinit var notificationBadge: View
    private lateinit var etSearchInput: EditText
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvGreeting: TextView
    private lateinit var ivHomeProfile: ImageView

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var propertiesListener: ListenerRegistration? = null
    private var selectedCategory: String = "All"

    private val propertyList = mutableListOf<Property>()
    private var currentFilterLocation: String? = null
    private var currentFilterLatitude: Double? = null
    private var currentFilterLongitude: Double? = null

    private val addPropertyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val property = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getSerializableExtra("NEW_PROPERTY", Property::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getSerializableExtra("NEW_PROPERTY") as? Property
            }
            
            property?.let {
                propertyList.add(0, it)
                updateNewListingsUI(etSearchInput.text.toString())
                updateNotificationBadge()
            }
        }
    }

    private val locationPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val address = result.data?.getStringExtra("address")
            address?.let {
                currentFilterLocation = it
                currentFilterLatitude = result.data?.getDoubleExtra("latitude", Double.NaN)
                    ?.takeIf { value -> !value.isNaN() }
                currentFilterLongitude = result.data?.getDoubleExtra("longitude", Double.NaN)
                    ?.takeIf { value -> !value.isNaN() }
                tvSelectedLocation.text = it
                newListingsLabel.text = getString(R.string.nearby_houses_format, it)
                updateNewListingsUI(etSearchInput.text.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        newListingsContainer = findViewById(R.id.newListingsContainer)
        tvSelectedLocation = findViewById(R.id.tvLocationName)
        newListingsLabel = findViewById(R.id.tvNewListingsTitle)
        notificationBadge = findViewById(R.id.notificationBadge)
        etSearchInput = findViewById(R.id.etSearchInput)
        tvGreeting = findViewById(R.id.greeting)
        ivHomeProfile = findViewById(R.id.ivHomeProfile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        loadUserData()
        setupNavigation()
        setupFilters()
        setupTopLocations()
        setupFeaturedCards()
        fetchCurrentLocation()

        // Location Picker logic
        findViewById<View>(R.id.locationPicker).setOnClickListener {
            val intent = Intent(this, SelectLocationActivity::class.java)
            locationPickerLauncher.launch(intent)
        }

        // Notification Bell logic
        findViewById<View>(R.id.notificationHub).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        // Also update the top profile card if it exists
        findViewById<View>(R.id.profileCard)?.setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }

        // Search Bar Filtering Logic
        etSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateNewListingsUI(s.toString().trim().lowercase())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Start listening to properties collection
        startPropertiesListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        propertiesListener?.remove()
    }

    private fun startPropertiesListener() {
        propertiesListener = db.collection("properties")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }

                propertyList.clear()
                snapshots?.forEach { doc ->
                    try {
                        val prop = Property(
                            propertyType = doc.getString("propertyType") ?: "House",
                            sellPrice = doc.getString("sellPrice"),
                            rentPrice = doc.getString("rentPrice"),
                            bedroomCount = (doc.getLong("bedroomCount") ?: 1L).toInt(),
                            bathroomCount = (doc.getLong("bathroomCount") ?: 1L).toInt(),
                            kitchenCount = (doc.getLong("kitchenCount") ?: 1L).toInt(),
                            imageUris = doc.get("imageUris") as? List<String> ?: emptyList(),
                            address = doc.getString("address") ?: "",
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            id = doc.id,
                            ownerId = doc.getString("ownerId"),
                            avgRating = doc.getDouble("avgRating") ?: 0.0,
                            ratingCount = (doc.getLong("ratingCount") ?: 0L).toInt()
                        )
                        propertyList.add(prop)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                runOnUiThread {
                    updateNewListingsUI()
                    updateFeaturedCards()
                }
                // If no properties present, seed some demo properties (system-owned)
                if (propertyList.isEmpty()) seedDemoProperties()
            }
    }

    private fun seedDemoProperties() {
        val demo = listOf(
            Property("House", null, "45,000", 3, 2, 1, listOf("https://picsum.photos/seed/lahore1/600/400"), "Model Town, Lahore", 31.5204, 74.3587),
            Property("Apartment", null, "60,000", 2, 2, 1, listOf("https://picsum.photos/seed/karachi1/600/400"), "Clifton, Karachi", 24.8607, 67.0011),
            Property("Villa", null, "120,000", 4, 3, 2, listOf("https://picsum.photos/seed/islamabad1/600/400"), "F-7, Islamabad", 33.6844, 73.0479),
            Property("House", null, "35,000", 2, 1, 1, listOf("https://picsum.photos/seed/faisalabad1/600/400"), "Gulberg, Faisalabad", 31.4504, 73.1350),
            Property("Apartment", null, "28,000", 1, 1, 1, listOf("https://picsum.photos/seed/peshawar1/600/400"), "University Town, Peshawar", 34.0151, 71.5249)
        )

        demo.forEach { prop ->
            val data = mapOf(
                "propertyType" to prop.propertyType,
                "sellPrice" to prop.sellPrice,
                "rentPrice" to prop.rentPrice,
                "bedroomCount" to prop.bedroomCount,
                "bathroomCount" to prop.bathroomCount,
                "kitchenCount" to prop.kitchenCount,
                "imageUris" to prop.imageUris,
                "address" to prop.address,
                "latitude" to prop.latitude,
                "longitude" to prop.longitude,
                "ownerId" to "system"
            )
            db.collection("properties").add(data)
        }
    }

    private fun setupFilters() {
        val filters = mapOf(
            "All" to findViewById<TextView>(R.id.filterAll),
            "House" to findViewById<TextView>(R.id.filterHouse),
            "Apartment" to findViewById<TextView>(R.id.filterApartment),
            "Villa" to findViewById<TextView>(R.id.filterVilla)
        )

        filters.forEach { (type, view) ->
            view?.setOnClickListener {
                selectedCategory = type
                updateFilterUI(filters)
                updateNewListingsUI(etSearchInput.text.toString())
            }
        }
    }

    private fun updateFilterUI(filters: Map<String, TextView?>) {
        filters.forEach { (type, view) ->
            if (type == selectedCategory) {
                view?.setBackgroundResource(R.drawable.social_button_bg)
                view?.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#234F68"))
                view?.setTextColor(Color.WHITE)
            } else {
                view?.setBackgroundResource(R.drawable.social_button_bg)
                view?.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
                view?.setTextColor(Color.parseColor("#2D3A5F"))
            }
        }
    }

    private fun setupTopLocations() {
        findViewById<View>(R.id.locLahore).setOnClickListener { setLocationFilter("Lahore") }
        findViewById<View>(R.id.locKarachi).setOnClickListener { setLocationFilter("Karachi") }
        findViewById<View>(R.id.locFaisalabad).setOnClickListener { setLocationFilter("Faisalabad") }
    }

    private fun setLocationFilter(city: String) {
        currentFilterLocation = city
        currentFilterLatitude = null
        currentFilterLongitude = null
        tvSelectedLocation.text = city
        newListingsLabel.text = getString(R.string.nearby_houses_format, city)
        updateNewListingsUI(etSearchInput.text.toString())
    }

    private fun setupFeaturedCards() {
        val primaryCard = findViewById<View>(R.id.featuredCardPrimary)
        val secondaryCard = findViewById<View>(R.id.featuredCardSecondary)
        val targetWidth = (Resources.getSystem().displayMetrics.widthPixels * 0.76f).toInt()
        val maxWidth = (320 * Resources.getSystem().displayMetrics.density).toInt()
        val minWidth = (240 * Resources.getSystem().displayMetrics.density).toInt()
        val bannerWidth = targetWidth.coerceIn(minWidth, maxWidth)

        listOf(primaryCard, secondaryCard).forEach { card ->
            card.layoutParams = card.layoutParams.apply { width = bannerWidth }
            card.requestLayout()
        }
    }

    private fun updateFeaturedCards() {
        val primaryCard = findViewById<View>(R.id.featuredCardPrimary)
        val secondaryCard = findViewById<View>(R.id.featuredCardSecondary)
        val ivPrimary = findViewById<ImageView>(R.id.ivFeaturedPrimaryImage)
        val ivSecondary = findViewById<ImageView>(R.id.ivFeaturedSecondaryImage)
        val tvPrimaryTitle = findViewById<TextView>(R.id.tvFeaturedPrimaryTitle)
        val tvPrimarySubtitle = findViewById<TextView>(R.id.tvFeaturedPrimarySubtitle)
        val tvSecondaryTitle = findViewById<TextView>(R.id.tvFeaturedSecondaryTitle)
        val tvSecondarySubtitle = findViewById<TextView>(R.id.tvFeaturedSecondarySubtitle)

        // pick properties with 5.0 rating (or very close)
        val topRated = propertyList.filter { it.avgRating >= 4.9 }.sortedByDescending { it.avgRating }
        if (topRated.isNotEmpty()) {
            val p = topRated[0]
            try { if (p.imageUris.isNotEmpty()) ivPrimary.load(p.imageUris[0]) } catch (e: Exception) {}
            tvPrimaryTitle.text = p.propertyType
            tvPrimarySubtitle.text = p.address
            primaryCard.setOnClickListener {
                val intent = Intent(this, PropertyDetailsActivity::class.java)
                intent.putExtra("PROPERTY", p)
                startActivity(intent)
            }
        }

        if (topRated.size > 1) {
            val p = topRated[1]
            try { if (p.imageUris.isNotEmpty()) ivSecondary.load(p.imageUris[0]) } catch (e: Exception) {}
            tvSecondaryTitle.text = p.propertyType
            tvSecondarySubtitle.text = p.address
            secondaryCard.setOnClickListener {
                val intent = Intent(this, PropertyDetailsActivity::class.java)
                intent.putExtra("PROPERTY", p)
                startActivity(intent)
            }
        }
    }

    private fun fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val city = addresses[0].locality ?: addresses[0].subAdminArea ?: "Unknown"
                        val country = addresses[0].countryName ?: ""
                        val locationString = if (country.isNotEmpty()) "$city, $country" else city
                        
                        tvSelectedLocation.text = locationString
                        // We don't necessarily filter by current location automatically on startup unless you want to
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.nav_explore).setOnClickListener {
            startActivity(Intent(this, ExplorePropertiesActivity::class.java))
        }

        findViewById<View>(R.id.nav_add).setOnClickListener {
            val intent = Intent(this, AddPropertyActivity::class.java)
            addPropertyLauncher.launch(intent)
        }

        findViewById<View>(R.id.nav_market).setOnClickListener {
            startActivity(Intent(this, MyAdsActivity::class.java))
        }

        findViewById<View>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotificationBadge()
        loadUserData()
    }

    private fun updateNotificationBadge() {
        val unreadCount = NotificationRepository.getUnreadCount()
        notificationBadge.visibility = if (unreadCount > 0) View.VISIBLE else View.GONE
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("fullName") ?: "User"
                        tvGreeting.text = "Hey, $name!"

                        val profileImageUrl = document.getString("profileImageUrl")
                        if (!profileImageUrl.isNullOrEmpty()) {
                            ivHomeProfile.load(profileImageUrl) {
                                crossfade(true)
                                placeholder(R.drawable.profile_icon_new)
                                error(R.drawable.profile_icon_new)
                                listener(
                                    onError = { _, _ ->
                                        ivHomeProfile.setImageResource(R.drawable.profile_icon_new)
                                    }
                                )
                            }
                        } else {
                            ivHomeProfile.setImageResource(R.drawable.profile_icon_new)
                        }
                    }
                }
        }
    }

    private fun updateNewListingsUI(searchQuery: String = "") {
        newListingsContainer.removeAllViews()
        
        val filteredList = propertyList.filter { property ->
            val type = property.propertyType.lowercase()
            
            val matchesSearch = when {
                searchQuery.isEmpty() -> true
                else -> property.address.lowercase().contains(searchQuery) || type.contains(searchQuery)
            }

            val matchesLocation = if (currentFilterLocation == null) {
                true
            } else {
                matchesSelectedLocation(property)
            }

            val matchesCategory = if (selectedCategory == "All") {
                true
            } else {
                property.propertyType.equals(selectedCategory, ignoreCase = true)
            }

            matchesSearch && matchesLocation && matchesCategory
        }

        findViewById<View>(R.id.newListingsSection).visibility = if (filteredList.isEmpty()) View.VISIBLE else View.VISIBLE 

        val inflater = LayoutInflater.from(this)
        var currentRow: LinearLayout? = null

        for ((index, property) in filteredList.withIndex()) {
            if (index % 2 == 0) {
                currentRow = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    orientation = LinearLayout.HORIZONTAL
                }
                newListingsContainer.addView(currentRow)
            }

            val itemView = inflater.inflate(R.layout.item_property_grid, currentRow, false)
            val params = itemView.layoutParams as LinearLayout.LayoutParams
            params.width = 0
            params.weight = 1f
            itemView.layoutParams = params

            val ivImage = itemView.findViewById<ImageView>(R.id.propertyImage)
            val tvName = itemView.findViewById<TextView>(R.id.propertyName)
            val tvLocation = itemView.findViewById<TextView>(R.id.propertyLocation)
            val tvPrice = itemView.findViewById<TextView>(R.id.propertyPrice)

            if (property.imageUris.isNotEmpty()) {
                ivImage.load(Uri.parse(property.imageUris[0]))
            }

            tvName.text = property.propertyType.replaceFirstChar { it.uppercase() }
            tvLocation.text = property.address
            
            val priceText = when {
                property.rentPrice != null -> "$${property.rentPrice}/month"
                property.sellPrice != null -> "$${property.sellPrice}"
                else -> "N/A"
            }
            tvPrice.text = priceText

            itemView.setOnClickListener {
                val intent = Intent(this, PropertyDetailsActivity::class.java)
                intent.putExtra("PROPERTY", property)
                startActivity(intent)
            }

            currentRow?.addView(itemView)
        }
        
        if (filteredList.size % 2 != 0) {
            val dummyView = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, 1).apply { weight = 1f }
            }
            currentRow?.addView(dummyView)
        }
    }

    private fun matchesSelectedLocation(property: Property): Boolean {
        val selected = currentFilterLocation ?: return true
        val normalizedAddress = property.address.lowercase(Locale.getDefault())
        if (normalizedAddress.contains(selected.lowercase(Locale.getDefault()))) {
            return true
        }

        val selectedTokens = selected
            .split(',', '-', '/')
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.length >= 3 && it != "pakistan" && it != "india" && it != "usa" }

        if (selectedTokens.any { token -> normalizedAddress.contains(token) }) {
            return true
        }

        val lat = currentFilterLatitude
        val lng = currentFilterLongitude
        return if (lat != null && lng != null) {
            isWithinRadiusKm(lat, lng, property.latitude, property.longitude, 30f)
        } else {
            false
        }
    }

    private fun isWithinRadiusKm(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
        radiusKm: Float
    ): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0] <= radiusKm * 1000f
    }
}
