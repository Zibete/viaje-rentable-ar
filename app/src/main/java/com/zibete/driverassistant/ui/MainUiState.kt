package com.zibete.driverassistant.ui

import com.zibete.driverassistant.calculator.TripDecisionResult
import com.zibete.driverassistant.config.DriverConfig

data class MainUiState(
    val overlayPermissionStatus: String = "Pendiente",
    val screenCapturePermissionStatus: String = "Pendiente",
    val serviceStatus: String = "Detenido",
    val lastDecision: TripDecisionResult? = null,
    val lastConfig: DriverConfig? = null
)

