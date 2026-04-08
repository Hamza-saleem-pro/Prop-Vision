package com.example.propvision

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
                propertyList.add(0, it) // Add to the top of the list
                updateNewListingsUI()
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
                Toast.makeText(this, "Filtering houses near: $it", Toast.LENGTH_SHORT).show()
                updateNewListingsUI()
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
        
        val isAdmin = intent.getBooleanExtra("IS_ADMIN", false)
        if (isAdmin) {
            findViewById<TextView>(R.id.greeting).text = "Admin Dashboard"
            findViewById<TextView>(R.id.discoverTitle).text = "Manage All Properties"
            Toast.makeText(this, "Welcome to Admin Mode", Toast.LENGTH_LONG).show()
        }

        // Location Picker logic
        findViewById<View>(R.id.locationPicker).setOnClickListener {
            val intent = Intent(this, SelectLocationActivity::class.java)
            locationPickerLauncher.launch(intent)
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

        findViewById<View>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, CreateProfileActivity::class.java))
        }

        // Search Bar Logic (Simple Toast for now)
        findViewById<View>(R.id.searchBar).setOnClickListener {
            Toast.makeText(this, "Search functionality coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateNewListingsUI() {
        newListingsContainer.removeAllViews()
        
        // Filter properties based on selected location (simple string matching for simulation)
        val filteredList = if (currentFilterLocation == null) {
            propertyList
        } else {
            propertyList.filter { 
                it.address.contains(currentFilterLocation!!, ignoreCase = true) || 
                currentFilterLocation!!.contains(it.address, ignoreCase = true)
            }
        }

        findViewById<View>(R.id.newListingsSection).visibility = if (filteredList.isEmpty()) View.GONE else View.VISIBLE

        val inflater = LayoutInflater.from(this)
        for (property in filteredList) {
            val itemView = inflater.inflate(R.layout.item_property_list, newListingsContainer, false)
            
            val ivImage = itemView.findViewById<ImageView>(R.id.propertyImage)
            val tvName = itemView.findViewById<TextView>(R.id.propertyName)
            val tvLocation = itemView.findViewById<TextView>(R.id.propertyLocation)
            val tvPrice = itemView.findViewById<TextView>(R.id.propertyPrice)

            // Load the first image
            if (property.imageUris.isNotEmpty()) {
                ivImage.load(Uri.parse(property.imageUris[0]))
            }

            // Construct title from type and features
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
