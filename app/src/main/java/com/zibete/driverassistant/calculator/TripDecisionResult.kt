package com.zibete.driverassistant.calculator

data class TripDecisionResult(
    val decision: DriverDecision,
    val fareAmount: Double?,
    val arsPerKm: Double?,
    val arsPerHour: Double?,
    val estimatedCost: Double?,
    val estimatedNetProfit: Double?,
    val totalKm: Double?,
    val totalMinutes: Double?,
    val rejectionReasons: List<String>,
    val reviewReasons: List<String>
)

