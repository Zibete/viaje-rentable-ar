package com.zibete.driverassistant.home

fun HomeDirectionResult.toDiagnosticShortLabel(): String {
    return when (status) {
        HomeDirectionStatus.AVAILABLE -> shortLabel
        HomeDirectionStatus.MISSING_HOME_LOCATION -> "Casa ? sin domicilio"
        HomeDirectionStatus.MISSING_CURRENT_LOCATION -> "Casa ? sin ubicacion"
        HomeDirectionStatus.MISSING_DESTINATION_TEXT -> "Casa ? sin destino"
        HomeDirectionStatus.DESTINATION_NOT_RESOLVED -> "Casa ? geocoder"
        HomeDirectionStatus.LOW_CONFIDENCE_DESTINATION -> "Casa ? baja confianza"
    }
}
