package com.zibete.driverassistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.driverassistant.calculator.DriverProfitCalculator
import com.zibete.driverassistant.calculator.TripDecisionResult
import com.zibete.driverassistant.calculator.TripOfferInput
import com.zibete.driverassistant.capture.ScreenCaptureSession
import com.zibete.driverassistant.capture.ScreenCaptureStatus
import com.zibete.driverassistant.config.DriverConfig
import com.zibete.driverassistant.config.DriverConfigRepository
import com.zibete.driverassistant.ocr.OcrStatus
import com.zibete.driverassistant.ocr.TripOfferAnalysisResult
import com.zibete.driverassistant.ocr.TripOfferDecisionPipeline
import com.zibete.driverassistant.overlay.OverlayCardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val configRepository: DriverConfigRepository,
    private val calculator: DriverProfitCalculator = DriverProfitCalculator(),
    private val ocrDecisionPipeline: TripOfferDecisionPipeline = TripOfferDecisionPipeline(
        calculator = calculator
    )
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            configRepository.config.collect { config ->
                _uiState.update { it.copy(lastConfig = config) }
            }
        }
    }

    fun refreshOverlayPermission(canDrawOverlays: Boolean) {
        _uiState.update {
            it.copy(
                overlayPermissionStatus = if (canDrawOverlays) {
                    "Otorgado"
                } else {
                    "Falta permiso"
                }
            )
        }
    }

    fun markOverlayStarted() {
        _uiState.update { it.copy(serviceStatus = "Overlay activo") }
    }

    fun markOverlayStopped() {
        _uiState.update { it.copy(serviceStatus = "Detenido") }
    }

    fun markOverlayPermissionMissing() {
        _uiState.update {
            it.copy(
                overlayPermissionStatus = "Falta permiso",
                serviceStatus = "No iniciado: falta permiso overlay"
            )
        }
    }

    fun markScreenCapturePending() {
        _uiState.update {
            it.copy(
                screenCapturePermissionStatus = "Captura pendiente",
                screenCaptureErrorMessage = null,
                lastCapturedFrameWidth = null,
                lastCapturedFrameHeight = null,
                lastCapturedFrameTimestamp = null,
                ocrStatus = OcrStatus.IDLE.toSpanishStatus(),
                lastRecognizedText = null,
                ocrErrorMessage = null
            )
        }
    }

    fun updateScreenCaptureSession(session: ScreenCaptureSession) {
        val frame = session.lastFrame
        _uiState.update {
            it.copy(
                screenCapturePermissionStatus = session.status.toSpanishStatus(),
                screenCaptureErrorMessage = session.errorMessage,
                lastCapturedFrameWidth = frame?.width,
                lastCapturedFrameHeight = frame?.height,
                lastCapturedFrameTimestamp = frame?.capturedAtMillis,
                ocrStatus = session.ocrStatus.toSpanishStatus(),
                lastRecognizedText = session.recognizedText,
                ocrErrorMessage = session.ocrErrorMessage
            )
        }
    }

    fun increaseMinArsPerKmPlaceholder() {
        val currentConfig = _uiState.value.lastConfig ?: DriverConfig.default()
        viewModelScope.launch {
            configRepository.updateConfig(
                currentConfig.copy(minArsPerKm = currentConfig.minArsPerKm + 25.0)
            )
        }
    }

    fun resetConfigToDefaults() {
        viewModelScope.launch {
            configRepository.resetToDefaults()
        }
    }

    fun runSimulatedTripDecision(): OverlayCardState {
        val config = _uiState.value.lastConfig ?: DriverConfig.default()
        val simulatedInput = TripOfferInput(
            fareAmount = 5127.0,
            pickupKm = 1.0,
            tripKm = 7.0,
            pickupMinutes = 5.0,
            tripMinutes = 36.0,
            platform = "uber",
            rawText = "ARS 5.127 41 min 8.0 km"
        )

        val result = calculator.calculate(
            input = simulatedInput,
            config = config
        )

        _uiState.update {
            it.copy(
                lastDecision = result,
                lastConfig = config,
                decisionStatusMessage = "Decision simulada calculada"
            )
        }
        return OverlayCardState.fromDecisionResult(result)
    }

    fun analyzeLastRecognizedText() {
        val state = _uiState.value
        val config = state.lastConfig ?: DriverConfig.default()

        when (val analysis = ocrDecisionPipeline.analyzeRecognizedText(state.lastRecognizedText, config)) {
            TripOfferAnalysisResult.NoText -> {
                _uiState.update {
                    it.copy(
                        lastConfig = config,
                        decisionStatusMessage = "No hay texto OCR para analizar"
                    )
                }
            }
            TripOfferAnalysisResult.NoTripDetected -> {
                _uiState.update {
                    it.copy(
                        lastConfig = config,
                        decisionStatusMessage = "No se pudo detectar una oferta de viaje en el OCR"
                    )
                }
            }
            is TripOfferAnalysisResult.DecisionReady -> {
                val result = analysis.result
                _uiState.update {
                    it.copy(
                        lastDecision = result,
                        lastRealDecision = result,
                        lastConfig = config,
                        decisionStatusMessage = result.toAnalysisStatusMessage()
                    )
                }
            }
        }
    }

    fun buildLastRealDecisionOverlayState(): OverlayCardState? {
        val result = _uiState.value.lastRealDecision
        if (result == null) {
            _uiState.update {
                it.copy(decisionStatusMessage = "No hay una decision real calculada para mostrar en overlay")
            }
            return null
        }

        return OverlayCardState.fromDecisionResult(result)
    }

    private fun ScreenCaptureStatus.toSpanishStatus(): String {
        return when (this) {
            ScreenCaptureStatus.PENDING -> "Captura pendiente"
            ScreenCaptureStatus.AUTHORIZED -> "Captura autorizada"
            ScreenCaptureStatus.CAPTURE_AVAILABLE -> "Captura disponible"
            ScreenCaptureStatus.STOPPED -> "Captura detenida"
            ScreenCaptureStatus.ERROR -> "Error de captura"
        }
    }

    private fun OcrStatus.toSpanishStatus(): String {
        return when (this) {
            OcrStatus.IDLE -> "OCR inactivo"
            OcrStatus.PROCESSING -> "OCR procesando"
            OcrStatus.TEXT_DETECTED -> "Texto detectado"
            OcrStatus.NO_TEXT -> "Sin texto detectado"
            OcrStatus.ERROR -> "Error OCR"
        }
    }

    private fun TripDecisionResult.toAnalysisStatusMessage(): String {
        val missingDataReasons = (rejectionReasons + reviewReasons)
            .filter { reason ->
                reason.contains("no detectada", ignoreCase = true) ||
                    reason.contains("incompleta", ignoreCase = true)
            }

        return when {
            missingDataReasons.isNotEmpty() -> "Decision OCR calculada con datos faltantes: ${missingDataReasons.first()}"
            rejectionReasons.isNotEmpty() -> "Decision OCR calculada: rechazo por ${rejectionReasons.first()}"
            reviewReasons.isNotEmpty() -> "Decision OCR calculada: revisar por ${reviewReasons.first()}"
            else -> "Decision OCR calculada con datos completos"
        }
    }
}
