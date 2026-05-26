package com.zibete.driverassistant.home

data class HomeLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val label: String?,
    val resolvedAtMillis: Long?,
    val provider: String?
) {
    fun hasValidCoordinates(): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
}
