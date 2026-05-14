package com.zibete.driverassistant.overlay

import com.zibete.driverassistant.calculator.TripDecisionResult

fun TripDecisionResult.hasCompleteOverlayData(): Boolean {
    return fareAmount != null &&
        totalKm != null &&
        totalMinutes != null &&
        arsPerKm != null &&
        arsPerHour != null
}
