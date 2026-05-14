package com.zibete.driverassistant.ui

import com.zibete.driverassistant.calculator.TripDecisionResult
import com.zibete.driverassistant.config.DriverConfig

data class MainUiState(
    val overlayPermissionStatus: String = "Pendiente",
    val screenCapturePermissionStatus: String = "Pendiente",
    val screenCaptureErrorMessage: String? = null,
    val lastCapturedFrameWidth: Int? = null,
    val lastCapturedFrameHeight: Int? = null,
    val lastCapturedFrameTimestamp: Long? = null,
    val ocrStatus: String = "OCR inactivo",
    val lastRecognizedText: String? = null,
    val ocrErrorMessage: String? = null,
    val serviceStatus: String = "Detenido",
    val lastDecision: TripDecisionResult? = null,
    val lastRealDecision: TripDecisionResult? = null,
    val decisionStatusMessage: String? = null,
    val lastConfig: DriverConfig? = null
)
