package com.zibete.driverassistant.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zibete.driverassistant.calculator.DriverDecision
import com.zibete.driverassistant.calculator.TripDecisionResult
import com.zibete.driverassistant.capture.ScreenCaptureFrameService
import com.zibete.driverassistant.capture.ScreenCaptureManager
import com.zibete.driverassistant.capture.ScreenCaptureMonitorResult
import com.zibete.driverassistant.capture.ScreenCaptureMonitorService
import com.zibete.driverassistant.config.DriverConfig
import com.zibete.driverassistant.config.DriverConfigFormField
import com.zibete.driverassistant.config.DriverConfigFormState
import com.zibete.driverassistant.overlay.DriverDecisionOverlayService
import com.zibete.driverassistant.overlay.OverlayCardState
import com.zibete.driverassistant.overlay.OverlayPermissionHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    val monitorLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val intent = ScreenCaptureMonitorService.buildStartIntent(
                context = context,
                resultCode = result.resultCode,
                resultData = data
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            resolvedViewModel.markMonitorPermissionDenied()
        }
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

    DisposableEffect(context, screenCaptureManager) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action == ScreenCaptureFrameService.ACTION_CAPTURE_RESULT) {
                    resolvedViewModel.updateScreenCaptureSession(
                        screenCaptureManager.handleCaptureResult(intent)
                    )
                }
            }
        }
        val filter = IntentFilter(ScreenCaptureFrameService.ACTION_CAPTURE_RESULT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                if (intent?.action == ScreenCaptureMonitorService.ACTION_MONITOR_RESULT) {
                    resolvedViewModel.updateScreenCaptureMonitor(
                        ScreenCaptureMonitorResult.fromIntent(intent)
                    )
                }
            }
        }
        val filter = IntentFilter(ScreenCaptureMonitorService.ACTION_MONITOR_RESULT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            context.unregisterReceiver(receiver)
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
        onCaptureFrame = {
            resolvedViewModel.updateScreenCaptureSession(screenCaptureManager.captureOnce())
        },
        onAnalyzeOcr = resolvedViewModel::analyzeLastRecognizedText,
        onStartMonitoring = {
            resolvedViewModel.markMonitorWaitingPermission()
            monitorLauncher.launch(screenCaptureManager.buildPermissionIntent())
        },
        onStopMonitoring = {
            context.stopService(Intent(context, ScreenCaptureMonitorService::class.java))
            resolvedViewModel.markMonitorStopped()
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
        onShowLastRealDecisionOverlay = {
            val overlayState = resolvedViewModel.buildLastRealDecisionOverlayState()
            if (overlayState != null) {
                startDecisionOverlay(
                    context = context,
                    overlayState = overlayState,
                    viewModel = resolvedViewModel,
                    overlayPermissionHelper = overlayPermissionHelper
                )
            }
        },
        onConfigInputChange = resolvedViewModel::updateDriverConfigInput,
        onSaveConfig = resolvedViewModel::saveDriverConfigForm,
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
    onCaptureFrame: () -> Unit,
    onAnalyzeOcr: () -> Unit,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onStopService: () -> Unit,
    onRunSimulatedDecision: () -> Unit,
    onShowLastRealDecisionOverlay: () -> Unit,
    onConfigInputChange: (DriverConfigFormField, String) -> Unit,
    onSaveConfig: () -> Unit,
    onResetConfig: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Viaje Rentable AR",
                style = MaterialTheme.typography.headlineSmall
            )

            StatusSection(uiState)
            PrimaryMonitorSection(
                uiState = uiState,
                onStartMonitoring = onStartMonitoring,
                onStopMonitoring = onStopMonitoring
            )
            LastDecisionSection(
                result = uiState.lastDecision,
                statusMessage = uiState.decisionStatusMessage
            )
            EditableConfigSection(
                form = uiState.configForm,
                statusMessage = uiState.configStatusMessage,
                onConfigInputChange = onConfigInputChange,
                onSaveConfig = onSaveConfig,
                onResetConfig = onResetConfig
            )
            ManualToolsSection(
                uiState = uiState,
                overlayActionsEnabled = overlayActionsEnabled,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onRequestScreenCapturePermission = onRequestScreenCapturePermission,
                onCaptureFrame = onCaptureFrame,
                onAnalyzeOcr = onAnalyzeOcr,
                onStopScreenCapture = onStopScreenCapture,
                onStopService = onStopService,
                onRunSimulatedDecision = onRunSimulatedDecision,
                onShowLastRealDecisionOverlay = onShowLastRealDecisionOverlay
            )
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
        putExtra(DriverDecisionOverlayService.EXTRA_VISUAL_STATE, overlayState.visualState.name)
        putExtra(DriverDecisionOverlayService.EXTRA_TITLE_TEXT, overlayState.titleText)
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
            Text(
                text = "Estado",
                style = MaterialTheme.typography.titleMedium
            )
            Text("Monitoreo: ${uiState.monitorStatus}")
            Text("Overlay: ${uiState.overlayPermissionStatus}")
            Text("Captura: ${uiState.screenCapturePermissionStatus}")
            Text("Servicio overlay: ${uiState.serviceStatus}")
        }
    }
}

@Composable
private fun PrimaryMonitorSection(
    uiState: MainUiState,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Monitoreo",
                style = MaterialTheme.typography.titleLarge
            )
            Text("Estado actual: ${uiState.monitorStatus}")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onStartMonitoring
                ) {
                    Text("Iniciar monitoreo")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onStopMonitoring
                ) {
                    Text("Detener")
                }
            }
            uiState.monitorOverlayStatus?.let { status ->
                Text(status)
            }
            uiState.monitorErrorMessage?.let { errorMessage ->
                Text("Detalle: $errorMessage")
            }
        }
    }
}

@Composable
private fun LastDecisionSection(
    result: TripDecisionResult?,
    statusMessage: String?
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Ultima decision",
                style = MaterialTheme.typography.titleLarge
            )
            statusMessage?.let { Text(it) }

            if (result == null) {
                Text("Sin calculo todavia")
            } else {
                Text(result.decision.toSpanishLabel(), style = MaterialTheme.typography.titleLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DecisionMetric(
                        modifier = Modifier.weight(1f),
                        label = "Tarifa",
                        value = "${'$'} ${result.fareAmount?.roundToInt() ?: "-"}"
                    )
                    DecisionMetric(
                        modifier = Modifier.weight(1f),
                        label = "$/km",
                        value = result.arsPerKm.toDisplayMoney()
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DecisionMetric(
                        modifier = Modifier.weight(1f),
                        label = "$/h",
                        value = result.arsPerHour.toDisplayMoney()
                    )
                    DecisionMetric(
                        modifier = Modifier.weight(1f),
                        label = "Total",
                        value = "${result.totalKm.toDisplayNumber()} km - ${result.totalMinutes.toDisplayNumber()} min"
                    )
                }

                val mainReason = result.rejectionReasons.firstOrNull()
                    ?: result.reviewReasons.firstOrNull()
                if (mainReason != null) {
                    Text("Motivo: $mainReason")
                }
            }
        }
    }
}

@Composable
private fun DecisionMetric(
    modifier: Modifier,
    label: String,
    value: String
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun EditableConfigSection(
    form: DriverConfigFormState,
    statusMessage: String?,
    onConfigInputChange: (DriverConfigFormField, String) -> Unit,
    onSaveConfig: () -> Unit,
    onResetConfig: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Configuracion",
                style = MaterialTheme.typography.titleLarge
            )
            ConfigNumberField(
                label = "Minimo ${'$'}/km",
                value = form.minArsPerKm,
                field = DriverConfigFormField.MIN_ARS_PER_KM,
                onConfigInputChange = onConfigInputChange
            )
            ConfigNumberField(
                label = "Minimo ${'$'}/h",
                value = form.minArsPerHour,
                field = DriverConfigFormField.MIN_ARS_PER_HOUR,
                onConfigInputChange = onConfigInputChange
            )
            ConfigNumberField(
                label = "Ganancia minima",
                value = form.minNetProfit,
                field = DriverConfigFormField.MIN_NET_PROFIT,
                onConfigInputChange = onConfigInputChange
            )
            ConfigNumberField(
                label = "Costo por km",
                value = form.costPerKm,
                field = DriverConfigFormField.COST_PER_KM,
                onConfigInputChange = onConfigInputChange
            )
            ConfigNumberField(
                label = "Costo por minuto",
                value = form.costPerMinute,
                field = DriverConfigFormField.COST_PER_MINUTE,
                onConfigInputChange = onConfigInputChange
            )
            ConfigNumberField(
                label = "Tolerancia revision %",
                value = form.reviewTolerancePercent,
                field = DriverConfigFormField.REVIEW_TOLERANCE_PERCENT,
                onConfigInputChange = onConfigInputChange
            )
            statusMessage?.let { message ->
                Text(message)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onSaveConfig
                ) {
                    Text("Guardar config")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onResetConfig
                ) {
                    Text("Restablecer config")
                }
            }
        }
    }
}

@Composable
private fun ConfigNumberField(
    label: String,
    value: String,
    field: DriverConfigFormField,
    onConfigInputChange: (DriverConfigFormField, String) -> Unit
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = { onConfigInputChange(field, it) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
private fun ManualToolsSection(
    uiState: MainUiState,
    overlayActionsEnabled: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestScreenCapturePermission: () -> Unit,
    onCaptureFrame: () -> Unit,
    onAnalyzeOcr: () -> Unit,
    onStopScreenCapture: () -> Unit,
    onStopService: () -> Unit,
    onRunSimulatedDecision: () -> Unit,
    onShowLastRealDecisionOverlay: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Herramientas manuales",
                style = MaterialTheme.typography.titleLarge
            )
            Text("Permisos, captura puntual y pruebas de overlay.")
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onCaptureFrame
                ) {
                    Text("Capturar frame")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onAnalyzeOcr
                ) {
                    Text("Analizar OCR")
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
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = overlayActionsEnabled,
                    onClick = onRunSimulatedDecision
                ) {
                    Text("Overlay simulado")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onStopService
                ) {
                    Text("Detener overlay")
                }
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = overlayActionsEnabled,
                onClick = onShowLastRealDecisionOverlay
            ) {
                Text("Mostrar ultima decision real")
            }
            Text("OCR: ${uiState.ocrStatus}")
            uiState.lastRecognizedText?.takeIf { it.isNotBlank() }?.let { text ->
                Text("Ultimo texto OCR:")
                Text(text)
            }
            uiState.lastCapturedFrameTimestamp?.let { timestamp ->
                Text("Ultima captura: ${timestamp.toDisplayTime()}")
            }
            if (uiState.lastCapturedFrameWidth != null && uiState.lastCapturedFrameHeight != null) {
                Text("Frame: ${uiState.lastCapturedFrameWidth} x ${uiState.lastCapturedFrameHeight}")
            }
            uiState.screenCaptureErrorMessage?.let { errorMessage ->
                Text("Captura: $errorMessage")
            }
            uiState.ocrErrorMessage?.let { errorMessage ->
                Text("OCR: $errorMessage")
            }
        }
    }
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

private fun Long.toDisplayTime(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(this))
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val sampleUiState = MainUiState(
        overlayPermissionStatus = "Concedido",
        screenCapturePermissionStatus = "Captura disponible",
        lastCapturedFrameWidth = 1080,
        lastCapturedFrameHeight = 2400,
        lastCapturedFrameTimestamp = 1_700_000_000_000L,
        ocrStatus = "Texto detectado",
        lastRecognizedText = "ARS 5.127\n41 min\n8.0 km",
        monitorStatus = "Oferta detectada",
        monitorLastRecognizedText = "Uber ARS 5.127\n41 min\n8.0 km",
        monitorOverlayStatus = "Overlay actualizado con oferta detectada",
        serviceStatus = "Ejecutando",
        decisionStatusMessage = "Decision OCR calculada con datos completos",
        lastConfig = DriverConfig.default(),
        configForm = DriverConfigFormState.fromConfig(DriverConfig.default()),
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
            onCaptureFrame = {},
            onAnalyzeOcr = {},
            onStartMonitoring = {},
            onStopMonitoring = {},
            onStopService = {},
            onRunSimulatedDecision = {},
            onShowLastRealDecisionOverlay = {},
            onConfigInputChange = { _, _ -> },
            onSaveConfig = {},
            onResetConfig = {}
        )
    }
}
