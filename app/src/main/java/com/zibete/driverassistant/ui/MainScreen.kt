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
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
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
                .systemBarsPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Viaje Rentable AR",
                style = MaterialTheme.typography.headlineSmall
            )

            StatusSection(
                uiState = uiState,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onRequestScreenCapturePermission = onRequestScreenCapturePermission
            )
            PrimaryMonitorSection(
                uiState = uiState,
                onStartMonitoring = onStartMonitoring,
                onStopMonitoring = onStopMonitoring
            )
            LastDecisionSection(
                result = uiState.lastDecision
            )
            ConfigSummarySection(
                form = uiState.configForm,
                statusMessage = uiState.configStatusMessage,
                onConfigInputChange = onConfigInputChange,
                onSaveConfig = onSaveConfig,
                onResetConfig = onResetConfig
            )
            DiagnosticToolsSection(
                uiState = uiState,
                overlayActionsEnabled = overlayActionsEnabled,
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
private fun StatusSection(
    uiState: MainUiState,
    onRequestOverlayPermission: () -> Unit,
    onRequestScreenCapturePermission: () -> Unit
) {
    val homeStatus = uiState.toHomeStatus()
    val containerColor = when (homeStatus) {
        HomeStatus.READY -> MaterialTheme.colorScheme.primaryContainer
        HomeStatus.REQUIRES_PERMISSIONS -> MaterialTheme.colorScheme.errorContainer
        HomeStatus.MONITORING -> MaterialTheme.colorScheme.secondaryContainer
        HomeStatus.STOPPED -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Estado del asistente",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = homeStatus.toTitle(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = homeStatus.toDescription(),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoMetric(
                    modifier = Modifier.weight(1f),
                    label = "Ventana flotante",
                    value = uiState.overlayPermissionStatus.toOverlayPermissionSummary()
                )
                InfoMetric(
                    modifier = Modifier.weight(1f),
                    label = "Captura",
                    value = uiState.screenCapturePermissionStatus.toScreenCaptureSummary()
                )
            }
            if (homeStatus == HomeStatus.REQUIRES_PERMISSIONS) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRequestOverlayPermission
                    ) {
                        Text("Permitir ventana flotante")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRequestScreenCapturePermission
                    ) {
                        Text("Permitir captura de pantalla")
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryMonitorSection(
    uiState: MainUiState,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit
) {
    val isMonitoring = uiState.isMonitoringActive()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Monitoreo",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = if (isMonitoring) {
                    "El monitoreo está activo."
                } else {
                    "Iniciá el monitoreo cuando estés por recibir solicitudes."
                },
                style = MaterialTheme.typography.bodyMedium
            )
            if (isMonitoring) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStopMonitoring
                ) {
                    Text("Detener monitoreo")
                }
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStartMonitoring
                ) {
                    Text("Iniciar monitoreo")
                }
            }
            uiState.monitorErrorMessage?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun LastDecisionSection(result: TripDecisionResult?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Última decisión",
                style = MaterialTheme.typography.titleLarge
            )

            if (result == null) {
                Text(
                    text = "Todavía no hay una decisión calculada.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = result.decision.toSpanishLabel(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Motivo: ${result.toMainReason()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoMetric(
                        modifier = Modifier.weight(1f),
                        label = "Tarifa",
                        value = result.fareAmount.toDisplayMoney()
                    )
                    InfoMetric(
                        modifier = Modifier.weight(1f),
                        label = "$/km",
                        value = result.arsPerKm.toDisplayMoney()
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoMetric(
                        modifier = Modifier.weight(1f),
                        label = "$/h",
                        value = result.arsPerHour.toDisplayMoney()
                    )
                    InfoMetric(
                        modifier = Modifier.weight(1f),
                        label = "Total",
                        value = "${result.totalKm.toDisplayNumber()} km / ${result.totalMinutes.toDisplayNumber()} min"
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoMetric(
    modifier: Modifier,
    label: String,
    value: String
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ConfigSummarySection(
    form: DriverConfigFormState,
    statusMessage: String?,
    onConfigInputChange: (DriverConfigFormField, String) -> Unit,
    onSaveConfig: () -> Unit,
    onResetConfig: () -> Unit
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Configuración",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Criterios locales usados para calcular rentabilidad.",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoMetric(
                    modifier = Modifier.weight(1f),
                    label = "Mín. $/km",
                    value = form.minArsPerKm.toMoneyInputSummary()
                )
                InfoMetric(
                    modifier = Modifier.weight(1f),
                    label = "Mín. $/h",
                    value = form.minArsPerHour.toMoneyInputSummary()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoMetric(
                    modifier = Modifier.weight(1f),
                    label = "Costo/km",
                    value = form.costPerKm.toMoneyInputSummary()
                )
                InfoMetric(
                    modifier = Modifier.weight(1f),
                    label = "Costo/min",
                    value = form.costPerMinute.toMoneyInputSummary()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoMetric(
                    modifier = Modifier.weight(1f),
                    label = "Ganancia mín.",
                    value = form.minNetProfit.toMoneyInputSummary()
                )
                InfoMetric(
                    modifier = Modifier.weight(1f),
                    label = "Revisión",
                    value = form.reviewTolerancePercent.toPercentInputSummary()
                )
            }
            statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { isEditing = !isEditing }
            ) {
                Text(if (isEditing) "Ocultar edición" else "Editar configuración")
            }
            if (isEditing) {
                ConfigNumberField(
                    label = "Mínimo ${'$'}/km",
                    value = form.minArsPerKm,
                    field = DriverConfigFormField.MIN_ARS_PER_KM,
                    onConfigInputChange = onConfigInputChange
                )
                ConfigNumberField(
                    label = "Mínimo ${'$'}/h",
                    value = form.minArsPerHour,
                    field = DriverConfigFormField.MIN_ARS_PER_HOUR,
                    onConfigInputChange = onConfigInputChange
                )
                ConfigNumberField(
                    label = "Ganancia mínima",
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
                    label = "Tolerancia de revisión %",
                    value = form.reviewTolerancePercent,
                    field = DriverConfigFormField.REVIEW_TOLERANCE_PERCENT,
                    onConfigInputChange = onConfigInputChange
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onSaveConfig
                    ) {
                        Text("Guardar")
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onResetConfig
                    ) {
                        Text("Restablecer")
                    }
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
private fun DiagnosticToolsSection(
    uiState: MainUiState,
    overlayActionsEnabled: Boolean,
    onCaptureFrame: () -> Unit,
    onAnalyzeOcr: () -> Unit,
    onStopScreenCapture: () -> Unit,
    onStopService: () -> Unit,
    onRunSimulatedDecision: () -> Unit,
    onShowLastRealDecisionOverlay: () -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Herramientas de diagnóstico",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedButton(onClick = { isExpanded = !isExpanded }) {
                    Text(if (isExpanded) "Ocultar" else "Mostrar")
                }
            }

            if (isExpanded) {
                Text(
                    text = "Captura puntual, análisis manual y pruebas de ventana flotante.",
                    style = MaterialTheme.typography.bodyMedium
                )
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
                    Text("Mostrar última decisión real")
                }
                Text(
                    text = "Análisis: ${uiState.ocrStatus}",
                    style = MaterialTheme.typography.bodyMedium
                )
                uiState.decisionStatusMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                uiState.lastRecognizedText?.takeIf { it.isNotBlank() }?.let { text ->
                    Text(
                        text = "Último texto OCR",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                uiState.lastCapturedFrameTimestamp?.let { timestamp ->
                    Text(
                        text = "Última captura: ${timestamp.toDisplayTime()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (uiState.lastCapturedFrameWidth != null && uiState.lastCapturedFrameHeight != null) {
                    Text(
                        text = "Tamaño de frame: ${uiState.lastCapturedFrameWidth} x ${uiState.lastCapturedFrameHeight}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                uiState.screenCaptureErrorMessage?.let { errorMessage ->
                    Text(
                        text = "Captura: $errorMessage",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                uiState.ocrErrorMessage?.let { errorMessage ->
                    Text(
                        text = "OCR: $errorMessage",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private enum class HomeStatus {
    READY,
    REQUIRES_PERMISSIONS,
    MONITORING,
    STOPPED
}

private fun MainUiState.toHomeStatus(): HomeStatus {
    val needsPermission = overlayPermissionStatus != "Otorgado" ||
        monitorStatus == "Esperando permiso" ||
        monitorErrorMessage?.contains("permiso", ignoreCase = true) == true

    return when {
        isMonitoringActive() -> HomeStatus.MONITORING
        needsPermission -> HomeStatus.REQUIRES_PERMISSIONS
        hasMonitoringSessionStarted && monitorStatus == "Detenido" -> HomeStatus.STOPPED
        else -> HomeStatus.READY
    }
}

private fun MainUiState.isMonitoringActive(): Boolean {
    return when (monitorStatus) {
        "Monitoreando",
        "Analizando",
        "Oferta detectada",
        "Datos incompletos",
        "Sin oferta detectada" -> true
        else -> false
    }
}

private fun HomeStatus.toTitle(): String {
    return when (this) {
        HomeStatus.READY -> "Listo para monitorear"
        HomeStatus.REQUIRES_PERMISSIONS -> "Requiere permisos"
        HomeStatus.MONITORING -> "Monitoreando"
        HomeStatus.STOPPED -> "Detenido"
    }
}

private fun HomeStatus.toDescription(): String {
    return when (this) {
        HomeStatus.READY -> "La configuración está cargada y podés iniciar el monitoreo."
        HomeStatus.REQUIRES_PERMISSIONS -> "Habilitá los permisos necesarios para que la app pueda mostrar recomendaciones."
        HomeStatus.MONITORING -> "La app está leyendo la pantalla autorizada y avisará cuando detecte una solicitud."
        HomeStatus.STOPPED -> "El monitoreo está detenido. Podés volver a iniciarlo cuando lo necesites."
    }
}

private fun String.toOverlayPermissionSummary(): String {
    return if (this == "Otorgado") "Permitida" else "Pendiente"
}

private fun String.toScreenCaptureSummary(): String {
    return when (this) {
        "Captura autorizada",
        "Captura disponible" -> "Permitida"
        "Captura detenida" -> "Detenida"
        else -> "Pendiente"
    }
}

private fun DriverDecision.toSpanishLabel(): String {
    return when (this) {
        DriverDecision.ACCEPT -> "Aceptar"
        DriverDecision.REJECT -> "Rechazar"
        DriverDecision.REVIEW -> "Revisar"
    }
}

private fun TripDecisionResult.toMainReason(): String {
    return rejectionReasons.firstOrNull()
        ?: reviewReasons.firstOrNull()
        ?: when (decision) {
            DriverDecision.ACCEPT -> "Cumple los criterios configurados."
            DriverDecision.REJECT -> "No cumple los criterios configurados."
            DriverDecision.REVIEW -> "Conviene revisarlo antes de aceptar."
        }
}

private fun Double?.toDisplayMoney(): String {
    return this?.roundToInt()?.let { "${'$'} $it" } ?: "-"
}

private fun Double?.toDisplayNumber(): String {
    return this?.let { "%.1f".format(it) } ?: "-"
}

private fun String.toMoneyInputSummary(): String {
    return takeIf { it.isNotBlank() }?.let { "${'$'} $it" } ?: "-"
}

private fun String.toPercentInputSummary(): String {
    return takeIf { it.isNotBlank() }?.let { "$it%" } ?: "-"
}

private fun Long.toDisplayTime(): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(this))
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val sampleUiState = MainUiState(
        overlayPermissionStatus = "Otorgado",
        screenCapturePermissionStatus = "Captura disponible",
        lastCapturedFrameWidth = 1080,
        lastCapturedFrameHeight = 2400,
        lastCapturedFrameTimestamp = 1_700_000_000_000L,
        ocrStatus = "Texto detectado",
        lastRecognizedText = "ARS 5.127\n41 min\n8.0 km",
        monitorStatus = "Oferta detectada",
        hasMonitoringSessionStarted = true,
        monitorLastRecognizedText = "Uber ARS 5.127\n41 min\n8.0 km",
        monitorOverlayStatus = "Overlay actualizado con oferta detectada",
        serviceStatus = "Ejecutando",
        decisionStatusMessage = "Decisión OCR calculada con datos completos",
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
