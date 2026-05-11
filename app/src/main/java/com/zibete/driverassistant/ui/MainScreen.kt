package com.zibete.driverassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zibete.driverassistant.calculator.DriverDecision
import com.zibete.driverassistant.calculator.TripDecisionResult
import com.zibete.driverassistant.config.DriverConfig
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    MainScreenContent(
        uiState = uiState,
        onStartService = viewModel::startServicePlaceholder,
        onStopService = viewModel::stopServicePlaceholder,
        onRunSimulatedDecision = viewModel::runSimulatedTripDecision
    )
}

@Composable
fun MainScreenContent(
    uiState: MainUiState,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRunSimulatedDecision: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Viaje Rentable AR",
                style = MaterialTheme.typography.headlineSmall
            )

            StatusSection(uiState)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { }
                ) {
                    Text("Permiso overlay")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { }
                ) {
                    Text("Permiso captura")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onStartService
                ) {
                    Text("Iniciar")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onStopService
                ) {
                    Text("Detener")
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRunSimulatedDecision
            ) {
                Text("Probar cálculo simulado")
            }

            DecisionSection(uiState.lastDecision)
            ConfigSection(uiState.lastConfig)
        }
    }
}

@Composable
private fun StatusSection(uiState: MainUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Overlay: ${uiState.overlayPermissionStatus}")
            Text("Captura: ${uiState.screenCapturePermissionStatus}")
            Text("Servicio: ${uiState.serviceStatus}")
        }
    }
}

@Composable
private fun DecisionSection(result: TripDecisionResult?) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Última decisión",
                style = MaterialTheme.typography.titleMedium
            )

            if (result == null) {
                Text("Sin cálculo todavía")
            } else {
                Text(result.decision.toSpanishLabel(), style = MaterialTheme.typography.titleLarge)
                Text("${'$'} ${result.fareAmount?.roundToInt() ?: "-"}")
                Text("${result.arsPerHour.toDisplayMoney()}/h · ${result.arsPerKm.toDisplayMoney()}/km")
                Text("${result.totalMinutes.toDisplayNumber()} min · ${result.totalKm.toDisplayNumber()} km")

                val mainReason = result.rejectionReasons.firstOrNull()
                    ?: result.reviewReasons.firstOrNull()
                if (mainReason != null) {
                    Text(mainReason)
                }
            }
        }
    }
}

@Composable
private fun ConfigSection(config: DriverConfig?) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Configuración cargada",
                style = MaterialTheme.typography.titleMedium
            )

            if (config == null) {
                Text("Cargando configuración local")
            } else {
                Text("Mínimo: ${config.minArsPerKm.roundToInt()} ${'$'}/km · ${config.minArsPerHour.roundToInt()} ${'$'}/h")
                Text("Pickup máximo: ${config.maxPickupKm.toDisplayNumber()} km · ${config.maxPickupMinutes.toDisplayNumber()} min")
                Text("Zonas configuradas: ${config.avoidZones.size}")
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

private fun DriverDecision.toSpanishLabel(): String {
    return when (this) {
        DriverDecision.ACCEPT -> "ACEPTAR"
        DriverDecision.REJECT -> "RECHAZAR"
        DriverDecision.REVIEW -> "REVISAR"
    }
}

private fun Double?.toDisplayMoney(): String {
    return this?.roundToInt()?.let { "${'$'} $it" } ?: "-"
}

private fun Double?.toDisplayNumber(): String {
    return this?.let { "%.1f".format(it) } ?: "-"
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val sampleUiState = MainUiState(
        overlayPermissionStatus = "Concedido",
        screenCapturePermissionStatus = "Concedido",
        serviceStatus = "Ejecutando",
        lastConfig = DriverConfig.default(),
        lastDecision = TripDecisionResult(
            decision = DriverDecision.ACCEPT,
            fareAmount = 5127.0,
            arsPerKm = 640.8,
            arsPerHour = 7502.9,
            estimatedCost = 3470.0,
            estimatedNetProfit = 1657.0,
            totalKm = 8.0,
            totalMinutes = 41.0,
            rejectionReasons = emptyList(),
            reviewReasons = emptyList()
        )
    )
    MaterialTheme {
        MainScreenContent(
            uiState = sampleUiState,
            onStartService = {},
            onStopService = {},
            onRunSimulatedDecision = {}
        )
    }
}
