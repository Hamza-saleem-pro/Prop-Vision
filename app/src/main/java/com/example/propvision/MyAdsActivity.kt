package com.example.propvision

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyAdsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adsContainer: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_ads)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        adsContainer = findViewById(R.id.adsContainer)

        setupNavigation()

        findViewById<View>(R.id.backBtn).setOnClickListener {
            finish()
        }

        loadMyAds()
    }

    override fun onResume() {
        super.onResume()
        // Refresh list in case of edits/deletes
        loadMyAds()
    }

    private fun loadMyAds() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("properties").whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                adsContainer.removeAllViews()
                val inflater = LayoutInflater.from(this)
                for (doc in snapshot.documents) {
                    val view = inflater.inflate(R.layout.item_my_ad, adsContainer, false)
                    val iv = view.findViewById<ImageView>(R.id.propertyImage)
                    val tvName = view.findViewById<TextView>(R.id.propertyName)
                    val tvLocation = view.findViewById<TextView>(R.id.propertyLocation)
                    val tvPrice = view.findViewById<TextView>(R.id.propertyPrice)

                    val images = doc.get("imageUris") as? List<String>
                    if (!images.isNullOrEmpty()) {
                        iv.load(images[0]) { placeholder(R.drawable.feature_estate1) }
                    }

                    tvName.text = doc.getString("propertyType") ?: "Property"
                    tvLocation.text = doc.getString("address") ?: ""
                    val rent = doc.getString("rentPrice")
                    val sell = doc.getString("sellPrice")
                    tvPrice.text = when {
                        !rent.isNullOrEmpty() -> "Rs. $rent/month"
                        !sell.isNullOrEmpty() -> "Rs. $sell"
                        else -> "N/A"
                    }

                    // Edit action - when card clicked
                    view.setOnClickListener {
                        val intent = Intent(this, AddPropertyActivity::class.java)
                        intent.putExtra("EDIT_PROPERTY_ID", doc.id)
                        startActivity(intent)
                    }

                    // Long click -> delete confirmation
                    view.setOnLongClickListener {
                        android.app.AlertDialog.Builder(this)
                            .setTitle("Delete Ad")
                            .setMessage("Are you sure you want to delete this ad?")
                            .setPositiveButton("Delete") { _, _ ->
                                db.collection("properties").document(doc.id).delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Ad deleted", Toast.LENGTH_SHORT).show()
                                        loadMyAds()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                        true
                    }

                    adsContainer.addView(view)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load ads: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.nav_home).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
        }

        findViewById<View>(R.id.nav_explore).setOnClickListener {
            startActivity(Intent(this, ExplorePropertiesActivity::class.java))
        }

        findViewById<View>(R.id.nav_add).setOnClickListener {
            startActivity(Intent(this, AddPropertyActivity::class.java))
        }

        findViewById<View>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, UserProfileActivity::class.java))
        }
    }
}
