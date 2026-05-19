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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
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

private val AppBackground = Color(0xFF0B0F14)
private val AppSurface = Color(0xFF121821)
private val AppSurfaceRaised = Color(0xFF18212D)
private val AppSurfaceSubtle = Color(0xFF202B38)
private val AppBorder = Color(0xFF2A3646)
private val AppText = Color(0xFFEAF0F7)
private val AppMutedText = Color(0xFFA8B3C2)
private val AppPrimary = Color(0xFF65D6AD)
private val AppPrimaryDim = Color(0xFF123B33)
private val AppWarning = Color(0xFFF7C948)
private val AppWarningDim = Color(0xFF3B3012)
private val AppErrorDim = Color(0xFF421C24)
private val AppError = Color(0xFFFF8A9A)

private val DriverAssistantDarkColorScheme = darkColorScheme(
    primary = AppPrimary,
    onPrimary = Color(0xFF06251F),
    primaryContainer = AppPrimaryDim,
    onPrimaryContainer = AppText,
    secondary = Color(0xFF8DB7FF),
    onSecondary = Color(0xFF071A38),
    secondaryContainer = Color(0xFF172B4D),
    onSecondaryContainer = AppText,
    background = AppBackground,
    onBackground = AppText,
    surface = AppSurface,
    onSurface = AppText,
    surfaceVariant = AppSurfaceSubtle,
    onSurfaceVariant = AppMutedText,
    error = AppError,
    onError = Color(0xFF33000A),
    errorContainer = AppErrorDim,
    onErrorContainer = AppText,
    outline = AppBorder
)

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
    var selectedSectionName by rememberSaveable { mutableStateOf(MainSection.HOME.name) }
    val selectedSection = MainSection.valueOf(selectedSectionName)

    MaterialTheme(colorScheme = DriverAssistantDarkColorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                AppHeader()
                MainSectionTabs(
                    selectedSection = selectedSection,
                    onSectionSelected = { section -> selectedSectionName = section.name }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedSection) {
                        MainSection.HOME -> HomeSection(
                            uiState = uiState,
                            onRequestOverlayPermission = onRequestOverlayPermission,
                            onRequestScreenCapturePermission = onRequestScreenCapturePermission,
                            onStartMonitoring = onStartMonitoring,
                            onStopMonitoring = onStopMonitoring
                        )
                        MainSection.SETTINGS -> SettingsSection(
                            uiState = uiState,
                            onConfigInputChange = onConfigInputChange,
                            onSaveConfig = onSaveConfig,
                            onResetConfig = onResetConfig
                        )
                        MainSection.DIAGNOSTIC -> DiagnosticToolsSection(
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
        }
    }
}

@Composable
private fun AppHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Viaje Rentable AR",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Asistente visual para evaluar solicitudes de viaje.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MainSectionTabs(
    selectedSection: MainSection,
    onSectionSelected: (MainSection) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedSection.ordinal,
        containerColor = AppBackground,
        contentColor = AppPrimary
    ) {
        MainSection.entries.forEach { section ->
            Tab(
                selected = selectedSection == section,
                onClick = { onSectionSelected(section) },
                selectedContentColor = AppText,
                unselectedContentColor = AppMutedText,
                text = {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedSection == section) {
                            FontWeight.Bold
                        } else {
                            FontWeight.SemiBold
                        }
                    )
                }
            )
        }
    }
}

private enum class MainSection(
    val title: String
) {
    HOME("Inicio"),
    SETTINGS("Configuración"),
    DIAGNOSTIC("Diagnóstico")
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
private fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: Color = AppSurface,
    borderColor: Color = AppBorder,
    elevation: Dp = 2.dp,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        content()
    }
}

@Composable
private fun darkOutlinedButtonColors() = ButtonDefaults.outlinedButtonColors(
    contentColor = AppText,
    containerColor = AppSurfaceRaised,
    disabledContentColor = AppMutedText.copy(alpha = 0.45f),
    disabledContainerColor = AppSurface.copy(alpha = 0.7f)
)

@Composable
private fun HomeSection(
    uiState: MainUiState,
    onRequestOverlayPermission: () -> Unit,
    onRequestScreenCapturePermission: () -> Unit,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit
) {
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
    LastDecisionSection(result = uiState.lastDecision)
    HomeConfigSummarySection(
        form = uiState.configForm,
        config = uiState.lastConfig ?: DriverConfig.default()
    )
}

@Composable
private fun StatusSection(
    uiState: MainUiState,
    onRequestOverlayPermission: () -> Unit,
    onRequestScreenCapturePermission: () -> Unit
) {
    val homeStatus = uiState.toHomeStatus()
    val containerColor = when (homeStatus) {
        HomeStatus.READY -> AppPrimaryDim
        HomeStatus.REQUIRES_PERMISSIONS -> AppErrorDim
        HomeStatus.MONITORING -> Color(0xFF172B4D)
        HomeStatus.STOPPED -> AppSurfaceRaised
    }
    val borderColor = when (homeStatus) {
        HomeStatus.READY -> AppPrimary.copy(alpha = 0.42f)
        HomeStatus.REQUIRES_PERMISSIONS -> AppError.copy(alpha = 0.48f)
        HomeStatus.MONITORING -> Color(0xFF8DB7FF).copy(alpha = 0.45f)
        HomeStatus.STOPPED -> AppBorder
    }

    AppCard(
        containerColor = containerColor,
        borderColor = borderColor,
        elevation = 4.dp
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
                        onClick = onRequestOverlayPermission,
                        colors = darkOutlinedButtonColors()
                    ) {
                        Text("Permitir ventana flotante")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRequestScreenCapturePermission,
                        colors = darkOutlinedButtonColors()
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

    AppCard(containerColor = AppSurface) {
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
                    onClick = onStopMonitoring,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppError,
                        contentColor = Color(0xFF220006)
                    )
                ) {
                    Text("Detener monitoreo")
                }
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onStartMonitoring,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppPrimary,
                        contentColor = Color(0xFF06251F)
                    )
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
    val containerColor = result?.decision.toDecisionContainerColor()
    val borderColor = result?.decision?.toDecisionAccentColor() ?: AppBorder

    AppCard(
        containerColor = containerColor,
        borderColor = borderColor.copy(alpha = 0.55f),
        elevation = if (result == null) 2.dp else 4.dp
    ) {
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
                    text = "Cuando se detecte una solicitud, vas a ver acá la recomendación y sus métricas clave.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = result.decision.toSpanishLabel(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = result.decision.toDecisionAccentColor()
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
        color = AppSurfaceSubtle,
        border = BorderStroke(1.dp, AppBorder.copy(alpha = 0.7f))
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
private fun HomeConfigSummarySection(
    form: DriverConfigFormState,
    config: DriverConfig
) {
    AppCard(containerColor = AppSurface) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Configuración resumida",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Umbrales locales usados para calcular la recomendación.",
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
            HorizontalDivider(color = AppBorder)
            ReadOnlySettingRow(
                label = "Reglas activas",
                value = config.toRulesSummary()
            )
            ReadOnlySettingRow(
                label = "Zonas a evitar",
                value = config.avoidZones.count { it.enabled }.toZonesCountText()
            )
        }
    }
}

@Composable
private fun SettingsSection(
    uiState: MainUiState,
    onConfigInputChange: (DriverConfigFormField, String) -> Unit,
    onSaveConfig: () -> Unit,
    onResetConfig: () -> Unit
) {
    val form = uiState.configForm
    val config = uiState.lastConfig ?: DriverConfig.default()

    Text(
        text = "Configuración",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Ajustá los criterios locales que usa la app para recomendar aceptar, revisar o rechazar.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    ConfigGroupCard(
        title = "Rentabilidad mínima",
        subtitle = "Umbrales que debe cumplir una solicitud para verse rentable."
    ) {
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
    }

    ConfigGroupCard(
        title = "Costos estimados",
        subtitle = "Valores usados para estimar costo y ganancia neta."
    ) {
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
    }

    ConfigGroupCard(
        title = "Límites de búsqueda",
        subtitle = "La configuración actual no tiene límites máximos editables para llegada al pasajero."
    ) {
        ReadOnlySettingRow(
            label = "Distancia máxima hasta pasajero",
            value = "Sin límite editable"
        )
        ReadOnlySettingRow(
            label = "Minutos máximos hasta pasajero",
            value = "Sin límite editable"
        )
    }

    ConfigGroupCard(
        title = "Reglas de revisión y rechazo",
        subtitle = "Criterios defensivos cuando faltan datos o aparece una zona marcada."
    ) {
        ConfigNumberField(
            label = "Tolerancia de revisión %",
            value = form.reviewTolerancePercent,
            field = DriverConfigFormField.REVIEW_TOLERANCE_PERCENT,
            onConfigInputChange = onConfigInputChange
        )
        HorizontalDivider(color = AppBorder)
        ReadOnlySettingRow(
            label = "Tarifa no detectada",
            value = if (config.rejectIfUnknownFare) "Rechazar" else "Revisar"
        )
        ReadOnlySettingRow(
            label = "Distancia incompleta",
            value = if (config.rejectIfUnknownDistance) "Rechazar" else "Revisar"
        )
        ReadOnlySettingRow(
            label = "Zona bloqueada detectada",
            value = if (config.rejectIfAvoidZoneDetected) "Rechazar" else "Revisar"
        )
    }

    ConfigGroupCard(
        title = "Plataformas y zonas",
        subtitle = "Filtros disponibles en la configuración local actual."
    ) {
        ReadOnlySettingRow(
            label = "Plataformas habilitadas",
            value = "Sin filtro por plataforma"
        )
        if (config.avoidZones.isEmpty()) {
            ReadOnlySettingRow(
                label = "Zonas a evitar",
                value = "No hay zonas configuradas"
            )
        } else {
            config.avoidZones.forEach { zone ->
                ReadOnlySettingRow(
                    label = zone.name,
                    value = if (zone.enabled) zone.policy.name.toZonePolicyLabel() else "Inactiva"
                )
            }
        }
    }

    uiState.configStatusMessage?.let { message ->
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AppPrimary
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            modifier = Modifier.weight(1f),
            onClick = onSaveConfig,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppPrimary,
                contentColor = Color(0xFF06251F)
            )
        ) {
            Text("Guardar")
        }
        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = onResetConfig,
            colors = darkOutlinedButtonColors()
        ) {
            Text("Restablecer")
        }
    }
}

@Composable
private fun ConfigGroupCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    AppCard(containerColor = AppSurface) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
private fun ReadOnlySettingRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            modifier = Modifier.weight(1f),
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
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
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppText,
            unfocusedTextColor = AppText,
            focusedLabelColor = AppPrimary,
            unfocusedLabelColor = AppMutedText,
            cursorColor = AppPrimary,
            focusedBorderColor = AppPrimary,
            unfocusedBorderColor = AppBorder,
            focusedContainerColor = AppSurfaceRaised,
            unfocusedContainerColor = AppSurfaceRaised
        )
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
    Text(
        text = "Diagnóstico",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Herramientas técnicas para revisar captura, OCR y overlay durante desarrollo.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    ConfigGroupCard(
        title = "Acciones técnicas",
        subtitle = "Usalas solo para validar el flujo manualmente."
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onCaptureFrame,
                colors = darkOutlinedButtonColors()
            ) {
                Text("Capturar frame")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onAnalyzeOcr,
                colors = darkOutlinedButtonColors()
            ) {
                Text("Analizar OCR")
            }
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onStopScreenCapture,
            colors = darkOutlinedButtonColors()
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
                onClick = onRunSimulatedDecision,
                colors = darkOutlinedButtonColors()
            ) {
                Text("Overlay simulado")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onStopService,
                colors = darkOutlinedButtonColors()
            ) {
                Text("Detener overlay")
            }
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = overlayActionsEnabled,
            onClick = onShowLastRealDecisionOverlay,
            colors = darkOutlinedButtonColors()
        ) {
            Text("Mostrar última decisión real")
        }
    }

    ConfigGroupCard(
        title = "Estado técnico",
        subtitle = "Lecturas actuales de permisos, servicios y análisis."
    ) {
        ReadOnlySettingRow(
            label = "Ventana flotante",
            value = uiState.overlayPermissionStatus
        )
        ReadOnlySettingRow(
            label = "Captura",
            value = uiState.screenCapturePermissionStatus
        )
        ReadOnlySettingRow(
            label = "Monitoreo",
            value = uiState.monitorStatus
        )
        ReadOnlySettingRow(
            label = "Servicio overlay",
            value = uiState.serviceStatus
        )
        ReadOnlySettingRow(
            label = "Análisis",
            value = uiState.ocrStatus
        )
        uiState.lastCapturedFrameTimestamp?.let { timestamp ->
            ReadOnlySettingRow(
                label = "Última captura",
                value = timestamp.toDisplayTime()
            )
        }
        if (uiState.lastCapturedFrameWidth != null && uiState.lastCapturedFrameHeight != null) {
            ReadOnlySettingRow(
                label = "Tamaño de frame",
                value = "${uiState.lastCapturedFrameWidth} x ${uiState.lastCapturedFrameHeight}"
            )
        }
        uiState.monitorOverlayStatus?.let { status ->
            ReadOnlySettingRow(
                label = "Último overlay",
                value = status
            )
        }
        uiState.decisionStatusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        uiState.screenCaptureErrorMessage?.let { errorMessage ->
            Text(
                text = "Captura: $errorMessage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        uiState.ocrErrorMessage?.let { errorMessage ->
            Text(
                text = "OCR: $errorMessage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    ConfigGroupCard(
        title = "Último texto OCR",
        subtitle = "Texto reconocido más reciente, limitado para no ocupar toda la pantalla."
    ) {
        val text = uiState.lastRecognizedText?.takeIf { it.isNotBlank() }
        if (text == null) {
            Text(
                text = "Todavía no hay texto reconocido.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp),
                shape = MaterialTheme.shapes.small,
                color = AppSurfaceSubtle,
                border = BorderStroke(1.dp, AppBorder)
            ) {
                Text(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    text = text,
                    style = MaterialTheme.typography.bodySmall
                )
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

@Composable
private fun DriverDecision?.toDecisionContainerColor(): Color {
    return when (this) {
        DriverDecision.ACCEPT -> Color(0xFF102A24)
        DriverDecision.REJECT -> AppErrorDim
        DriverDecision.REVIEW -> AppWarningDim
        null -> AppSurface
    }
}

private fun DriverDecision.toDecisionAccentColor(): Color {
    return when (this) {
        DriverDecision.ACCEPT -> AppPrimary
        DriverDecision.REJECT -> AppError
        DriverDecision.REVIEW -> AppWarning
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

private fun DriverConfig.toRulesSummary(): String {
    val activeRules = listOf(
        rejectIfUnknownFare,
        rejectIfUnknownDistance,
        rejectIfAvoidZoneDetected
    ).count { it }

    return "$activeRules de 3 reglas en rechazo"
}

private fun Int.toZonesCountText(): String {
    return when (this) {
        0 -> "Ninguna activa"
        1 -> "1 zona activa"
        else -> "$this zonas activas"
    }
}

private fun String.toZonePolicyLabel(): String {
    return when (this) {
        "REJECT" -> "Rechazar"
        "REVIEW" -> "Revisar"
        else -> "Activa"
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
