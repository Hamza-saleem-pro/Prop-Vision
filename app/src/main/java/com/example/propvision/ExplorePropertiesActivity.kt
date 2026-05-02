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

        setupNavigation()
        setupSearch()
        setupViewToggles()

        findViewById<View>(R.id.backBtn).setOnClickListener { finish() }
        findViewById<View>(R.id.filterBtn).setOnClickListener {
            Toast.makeText(this, "Filter options coming soon", Toast.LENGTH_SHORT).show()
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
                                avgRating = doc.getDouble("avgRating") ?: 0.0,
                                ratingCount = (doc.getLong("ratingCount") ?: 0L).toInt()
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

    private fun renderProperties() {
        val filteredList = propertyList.filter { PropertySearchUtils.matchesQuery(it, currentQuery) }
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

        if (property.imageUris.isNotEmpty()) {
            iv.load(property.imageUris[0].toUri())
        }
        tvName.text = property.propertyType
        tvAddress.text = property.address
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
