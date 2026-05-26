package com.zibete.driverassistant.calculator

data class TripOfferInput(
    val fareAmount: Double?,
    val pickupKm: Double?,
    val tripKm: Double?,
    val pickupMinutes: Double?,
    val tripMinutes: Double?,
    val platform: String?,
    val destinationText: String? = null,
    val rawText: String? = null
)
