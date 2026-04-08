package com.example.propvision

import java.io.Serializable

data class Property(
    val propertyType: String, // House, Apartment, Villa, Flat
    val sellPrice: String?,
    val rentPrice: String?,
    val bedroomCount: Int,
    val bathroomCount: Int,
    val kitchenCount: Int,
    val imageUris: List<String>,
    val address: String,
    val latitude: Double,
    val longitude: Double
) : Serializable
