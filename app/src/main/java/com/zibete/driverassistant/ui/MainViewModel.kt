package com.zibete.driverassistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.driverassistant.calculator.DriverProfitCalculator
import com.zibete.driverassistant.calculator.TripOfferInput
import com.zibete.driverassistant.config.DriverConfig
import com.zibete.driverassistant.config.DriverConfigRepository
import com.zibete.driverassistant.overlay.OverlayCardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val configRepository: DriverConfigRepository,
    private val calculator: DriverProfitCalculator = DriverProfitCalculator()
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

        _uiState.update { it.copy(lastDecision = result, lastConfig = config) }
        return OverlayCardState.fromDecisionResult(result)
    }
}
