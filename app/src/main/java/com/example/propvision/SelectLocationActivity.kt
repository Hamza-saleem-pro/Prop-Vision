package com.example.propvision

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Locale

class SelectLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedLatLng: LatLng? = null
    private var selectedAddress: String = "Location Selected"

    private lateinit var etSearch: EditText
    private lateinit var btnSearch: ImageView
    private lateinit var fabCurrentLocation: FloatingActionButton

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                enableMyLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_location)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        etSearch = findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)
        fabCurrentLocation = findViewById(R.id.fabCurrentLocation)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<Button>(R.id.btnConfirmLocation).setOnClickListener {
            if (selectedLatLng == null) {
                Toast.makeText(this, "Please move the map to select a location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val intent = Intent()
            intent.putExtra("latitude", selectedLatLng!!.latitude)
            intent.putExtra("longitude", selectedLatLng!!.longitude)
            intent.putExtra("address", selectedAddress)
            setResult(RESULT_OK, intent)
            finish()
        }

        findViewById<android.view.View>(R.id.backBtn).setOnClickListener {
            finish()
        }

        setupSearch()
        
        fabCurrentLocation.setOnClickListener {
            checkLocationPermission()
        }
    }

    private fun setupSearch() {
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocation()
                true
            } else false
        }

        btnSearch.setOnClickListener {
            searchLocation()
        }
    }

    private fun searchLocation() {
        val locationName = etSearch.text.toString()
        if (locationName.isNotEmpty()) {
            val geocoder = Geocoder(this, Locale.getDefault())
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(locationName, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val latLng = LatLng(address.latitude, address.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    selectedLatLng = latLng
                    
                    // Format to "City, Country"
                    val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                    val country = address.countryName ?: ""
                    selectedAddress = if (city.isNotEmpty() && country.isNotEmpty()) "$city, $country" else city.ifEmpty { country }
                    if (selectedAddress.isEmpty()) selectedAddress = address.getAddressLine(0)
                    
                    etSearch.setText(selectedAddress)
                } else {
                    Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error searching location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Set Lahore as default start point
        val startLocation = LatLng(31.5204, 74.3587)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLocation, 15f))
        
        selectedLatLng = startLocation
        updateAddress(startLocation)

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true

        mMap.setOnCameraIdleListener {
            val center = mMap.cameraPosition.target
            selectedLatLng = center
            updateAddress(center)
        }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (!::mMap.isInitialized) return
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = false // We use our own FAB

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                selectedLatLng = currentLatLng
                updateAddress(currentLatLng)
            }
        }
    }

    private fun updateAddress(latLng: LatLng) {
        Thread {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    
                    // Format to "City, Country"
                    val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                    val country = address.countryName ?: ""
                    selectedAddress = if (city.isNotEmpty() && country.isNotEmpty()) "$city, $country" else city.ifEmpty { country }
                    if (selectedAddress.isEmpty()) selectedAddress = address.getAddressLine(0)

                    runOnUiThread {
                        etSearch.setText(selectedAddress)
                    }
                }
            } catch (e: Exception) {
                // Handle exception
            }
        }.start()
    }
}
