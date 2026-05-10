package com.example.propvision

import android.content.Intent
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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import coil.load
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import androidx.core.net.toUri

class ExplorePropertiesActivity : AppCompatActivity() {

    private lateinit var listViewLayout: LinearLayout
    private lateinit var gridViewLayout: androidx.gridlayout.widget.GridLayout
    private lateinit var etSearchInput: EditText
    private lateinit var tvResultsSummary: TextView
    private lateinit var gridViewBtn: ImageView
    private lateinit var listViewBtn: ImageView

    private val propertyList = mutableListOf<Property>()
    private var showingGrid = false
    private var currentQuery = ""
    private var selectedCategory = "All"
    
    private var filterType: String? = null
    private var filterMinPrice: Long? = null
    private var filterMaxPrice: Long? = null
    private var filterLocation: String? = null
    private var filterListingType: String? = null
    private var filterBedrooms: Int? = null

    private lateinit var chipHouse: TextView
    private lateinit var chipApartment: TextView
    private lateinit var chipVilla: TextView
    private lateinit var chipFlat: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        setContentView(R.layout.activity_explore_properties)

        listViewLayout = findViewById(R.id.listViewLayout)
        gridViewLayout = findViewById(R.id.gridViewLayout)
        etSearchInput = findViewById(R.id.etSearchInput)
        tvResultsSummary = findViewById(R.id.tvResultsSummary)
        gridViewBtn = findViewById(R.id.gridViewBtn)
        listViewBtn = findViewById(R.id.listViewBtn)
        
        chipHouse = findViewById(R.id.chipHouse)
        chipApartment = findViewById(R.id.chipApartment)
        chipVilla = findViewById(R.id.chipVilla)
        chipFlat = findViewById(R.id.chipFlat)

        setupNavigation()
        setupSearch()
        setupViewToggles()
        setupCategoryChips()

        findViewById<View>(R.id.backBtn).setOnClickListener { finish() }
        findViewById<View>(R.id.filterBtn).setOnClickListener {
            showFilterBottomSheet()
        }
        findViewById<View>(R.id.btnSearch).setOnClickListener {
            currentQuery = etSearchInput.text.toString()
            renderProperties()
        }

        loadProperties()
    }

    private fun loadProperties() {
        FirebaseFirestore.getInstance().collection("properties")
            .get()
            .addOnSuccessListener { snapshot ->
                propertyList.clear()
                snapshot.documents.forEach { doc ->
                    try {
                        propertyList.add(
                            Property(
                                propertyType = doc.getString("propertyType") ?: "Property",
                                sellPrice = doc.getString("sellPrice"),
                                rentPrice = doc.getString("rentPrice"),
                                bedroomCount = (doc.getLong("bedroomCount") ?: 1L).toInt(),
                                bathroomCount = (doc.getLong("bathroomCount") ?: 1L).toInt(),
                                kitchenCount = (doc.getLong("kitchenCount") ?: 1L).toInt(),
                                imageUris = extractImageUris(doc.get("imageUris")),
                                address = doc.getString("address") ?: "",
                                latitude = doc.getDouble("latitude") ?: 0.0,
                                longitude = doc.getDouble("longitude") ?: 0.0,
                                id = doc.id,
                                ownerId = doc.getString("ownerId"),
                                ownerName = doc.getString("ownerName"),
                                ownerEmail = doc.getString("ownerEmail"),
                                ownerPhone = doc.getString("ownerPhone"),
                                description = doc.getString("description"),
                                avgRating = doc.getDouble("avgRating") ?: 0.0,
                                ratingCount = (doc.getLong("ratingCount") ?: 0L).toInt(),
                                timestamp = doc.getLong("timestamp")
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                renderProperties()
            }
    }

    private fun setupSearch() {
        etSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString().orEmpty()
                renderProperties()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }


    private fun setupViewToggles() {
        gridViewBtn.setOnClickListener {
            showingGrid = true
            renderProperties()
        }

        listViewBtn.setOnClickListener {
            showingGrid = false
            renderProperties()
        }
    }

    private fun setupCategoryChips() {
        val chips = mapOf(
            "House" to chipHouse,
            "Apartment" to chipApartment,
            "Villa" to chipVilla,
            "Flat" to chipFlat
        )

        chips.forEach { (category, view) ->
            view.setOnClickListener {
                selectedCategory = if (selectedCategory == category) "All" else category
                updateChipsUI()
                renderProperties()
            }
        }
    }

    private fun updateChipsUI() {
        val chips = mapOf(
            "House" to chipHouse,
            "Apartment" to chipApartment,
            "Villa" to chipVilla,
            "Flat" to chipFlat
        )

        chips.forEach { (category, view) ->
            if (category == selectedCategory) {
                view.setBackgroundResource(R.drawable.social_button_bg)
                view.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#234F68"))
                view.setTextColor(android.graphics.Color.WHITE)
            } else {
                view.setBackgroundResource(R.drawable.social_button_bg)
                view.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                view.setTextColor(android.graphics.Color.parseColor("#2D3A5F"))
            }
        }
    }

    private fun showFilterBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_filter, null)
        dialog.setContentView(view)

        val chipGroupType = view.findViewById<ChipGroup>(R.id.chipGroupType)
        val etMinPrice = view.findViewById<EditText>(R.id.etMinPrice)
        val etMaxPrice = view.findViewById<EditText>(R.id.etMaxPrice)
        val etLocationFilter = view.findViewById<EditText>(R.id.etLocationFilter)
        val chipGroupListingType = view.findViewById<ChipGroup>(R.id.chipGroupListingType)
        val chipGroupBedrooms = view.findViewById<ChipGroup>(R.id.chipGroupBedrooms)
        val btnApply = view.findViewById<View>(R.id.btnApplyFilter)
        val btnReset = view.findViewById<View>(R.id.btnReset)

        // Pre-fill existing filters if any
        filterMinPrice?.let { etMinPrice.setText(it.toString()) }
        filterMaxPrice?.let { etMaxPrice.setText(it.toString()) }
        filterLocation?.let { etLocationFilter.setText(it) }
        
        // Select chips based on current filter state
        filterType?.let { type ->
            for (i in 0 until chipGroupType.childCount) {
                val chip = chipGroupType.getChildAt(i) as Chip
                if (chip.text.toString().equals(type, ignoreCase = true)) {
                    chip.isChecked = true
                    break
                }
            }
        }

        filterListingType?.let { listingType ->
            for (i in 0 until chipGroupListingType.childCount) {
                val chip = chipGroupListingType.getChildAt(i) as Chip
                if (chip.text.toString().equals(listingType, ignoreCase = true)) {
                    chip.isChecked = true
                    break
                }
            }
        }

        filterBedrooms?.let { beds ->
            val bedText = if (beds == 4) "4+" else beds.toString()
            for (i in 0 until chipGroupBedrooms.childCount) {
                val chip = chipGroupBedrooms.getChildAt(i) as Chip
                if (chip.text.toString() == bedText) {
                    chip.isChecked = true
                    break
                }
            }
        }

        btnReset.setOnClickListener {
            filterType = null
            filterMinPrice = null
            filterMaxPrice = null
            filterLocation = null
            filterListingType = null
            filterBedrooms = null
            dialog.dismiss()
            renderProperties()
        }

        btnApply.setOnClickListener {
            val selectedTypeId = chipGroupType.checkedChipId
            filterType = if (selectedTypeId != View.NO_ID) {
                view.findViewById<Chip>(selectedTypeId).text.toString()
            } else null

            filterMinPrice = etMinPrice.text.toString().toLongOrNull()
            filterMaxPrice = etMaxPrice.text.toString().toLongOrNull()
            filterLocation = etLocationFilter.text.toString().takeIf { it.isNotBlank() }

            val selectedListingTypeId = chipGroupListingType.checkedChipId
            filterListingType = if (selectedListingTypeId != View.NO_ID) {
                view.findViewById<Chip>(selectedListingTypeId).text.toString()
            } else null

            val selectedBedroomId = chipGroupBedrooms.checkedChipId
            filterBedrooms = if (selectedBedroomId != View.NO_ID) {
                val text = view.findViewById<Chip>(selectedBedroomId).text.toString()
                if (text == "4+") 4 else text.toIntOrNull()
            } else null

            dialog.dismiss()
            renderProperties()
        }

        dialog.show()
    }

    private fun renderProperties() {
        val filteredList = propertyList.filter { 
            val matchesSearch = PropertySearchUtils.matchesQuery(it, currentQuery)
            val matchesCategory = if (selectedCategory == "All") true else it.propertyType.equals(selectedCategory, ignoreCase = true)
            
            // Advanced Filters
            val matchesType = if (filterType == null) true else it.propertyType.equals(filterType, ignoreCase = true)
            
            val price = it.rentPrice?.replace(",", "")?.toLongOrNull() ?: it.sellPrice?.replace(",", "")?.toLongOrNull() ?: 0L
            val matchesMinPrice = if (filterMinPrice == null) true else price >= filterMinPrice!!
            val matchesMaxPrice = if (filterMaxPrice == null) true else price <= filterMaxPrice!!
            
            val matchesLocation = if (filterLocation == null) true else it.address.contains(filterLocation!!, ignoreCase = true)
            
            val matchesListingType = when (filterListingType) {
                "For Rent" -> it.rentPrice != null
                "For Sale" -> it.sellPrice != null
                "For Rent & Sale" -> it.rentPrice != null && it.sellPrice != null
                else -> true
            }
            
            val matchesBedrooms = if (filterBedrooms == null) true else {
                if (filterBedrooms == 4) it.bedroomCount >= 4 else it.bedroomCount == filterBedrooms
            }

            matchesSearch && matchesCategory && matchesType && matchesMinPrice && matchesMaxPrice && matchesLocation && matchesListingType && matchesBedrooms
        }
        tvResultsSummary.text = getString(R.string.explore_search_summary_format, filteredList.size)

        listViewLayout.removeAllViews()
        gridViewLayout.removeAllViews()

        val inflater = LayoutInflater.from(this)
        filteredList.forEach { prop ->
            listViewLayout.addView(createListItem(inflater, prop))
            gridViewLayout.addView(createGridItem(inflater, prop))
        }

        listViewLayout.visibility = if (showingGrid) View.GONE else View.VISIBLE
        gridViewLayout.visibility = if (showingGrid) View.VISIBLE else View.GONE

        gridViewBtn.setBackgroundResource(if (showingGrid) R.drawable.social_button_bg else android.R.color.transparent)
        listViewBtn.setBackgroundResource(if (showingGrid) android.R.color.transparent else R.drawable.social_button_bg)
    }

    private fun createListItem(inflater: LayoutInflater, property: Property): View {
        val listItem = inflater.inflate(R.layout.item_property_list, listViewLayout, false)
        bindPropertyItem(listItem, property)
        return listItem
    }

    private fun createGridItem(inflater: LayoutInflater, property: Property): View {
        val gridItem = inflater.inflate(R.layout.item_property_grid, gridViewLayout, false)
        bindPropertyItem(gridItem, property)
        return gridItem
    }

    private fun bindPropertyItem(itemView: View, property: Property) {
        val iv = itemView.findViewById<ImageView>(R.id.propertyImage)
        val tvName = itemView.findViewById<TextView>(R.id.propertyName)
        val tvAddress = itemView.findViewById<TextView>(R.id.propertyLocation)
        val tvPrice = itemView.findViewById<TextView>(R.id.propertyPrice)
        val tvOwnerName = itemView.findViewById<TextView>(R.id.ownerName)
        val tvOwnerPhone = itemView.findViewById<TextView>(R.id.ownerPhone)
        val tvOwnerEmail = itemView.findViewById<TextView>(R.id.ownerEmail)
        val tvStatus = itemView.findViewById<TextView>(R.id.propertyStatus)
        val btnEdit = itemView.findViewById<ImageView>(R.id.btnEditProperty)

        if (property.imageUris.isNotEmpty()) {
            iv.load(property.imageUris[0])
        }
        tvName.text = property.propertyType
        tvAddress.text = property.address
        tvOwnerName?.text = property.ownerName ?: "Owner"
        tvOwnerPhone?.text = property.ownerPhone ?: ""
        tvOwnerEmail?.text = property.ownerEmail ?: ""

        // Dynamic Status Label Logic
        when {
            !property.rentPrice.isNullOrEmpty() && !property.sellPrice.isNullOrEmpty() -> {
                tvStatus?.text = "For Rent & Sale"
            }
            !property.sellPrice.isNullOrEmpty() -> {
                tvStatus?.text = "For Sale"
            }
            !property.rentPrice.isNullOrEmpty() -> {
                tvStatus?.text = "For Rent"
            }
            else -> {
                tvStatus?.text = "N/A"
            }
        }

        val priceText = when {
            property.rentPrice != null -> "Rs.${property.rentPrice}/mo"
            property.sellPrice != null -> "Rs.${property.sellPrice}"
            else -> "N/A"
        }
        tvPrice?.text = priceText

        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId != null && currentUserId == property.ownerId) {
            btnEdit?.visibility = View.VISIBLE
            btnEdit?.setOnClickListener {
                val intent = Intent(this, AddPropertyActivity::class.java)
                intent.putExtra("EDIT_PROPERTY_ID", property.id)
                startActivity(intent)
            }
        } else {
            btnEdit?.visibility = View.GONE
        }

        itemView.setOnClickListener {
            startActivity(Intent(this, PropertyDetailsActivity::class.java).apply {
                putExtra("PROPERTY", property)
            })
        }
    }

    private fun extractImageUris(value: Any?): List<String> {
        return (value as? List<*>)
            ?.mapNotNull { it as? String }
            ?: emptyList()
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.nav_home).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }

        findViewById<View>(R.id.nav_add).setOnClickListener {
            startActivity(Intent(this, AddPropertyActivity::class.java))
        }

        findViewById<View>(R.id.nav_market).setOnClickListener {
            startActivity(Intent(this, MyAdsActivity::class.java))
        }

        findViewById<View>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }
    }
}
