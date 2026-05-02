package com.example.propvision

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.LinearLayout
import android.view.LayoutInflater
import android.widget.TextView
import android.net.Uri
import coil.load

class ExplorePropertiesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        setContentView(R.layout.activity_explore_properties)

        setupNavigation()

        // populate search results from Firestore
        val db = FirebaseFirestore.getInstance()
        val listViewLayout = findViewById<LinearLayout>(R.id.listViewLayout)
        val gridViewLayout = findViewById<androidx.gridlayout.widget.GridLayout>(R.id.gridViewLayout)
        listViewLayout.removeAllViews()
        gridViewLayout.removeAllViews()
        val inflater = LayoutInflater.from(this)
        db.collection("properties").get().addOnSuccessListener { snap ->
            for (doc in snap.documents) {
                try {
                    val prop = com.example.propvision.Property(
                        propertyType = doc.getString("propertyType") ?: "Property",
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
                        ownerId = doc.getString("ownerId")
                    )

                    // inflate list item
                    val listItem = inflater.inflate(R.layout.item_property_list, listViewLayout, false)
                    val iv = listItem.findViewById<ImageView>(R.id.propertyImage)
                    val tvName = listItem.findViewById<TextView>(R.id.propertyName)
                    val tvAddress = listItem.findViewById<TextView>(R.id.propertyLocation)
                    if (prop.imageUris.isNotEmpty()) iv.load(Uri.parse(prop.imageUris[0]))
                    tvName.text = prop.propertyType
                    tvAddress.text = prop.address
                    listItem.setOnClickListener {
                        val intent = android.content.Intent(this, PropertyDetailsActivity::class.java)
                        intent.putExtra("PROPERTY", prop)
                        startActivity(intent)
                    }
                    listViewLayout.addView(listItem)

                    // inflate grid item
                    val gridItem = inflater.inflate(R.layout.item_property_grid, gridViewLayout, false)
                    val ivg = gridItem.findViewById<ImageView>(R.id.propertyImage)
                    val tvgName = gridItem.findViewById<TextView>(R.id.propertyName)
                    val tvgLoc = gridItem.findViewById<TextView>(R.id.propertyLocation)
                    if (prop.imageUris.isNotEmpty()) ivg.load(Uri.parse(prop.imageUris[0]))
                    tvgName.text = prop.propertyType
                    tvgLoc.text = prop.address
                    gridItem.setOnClickListener {
                        val intent = android.content.Intent(this, PropertyDetailsActivity::class.java)
                        intent.putExtra("PROPERTY", prop)
                        startActivity(intent)
                    }
                    gridViewLayout.addView(gridItem)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        findViewById<View>(R.id.backBtn).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.filterBtn).setOnClickListener {
            Toast.makeText(this, "Filter options coming soon", Toast.LENGTH_SHORT).show()
        }

        val gridViewBtn = findViewById<ImageView>(R.id.gridViewBtn)
        val listViewBtn = findViewById<ImageView>(R.id.listViewBtn)

        gridViewBtn.setOnClickListener {
            listViewLayout.visibility = View.GONE
            gridViewLayout.visibility = View.VISIBLE
            gridViewBtn.setBackgroundResource(R.drawable.social_button_bg) // Simplified toggle
            listViewBtn.background = null
        }

        listViewBtn.setOnClickListener {
            listViewLayout.visibility = View.VISIBLE
            gridViewLayout.visibility = View.GONE
            listViewBtn.setBackgroundResource(R.drawable.social_button_bg)
            gridViewBtn.background = null
        }
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
