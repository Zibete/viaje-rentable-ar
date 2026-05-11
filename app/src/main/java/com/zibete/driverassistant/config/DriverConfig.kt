package com.zibete.driverassistant.config

import com.zibete.driverassistant.zones.AvoidZoneRule

data class DriverConfig(
    val minArsPerKm: Double,
    val minArsPerHour: Double,
    val minNetProfit: Double,
    val costPerKm: Double,
    val costPerMinute: Double,
    val maxPickupKm: Double,
    val maxPickupMinutes: Double,
    val reviewTolerancePercent: Double,
    val rejectIfUnknownFare: Boolean,
    val rejectIfUnknownDistance: Boolean,
    val rejectIfAvoidZoneDetected: Boolean,
    val enabledPlatforms: Map<String, Boolean>,
    val avoidZones: List<AvoidZoneRule>
) {
    companion object {
        fun default(): DriverConfig = DriverConfig(
            minArsPerKm = 600.0,
            minArsPerHour = 7000.0,
            minNetProfit = 1000.0,
            costPerKm = 280.0,
            costPerMinute = 30.0,
            maxPickupKm = 3.0,
            maxPickupMinutes = 10.0,
            reviewTolerancePercent = 10.0,
            rejectIfUnknownFare = true,
            rejectIfUnknownDistance = false,
            rejectIfAvoidZoneDetected = true,
            enabledPlatforms = mapOf(
                "uber" to true,
                "didi" to true,
                "cabify" to true
            ),
            avoidZones = emptyList()
        )
    }
}

