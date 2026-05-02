package com.example.propvision

object CountryCityData {
    private val countryCityMap = mapOf(
        "Pakistan" to listOf(
            "Karachi", "Lahore", "Islamabad", "Rawalpindi", "Faisalabad",
            "Multan", "Hyderabad", "Peshawar", "Quetta", "Sukkur"
        ),
        "India" to listOf(
            "Mumbai", "Delhi", "Bangalore", "Hyderabad", "Chennai",
            "Kolkata", "Pune", "Ahmedabad", "Jaipur", "Lucknow"
        ),
        "United States" to listOf(
            "New York", "Los Angeles", "Chicago", "Houston", "Phoenix",
            "Philadelphia", "San Antonio", "San Diego", "Dallas", "San Jose"
        ),
        "United Kingdom" to listOf(
            "London", "Manchester", "Birmingham", "Leeds", "Glasgow",
            "Liverpool", "Newcastle", "Sheffield", "Bristol", "Edinburgh"
        ),
        "Canada" to listOf(
            "Toronto", "Vancouver", "Montreal", "Calgary", "Ottawa",
            "Edmonton", "Mississauga", "Winnipeg", "Quebec City", "Hamilton"
        ),
        "Australia" to listOf(
            "Sydney", "Melbourne", "Brisbane", "Perth", "Adelaide",
            "Gold Coast", "Canberra", "Newcastle", "Wollongong", "Logan City"
        ),
        "United Arab Emirates" to listOf(
            "Dubai", "Abu Dhabi", "Sharjah", "Ajman", "Ras Al Khaimah",
            "Fujairah", "Umm Al Quwain", "Al Ain", "Jebel Ali", "Deira"
        ),
        "Saudi Arabia" to listOf(
            "Riyadh", "Jeddah", "Mecca", "Medina", "Dammam",
            "Khobar", "Abha", "Tabuk", "Buraydah", "Al-Qassim"
        ),
        "Malaysia" to listOf(
            "Kuala Lumpur", "George Town", "Johor Bahru", "Ipoh", "Kota Kinabalu",
            "Kuching", "Petaling Jaya", "Subang Jaya", "Shah Alam", "Klang"
        ),
        "Singapore" to listOf(
            "Central", "East", "North", "West", "Northeast"
        )
    )

    fun getCountries(): List<String> {
        return countryCityMap.keys.sorted()
    }

    fun getCitiesByCountry(country: String): List<String> {
        return countryCityMap[country] ?: emptyList()
    }
}

