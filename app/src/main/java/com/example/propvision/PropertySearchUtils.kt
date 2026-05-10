package com.example.propvision

import java.util.Locale

object PropertySearchUtils {
    fun matchesQuery(property: Property, query: String): Boolean {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank()) return true

        return searchableText(property).contains(normalizedQuery)
    }

    private fun searchableText(property: Property): String {
        return buildString {
            append(property.propertyType)
            append(' ')
            append(property.address)
            append(' ')
            append(property.description.orEmpty())
            append(' ')
            append(property.ownerName.orEmpty())
            append(' ')
            append(property.sellPrice.orEmpty())
            append(' ')
            append(property.rentPrice.orEmpty())
            append(' ')
            append(property.bedroomCount)
            append(' ')
            append(property.bathroomCount)
            append(' ')
            append(property.kitchenCount)
            append(' ')
            append(property.ownerId.orEmpty())
            append(' ')
            append(property.id.orEmpty())
            append(' ')
            append(property.avgRating)
            append(' ')
            append(property.ratingCount)
        }.let(::normalize)
    }

    private fun normalize(value: String): String {
        return value
            .lowercase(Locale.getDefault())
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

