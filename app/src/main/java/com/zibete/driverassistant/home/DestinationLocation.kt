package com.zibete.driverassistant.home

data class DestinationLocation(
    val latitude: Double,
    val longitude: Double,
    val sourceText: String,
    val confidence: Double?
) {
    fun hasValidCoordinates(): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
}
