package com.example.propvision

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class AddPropertyActivity : AppCompatActivity() {

    private var bedroomCount = 1
    private var bathroomCount = 1
    private var kitchenCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_property)

        setupCounters()
        setupNavigation()
        setupMapIntegration()

        findViewById<Button>(R.id.finishBtn).setOnClickListener {
            validateAndFinish()
        }
    }

    private fun setupCounters() {
        // Bedroom
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

        // Bathroom
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

        // Kitchen
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
        val mapAction = View.OnClickListener {
            val gmmIntentUri = Uri.parse("geo:31.5204,74.3587?q=properties")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(packageManager) != null) {
                startActivity(mapIntent)
            } else {
                // Fallback to browser
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/properties/@31.5204,74.3587,15z"))
                startActivity(browserIntent)
            }
        }

        findViewById<View>(R.id.mapPlaceholder).setOnClickListener(mapAction)
        findViewById<View>(R.id.openMapBtn).setOnClickListener(mapAction)
    }

    private fun validateAndFinish() {
        val sellPrice = findViewById<EditText>(R.id.sellPriceInput).text.toString()
        val rentPrice = findViewById<EditText>(R.id.rentPriceInput).text.toString()

        if (sellPrice.isEmpty() && rentPrice.isEmpty()) {
            Toast.makeText(this, "Please enter at least one price", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Listing added successfully!", Toast.LENGTH_LONG).show()
        finish()
    }
}
