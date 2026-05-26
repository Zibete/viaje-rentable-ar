package com.zibete.driverassistant.location

data class DriverLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val capturedAtMillis: Long?
) {
    fun hasValidCoordinates(): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
}
