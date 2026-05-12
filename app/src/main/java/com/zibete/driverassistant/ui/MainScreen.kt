package com.zibete.driverassistant.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zibete.driverassistant.calculator.DriverDecision
import com.zibete.driverassistant.calculator.TripDecisionResult
import com.zibete.driverassistant.capture.ScreenCaptureManager
import com.zibete.driverassistant.config.DriverConfig
import com.zibete.driverassistant.overlay.DriverDecisionOverlayService
import com.zibete.driverassistant.overlay.OverlayCardState
import com.zibete.driverassistant.overlay.OverlayPermissionHelper
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    viewModel: MainViewModel? = null
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val factory = remember(appContext) { MainViewModelFactory(appContext) }
    val resolvedViewModel = viewModel ?: viewModel(factory = factory)
    val uiState by resolvedViewModel.uiState.collectAsState()
    val overlayPermissionHelper = remember { OverlayPermissionHelper() }
    val screenCaptureManager = remember(context) { ScreenCaptureManager(context) }
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        resolvedViewModel.updateScreenCaptureSession(
            screenCaptureManager.handlePermissionResult(
                resultCode = result.resultCode,
                data = result.data
            )
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resolvedViewModel.refreshOverlayPermission(
                    overlayPermissionHelper.canDrawOverlays(context)
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        resolvedViewModel.refreshOverlayPermission(overlayPermissionHelper.canDrawOverlays(context))

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    MainScreenContent(
        uiState = uiState,
        overlayActionsEnabled = uiState.overlayPermissionStatus == "Otorgado",
        onRequestOverlayPermission = {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                overlayPermissionHelper.buildOverlaySettingsUri(context)
            )
            context.startActivity(intent)
        },
        onRequestScreenCapturePermission = {
            resolvedViewModel.markScreenCapturePending()
            screenCaptureLauncher.launch(screenCaptureManager.buildPermissionIntent())
        },
        onStopScreenCapture = {
            resolvedViewModel.updateScreenCaptureSession(screenCaptureManager.stopSession())
        },
        onStartService = {
            val overlayState = resolvedViewModel.runSimulatedTripDecision()
            startDecisionOverlay(
                context = context,
                overlayState = overlayState,
                viewModel = resolvedViewModel,
                overlayPermissionHelper = overlayPermissionHelper
            )
        },
        onStopService = {
            context.stopService(Intent(context, DriverDecisionOverlayService::class.java))
            resolvedViewModel.markOverlayStopped()
        },
        onRunSimulatedDecision = {
            val overlayState = resolvedViewModel.runSimulatedTripDecision()
            startDecisionOverlay(
                context = context,
                overlayState = overlayState,
                viewModel = resolvedViewModel,
                overlayPermissionHelper = overlayPermissionHelper
            )
        },
        onIncreaseMinArsPerKm = resolvedViewModel::increaseMinArsPerKmPlaceholder,
        onResetConfig = resolvedViewModel::resetConfigToDefaults
    )
}

@Composable
fun MainScreenContent(
    uiState: MainUiState,
    overlayActionsEnabled: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestScreenCapturePermission: () -> Unit,
    onStopScreenCapture: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRunSimulatedDecision: () -> Unit,
    onIncreaseMinArsPerKm: () -> Unit,
    onResetConfig: () -> Unit
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
            ForegroundServiceInfoSection(uiState.serviceStatus)
            ScreenCaptureInfoSection(uiState)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onRequestOverlayPermission
                ) {
                    Text("Permiso overlay")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onRequestScreenCapturePermission
                ) {
                    Text("Permiso captura")
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onStopScreenCapture
            ) {
                Text("Detener captura")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = overlayActionsEnabled,
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
                enabled = overlayActionsEnabled,
                onClick = onRunSimulatedDecision
            ) {
                Text("Probar overlay simulado")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onIncreaseMinArsPerKm
                ) {
                    Text("Subir min ${'$'}/km")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onResetConfig
                ) {
                    Text("Restablecer config")
                }
            }

            DecisionSection(uiState.lastDecision)
            ConfigSection(uiState.lastConfig)
        }
    }
}

private fun startDecisionOverlay(
    context: Context,
    overlayState: OverlayCardState,
    viewModel: MainViewModel,
    overlayPermissionHelper: OverlayPermissionHelper
) {
    if (!overlayPermissionHelper.canDrawOverlays(context)) {
        viewModel.markOverlayPermissionMissing()
        return
    }

    val intent = Intent(context, DriverDecisionOverlayService::class.java).apply {
        putExtra(DriverDecisionOverlayService.EXTRA_DECISION, overlayState.decision.name)
        putExtra(DriverDecisionOverlayService.EXTRA_FARE_TEXT, overlayState.fareText)
        putExtra(DriverDecisionOverlayService.EXTRA_ARS_PER_HOUR_TEXT, overlayState.arsPerHourText)
        putExtra(DriverDecisionOverlayService.EXTRA_ARS_PER_KM_TEXT, overlayState.arsPerKmText)
        putExtra(DriverDecisionOverlayService.EXTRA_TOTAL_TIME_TEXT, overlayState.totalTimeText)
        putExtra(DriverDecisionOverlayService.EXTRA_TOTAL_KM_TEXT, overlayState.totalKmText)
        putExtra(DriverDecisionOverlayService.EXTRA_SHORT_REASON, overlayState.shortReason)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
    viewModel.markOverlayStarted()
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
private fun ForegroundServiceInfoSection(serviceStatus: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Notificacion del servicio",
                style = MaterialTheme.typography.titleMedium
            )
            Text("Overlay de decision activo")
            Text("Estado actual: $serviceStatus")
            Text("La notificacion permite detener el overlay sin volver a la app.")
        }
    }
}

@Composable
private fun ScreenCaptureInfoSection(uiState: MainUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Captura de pantalla",
                style = MaterialTheme.typography.titleMedium
            )
            Text("Estado: ${uiState.screenCapturePermissionStatus}")
            Text("MediaProjection solo queda autorizado; todavia no se captura ni procesa imagen.")
            uiState.screenCaptureErrorMessage?.let { errorMessage ->
                Text("Detalle: $errorMessage")
            }
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
                text = "Ultima decision",
                style = MaterialTheme.typography.titleMedium
            )

            if (result == null) {
                Text("Sin calculo todavia")
            } else {
                Text(result.decision.toSpanishLabel(), style = MaterialTheme.typography.titleLarge)
                Text("${'$'} ${result.fareAmount?.roundToInt() ?: "-"}")
                Text("${result.arsPerHour.toDisplayMoney()}/h - ${result.arsPerKm.toDisplayMoney()}/km")
                Text("${result.totalMinutes.toDisplayNumber()} min - ${result.totalKm.toDisplayNumber()} km")

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
                text = "Configuracion cargada",
                style = MaterialTheme.typography.titleMedium
            )

            if (config == null) {
                Text("Cargando configuracion local")
            } else {
                Text("Minimo: ${config.minArsPerKm.roundToInt()} ${'$'}/km - ${config.minArsPerHour.roundToInt()} ${'$'}/h")
                Text("Pickup maximo: ${config.maxPickupKm.toDisplayNumber()} km - ${config.maxPickupMinutes.toDisplayNumber()} min")
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
            overlayActionsEnabled = true,
            onRequestOverlayPermission = {},
            onRequestScreenCapturePermission = {},
            onStopScreenCapture = {},
            onStartService = {},
            onStopService = {},
            onRunSimulatedDecision = {},
            onIncreaseMinArsPerKm = {},
            onResetConfig = {}
        )
    }
}
