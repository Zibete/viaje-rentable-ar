package com.zibete.driverassistant.overlay

import com.zibete.driverassistant.calculator.DriverDecision

data class OverlayCardState(
    val decision: DriverDecision,
    val fareText: String,
    val arsPerHourText: String,
    val arsPerKmText: String,
    val totalTimeText: String,
    val totalKmText: String,
    val shortReason: String? = null
)

