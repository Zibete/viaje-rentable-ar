package com.zibete.driverassistant.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.driverassistant.calculator.DriverProfitCalculator
import com.zibete.driverassistant.calculator.TripOfferInput
import com.zibete.driverassistant.config.DriverConfig
import com.zibete.driverassistant.config.DriverConfigRepository
import com.zibete.driverassistant.config.LocalDriverConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val configRepository: DriverConfigRepository = LocalDriverConfigRepository(),
    private val calculator: DriverProfitCalculator = DriverProfitCalculator()
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(lastConfig = configRepository.getConfig()) }
        }
    }

    fun startServicePlaceholder() {
        _uiState.update { it.copy(serviceStatus = "Servicio simulado iniciado") }
    }

    fun stopServicePlaceholder() {
        _uiState.update { it.copy(serviceStatus = "Detenido") }
    }

    fun runSimulatedTripDecision() {
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
    }
}

