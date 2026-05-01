package com.example.propvision

import android.app.Activity
import android.content.Intent
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
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import coil.load

class HomeActivity : AppCompatActivity() {

    private lateinit var newListingsContainer: LinearLayout
    private lateinit var tvSelectedLocation: TextView
    private lateinit var newListingsLabel: TextView
    private lateinit var notificationBadge: View
    private lateinit var etSearchInput: EditText
    
    private val propertyList = mutableListOf<Property>()
    private var currentFilterLocation: String? = null

    private val addPropertyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
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
        if (result.resultCode == Activity.RESULT_OK) {
            val address = result.data?.getStringExtra("address")
            address?.let {
                currentFilterLocation = it
                tvSelectedLocation.text = it
                newListingsLabel.text = "Houses near $it"
                updateNewListingsUI(etSearchInput.text.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        newListingsContainer = findViewById(R.id.newListingsContainer)
        tvSelectedLocation = findViewById(R.id.tvLocationName)
        newListingsLabel = findViewById(R.id.tvNewListingsTitle)
        notificationBadge = findViewById(R.id.notificationBadge)
        etSearchInput = findViewById(R.id.etSearchInput)

        // Location Picker logic
        findViewById<View>(R.id.locationPicker).setOnClickListener {
            val intent = Intent(this, SelectLocationActivity::class.java)
            locationPickerLauncher.launch(intent)
        }

        // Notification Bell logic
        findViewById<View>(R.id.notificationHub).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        // Bottom Navigation linking
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

        // UPDATED: Navigate to UserProfileActivity instead of CreateProfileActivity
        findViewById<View>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
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
    }

    override fun onResume() {
        super.onResume()
        updateNotificationBadge()
    }

    private fun updateNotificationBadge() {
        val unreadCount = NotificationRepository.getUnreadCount()
        notificationBadge.visibility = if (unreadCount > 0) View.VISIBLE else View.GONE
    }

    private fun updateNewListingsUI(searchQuery: String = "") {
        newListingsContainer.removeAllViews()
        
        val filteredList = propertyList.filter { property ->
            val type = property.propertyType.lowercase()
            val isSell = property.sellPrice != null
            val isRent = property.rentPrice != null
            
            val matchesSearch = when {
                searchQuery.isEmpty() -> true
                searchQuery == "apartment" -> type == "apartment"
                searchQuery == "apartment rent" -> type == "apartment" && isRent
                searchQuery == "apartment sell" -> type == "apartment" && isSell
                searchQuery == "house" -> type == "house"
                searchQuery == "house rent" -> type == "house" && isRent
                searchQuery == "house sell" -> type == "house" && isSell
                searchQuery == "villa" -> type == "villa"
                searchQuery == "villa rent" -> type == "villa" && isRent
                searchQuery == "villa sell" -> type == "villa" && isSell
                searchQuery == "flat" -> type == "flat"
                searchQuery == "flat rent" -> type == "flat" && isRent
                searchQuery == "flat sell" -> type == "flat" && isSell
                else -> property.address.lowercase().contains(searchQuery) || type.contains(searchQuery)
            }

            val matchesLocation = if (currentFilterLocation == null) {
                true
            } else {
                property.address.contains(currentFilterLocation!!, ignoreCase = true) ||
                currentFilterLocation!!.contains(property.address, ignoreCase = true)
            }

            matchesSearch && matchesLocation
        }

        findViewById<View>(R.id.newListingsSection).visibility = if (filteredList.isEmpty()) View.GONE else View.VISIBLE

        val inflater = LayoutInflater.from(this)
        for (property in filteredList) {
            val itemView = inflater.inflate(R.layout.item_property_list, newListingsContainer, false)
            
            val ivImage = itemView.findViewById<ImageView>(R.id.propertyImage)
            val tvName = itemView.findViewById<TextView>(R.id.propertyName)
            val tvLocation = itemView.findViewById<TextView>(R.id.propertyLocation)
            val tvPrice = itemView.findViewById<TextView>(R.id.propertyPrice)

            if (property.imageUris.isNotEmpty()) {
                ivImage.load(Uri.parse(property.imageUris[0]))
            }

            tvName.text = "${property.propertyType} - ${property.bedroomCount} Bed"
            tvLocation.text = property.address
            
            val priceText = when {
                property.sellPrice != null && property.rentPrice != null -> 
                    "Rs. ${property.sellPrice} (Sale) / Rs. ${property.rentPrice} (Rent)"
                property.sellPrice != null -> "Rs. ${property.sellPrice} (For Sale)"
                property.rentPrice != null -> "Rs. ${property.rentPrice}/month (Rent)"
                else -> "Contact for Price"
            }
            tvPrice.text = priceText

            newListingsContainer.addView(itemView)
        }
    }
}
