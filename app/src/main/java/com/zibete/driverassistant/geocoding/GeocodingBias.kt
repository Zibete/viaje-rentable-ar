package com.zibete.driverassistant.geocoding

data class GeocodingBias(
    val lowerLeftLatitude: Double,
    val lowerLeftLongitude: Double,
    val upperRightLatitude: Double,
    val upperRightLongitude: Double,
    val countryHint: String
) {
    companion object {
        val ARGENTINA = GeocodingBias(
            lowerLeftLatitude = -55.2,
            lowerLeftLongitude = -73.7,
            upperRightLatitude = -21.7,
            upperRightLongitude = -53.6,
            countryHint = "Argentina"
        )
    }
}
