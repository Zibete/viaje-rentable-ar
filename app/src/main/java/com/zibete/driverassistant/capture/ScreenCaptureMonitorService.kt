package com.zibete.driverassistant.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat
import com.zibete.driverassistant.calculator.TripDecisionResult
import com.zibete.driverassistant.config.DataStoreDriverConfigRepository
import com.zibete.driverassistant.config.DriverConfig
import com.zibete.driverassistant.debug.DriverAssistantDebugLogger
import com.zibete.driverassistant.ocr.MlKitScreenTextRecognizer
import com.zibete.driverassistant.ocr.OcrStatus
import com.zibete.driverassistant.ocr.TripOfferAnalysisResult
import com.zibete.driverassistant.ocr.TripOfferDecisionPipeline
import com.zibete.driverassistant.overlay.DriverDecisionOverlayService
import com.zibete.driverassistant.overlay.OverlayCardState
import com.zibete.driverassistant.overlay.hasCompleteOverlayData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

class ScreenCaptureMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val textRecognizer = MlKitScreenTextRecognizer()
    private val decisionPipeline = TripOfferDecisionPipeline()
    private val detectionState = TripOfferDetectionState()
    private val frameGate = OcrFrameGate()
    private val isProcessingFrame = AtomicBoolean(false)

    @Volatile
    private var currentConfig: DriverConfig = DriverConfig.default()

    private var mediaProjection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null
    private var nextFrameAtMillis: Long = 0L
    private var noOfferCycles: Int = 0
    private var overlayVisible: Boolean = false

    override fun onCreate() {
        super.onCreate()
        DataStoreDriverConfigRepository(context = applicationContext).let { repository ->
            serviceScope.launch {
                repository.config.collect { config ->
                    currentConfig = config
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_MONITORING) {
            publishStatus(ScreenCaptureMonitorStatus.STOPPED)
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action != ACTION_START_MONITORING) {
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultCode == 0 || resultData == null) {
            publishStatus(
                status = ScreenCaptureMonitorStatus.ERROR,
                errorMessage = "Falta el permiso autorizado de MediaProjection."
            )
            stopSelf()
            return START_NOT_STICKY
        }

        ensureNotificationChannel()
        startMonitorForeground()
        startMonitoring(resultCode, resultData)
        return START_STICKY
    }

    override fun onDestroy() {
        releaseCaptureResources()
        textRecognizer.close()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring(resultCode: Int, resultData: Intent) {
        if (mediaProjection != null) {
            publishStatus(ScreenCaptureMonitorStatus.MONITORING)
            return
        }

        val manager = getSystemService(MediaProjectionManager::class.java)
        val projection = runCatching { manager.getMediaProjection(resultCode, resultData) }
            .onFailure { error ->
                publishStatus(
                    status = ScreenCaptureMonitorStatus.ERROR,
                    errorMessage = error.message ?: "No se pudo crear MediaProjection."
                )
                stopSelf()
            }
            .getOrNull() ?: return

        mediaProjection = projection
        captureThread = HandlerThread("ScreenCaptureMonitor").apply { start() }
        val handler = Handler(requireNotNull(captureThread).looper)
        captureHandler = handler

        projectionCallback = object : MediaProjection.Callback() {
            override fun onStop() {
                publishStatus(
                    status = ScreenCaptureMonitorStatus.STOPPED,
                    errorMessage = "La sesion de monitoreo se detuvo."
                )
                stopSelf()
            }
        }
        projection.registerCallback(requireNotNull(projectionCallback), handler)

        val metrics = resources.displayMetrics
        val rawWidth = metrics.widthPixels.takeIf { it > 0 } ?: FALLBACK_CAPTURE_SIZE
        val rawHeight = metrics.heightPixels.takeIf { it > 0 } ?: FALLBACK_CAPTURE_SIZE
        val captureWidth = rawWidth.coerceAtMost(MAX_CAPTURE_WIDTH)
        val captureHeight = ((rawHeight.toFloat() * captureWidth) / rawWidth)
            .roundToInt()
            .coerceAtLeast(FALLBACK_CAPTURE_SIZE)
        val densityDpi = metrics.densityDpi.takeIf { it > 0 } ?: DisplayMetrics.DENSITY_DEFAULT

        imageReader = ImageReader.newInstance(
            captureWidth,
            captureHeight,
            PixelFormat.RGBA_8888,
            IMAGE_BUFFER_SIZE
        ).apply {
            setOnImageAvailableListener({ reader ->
                handleImageAvailable(reader)
            }, handler)
        }

        runCatching {
            virtualDisplay = projection.createVirtualDisplay(
                "DriverAssistantScreenMonitor",
                captureWidth,
                captureHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                handler
            )
            frameGate.reset()
            nextFrameAtMillis = 0L
            DriverAssistantDebugLogger.log(
                "monitor capture size",
                "raw=${rawWidth}x$rawHeight, capture=${captureWidth}x$captureHeight, maxWidth=$MAX_CAPTURE_WIDTH"
            )
            publishStatus(ScreenCaptureMonitorStatus.MONITORING)
        }.onFailure { error ->
            publishStatus(
                status = ScreenCaptureMonitorStatus.ERROR,
                errorMessage = error.message ?: "No se pudo iniciar el monitoreo de pantalla."
            )
            stopSelf()
        }
    }

    private fun handleImageAvailable(reader: ImageReader) {
        val now = System.currentTimeMillis()
        val image = runCatching { reader.acquireLatestImage() }.getOrNull() ?: return

        if (now < nextFrameAtMillis) {
            image.close()
            return
        }

        val bitmap = runCatching { image.toMonitorBitmap() }
            .onFailure { error ->
                image.close()
                publishStatus(
                    status = ScreenCaptureMonitorStatus.ERROR,
                    errorMessage = error.message ?: "No se pudo preparar el frame para OCR."
                )
            }
            .getOrNull() ?: return
        image.close()

        val gateDecision = runCatching {
            frameGate.evaluate(
                signature = bitmap.toMonitorFrameSignature(),
                nowMillis = now
            )
        }.onFailure { error ->
            bitmap.recycle()
            publishStatus(
                status = ScreenCaptureMonitorStatus.ERROR,
                errorMessage = error.message ?: "No se pudo evaluar el frame de monitoreo."
            )
        }.getOrNull() ?: return

        nextFrameAtMillis = now + gateDecision.nextFrameDelayMillis

        if (!gateDecision.shouldRunOcr) {
            DriverAssistantDebugLogger.log(
                "monitor OCR skipped",
                "reason=${gateDecision.reason}, changeScore=${gateDecision.changeScore}, " +
                    "nextDelay=${gateDecision.nextFrameDelayMillis}"
            )
            bitmap.recycle()
            return
        }

        if (!isProcessingFrame.compareAndSet(false, true)) {
            DriverAssistantDebugLogger.log("monitor OCR skipped", "reason=ocr already processing")
            bitmap.recycle()
            return
        }

        DriverAssistantDebugLogger.log(
            "monitor OCR started",
            "reason=${gateDecision.reason}, changeScore=${gateDecision.changeScore}, bitmap=${bitmap.width}x${bitmap.height}"
        )
        publishStatus(ScreenCaptureMonitorStatus.ANALYZING)

        textRecognizer.recognizeText(bitmap) { ocrResult ->
            bitmap.recycle()
            DriverAssistantDebugLogger.log(
                "monitor OCR completed",
                "status=${ocrResult.status}, textLength=${ocrResult.rawText?.length ?: 0}, error=${ocrResult.errorMessage}"
            )
            when (ocrResult.status) {
                OcrStatus.TEXT_DETECTED -> analyzeRecognizedText(ocrResult.rawText)
                OcrStatus.NO_TEXT -> markNoOffer(ocrResult.rawText, ocrResult.status)
                OcrStatus.ERROR -> publishStatus(
                    status = ScreenCaptureMonitorStatus.ERROR,
                    recognizedText = ocrResult.rawText,
                    ocrStatus = ocrResult.status,
                    errorMessage = ocrResult.errorMessage
                )
                OcrStatus.IDLE,
                OcrStatus.PROCESSING -> publishStatus(
                    status = ScreenCaptureMonitorStatus.MONITORING,
                    recognizedText = ocrResult.rawText,
                    ocrStatus = ocrResult.status
                )
            }
            isProcessingFrame.set(false)
        }
    }

    private fun analyzeRecognizedText(rawText: String?) {
        DriverAssistantDebugLogger.log("monitor analyze textLength", rawText?.length ?: 0)
        when (val analysis = decisionPipeline.analyzeRecognizedText(rawText, currentConfig)) {
            TripOfferAnalysisResult.NoText -> {
                DriverAssistantDebugLogger.log("monitor analysis", "NoText")
                markNoOffer(rawText, OcrStatus.NO_TEXT)
            }
            TripOfferAnalysisResult.NoTripDetected -> {
                DriverAssistantDebugLogger.log("monitor analysis", "NoTripDetected")
                markNoOffer(rawText, OcrStatus.TEXT_DETECTED)
            }
            is TripOfferAnalysisResult.DecisionReady -> {
                noOfferCycles = 0
                val result = analysis.result
                val hasCompleteData = result.hasCompleteOverlayData()
                val shouldShowOverlay = !hasCompleteData ||
                    detectionState.shouldShowOverlay(analysis.input)
                val overlayUpdated = if (shouldShowOverlay) {
                    startDecisionOverlay(result)
                } else {
                    false
                }
                if (overlayUpdated && hasCompleteData) {
                    detectionState.markOverlayShown(analysis.input)
                }
                DriverAssistantDebugLogger.log(
                    "monitor decision summary",
                    "hasCompleteData=$hasCompleteData, shouldShowOverlay=$shouldShowOverlay, " +
                        "overlayUpdated=$overlayUpdated, decision=${result.decision}"
                )
                publishStatus(
                    status = if (hasCompleteData) {
                        ScreenCaptureMonitorStatus.OFFER_DETECTED
                    } else {
                        ScreenCaptureMonitorStatus.INCOMPLETE_DATA
                    },
                    recognizedText = rawText,
                    ocrStatus = OcrStatus.TEXT_DETECTED,
                    decisionResult = result,
                    overlayUpdated = overlayUpdated
                )
            }
        }
    }

    private fun markNoOffer(rawText: String?, ocrStatus: OcrStatus) {
        noOfferCycles += 1
        val status = if (noOfferCycles >= NO_OFFER_STATUS_THRESHOLD) {
            detectionState.reset()
            hideDecisionOverlay("no active offer")
            ScreenCaptureMonitorStatus.NO_OFFER_DETECTED
        } else {
            ScreenCaptureMonitorStatus.MONITORING
        }
        publishStatus(
            status = status,
            recognizedText = rawText,
            ocrStatus = ocrStatus
        )
    }

    private fun startDecisionOverlay(result: TripDecisionResult): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            publishStatus(
                status = ScreenCaptureMonitorStatus.ERROR,
                errorMessage = "Oferta detectada, pero falta permiso overlay para mostrar la decision."
            )
            return false
        }

        val overlayState = OverlayCardState.fromDecisionResult(result)
        val intent = Intent(this, DriverDecisionOverlayService::class.java).apply {
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
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        overlayVisible = true
        return true
    }

    private fun hideDecisionOverlay(reason: String) {
        if (!overlayVisible) return
        DriverAssistantDebugLogger.log("monitor overlay hidden", reason)
        stopService(Intent(this, DriverDecisionOverlayService::class.java))
        overlayVisible = false
        detectionState.reset()
    }

    private fun publishStatus(
        status: ScreenCaptureMonitorStatus,
        recognizedText: String? = null,
        ocrStatus: OcrStatus = OcrStatus.IDLE,
        errorMessage: String? = null,
        decisionResult: TripDecisionResult? = null,
        overlayUpdated: Boolean = false
    ) {
        val intent = Intent(ACTION_MONITOR_RESULT).apply {
            putExtra(EXTRA_MONITOR_STATUS, status.name)
            putExtra(EXTRA_RECOGNIZED_TEXT, recognizedText)
            putExtra(EXTRA_OCR_STATUS, ocrStatus.name)
            putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
            putExtra(EXTRA_OVERLAY_UPDATED, overlayUpdated)
            decisionResult?.writeToIntent(this)
        }
        sendBroadcast(intent.setPackage(packageName))
    }

    private fun TripDecisionResult.writeToIntent(intent: Intent) {
        intent.putExtra(EXTRA_DECISION, decision.name)
        fareAmount?.let { intent.putExtra(EXTRA_FARE_AMOUNT, it) }
        arsPerKm?.let { intent.putExtra(EXTRA_ARS_PER_KM, it) }
        arsPerHour?.let { intent.putExtra(EXTRA_ARS_PER_HOUR, it) }
        estimatedCost?.let { intent.putExtra(EXTRA_ESTIMATED_COST, it) }
        estimatedNetProfit?.let { intent.putExtra(EXTRA_ESTIMATED_NET_PROFIT, it) }
        totalKm?.let { intent.putExtra(EXTRA_TOTAL_KM, it) }
        totalMinutes?.let { intent.putExtra(EXTRA_TOTAL_MINUTES, it) }
        intent.putStringArrayListExtra(EXTRA_REJECTION_REASONS, ArrayList(rejectionReasons))
        intent.putStringArrayListExtra(EXTRA_REVIEW_REASONS, ArrayList(reviewReasons))
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Monitoreo de pantalla",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitorea pantalla con captura autorizada para detectar ofertas."
            setShowBadge(false)
        }

        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startMonitorForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, ScreenCaptureMonitorService::class.java).apply {
            action = ACTION_STOP_MONITORING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            STOP_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Monitoreo de pantalla activo")
            .setContentText("Analizando ofertas con captura autorizada.")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Detener",
                stopPendingIntent
            )
            .build()
    }

    private fun releaseCaptureResources() {
        frameGate.reset()
        detectionState.reset()
        isProcessingFrame.set(false)
        overlayVisible = false
        runCatching { virtualDisplay?.release() }
        runCatching { imageReader?.close() }
        projectionCallback?.let { callback ->
            runCatching { mediaProjection?.unregisterCallback(callback) }
        }
        runCatching { mediaProjection?.stop() }
        runCatching { captureThread?.quitSafely() }

        virtualDisplay = null
        imageReader = null
        projectionCallback = null
        mediaProjection = null
        captureThread = null
        captureHandler = null
    }

    companion object {
        const val ACTION_START_MONITORING = "com.zibete.driverassistant.capture.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.zibete.driverassistant.capture.STOP_MONITORING"
        const val ACTION_MONITOR_RESULT = "com.zibete.driverassistant.capture.MONITOR_RESULT"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_MONITOR_STATUS = "extra_monitor_status"
        const val EXTRA_RECOGNIZED_TEXT = "extra_recognized_text"
        const val EXTRA_OCR_STATUS = "extra_ocr_status"
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"
        const val EXTRA_OVERLAY_UPDATED = "extra_overlay_updated"
        const val EXTRA_DECISION = "extra_decision"
        const val EXTRA_FARE_AMOUNT = "extra_fare_amount"
        const val EXTRA_ARS_PER_KM = "extra_ars_per_km"
        const val EXTRA_ARS_PER_HOUR = "extra_ars_per_hour"
        const val EXTRA_ESTIMATED_COST = "extra_estimated_cost"
        const val EXTRA_ESTIMATED_NET_PROFIT = "extra_estimated_net_profit"
        const val EXTRA_TOTAL_KM = "extra_total_km"
        const val EXTRA_TOTAL_MINUTES = "extra_total_minutes"
        const val EXTRA_REJECTION_REASONS = "extra_rejection_reasons"
        const val EXTRA_REVIEW_REASONS = "extra_review_reasons"

        private const val NOTIFICATION_CHANNEL_ID = "screen_capture_monitor_service"
        private const val NOTIFICATION_ID = 4301
        private const val STOP_REQUEST_CODE = 4302
        private const val MAX_CAPTURE_WIDTH = 720
        private const val NO_OFFER_STATUS_THRESHOLD = 2
        private const val IMAGE_BUFFER_SIZE = 1
        private const val FALLBACK_CAPTURE_SIZE = 1

        fun buildStartIntent(
            context: Context,
            resultCode: Int,
            resultData: Intent
        ): Intent {
            return Intent(context, ScreenCaptureMonitorService::class.java).apply {
                action = ACTION_START_MONITORING
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
        }
    }
}

private fun Image.toMonitorBitmap(): Bitmap {
    val plane = planes.first()
    val buffer = plane.buffer
    val pixelStride = plane.pixelStride
    val rowStride = plane.rowStride
    val bitmapWidth = rowStride / pixelStride
    val paddedBitmap = Bitmap.createBitmap(bitmapWidth, height, Bitmap.Config.ARGB_8888)
    paddedBitmap.copyPixelsFromBuffer(buffer)
    return if (bitmapWidth == width) {
        paddedBitmap
    } else {
        Bitmap.createBitmap(paddedBitmap, 0, 0, width, height).also {
            paddedBitmap.recycle()
        }
    }
}

private fun Bitmap.toMonitorFrameSignature(
    columns: Int = 8,
    rows: Int = 8,
    cropTopRatio: Float = 0.45f
): FrameSignature {
    val startY = (height * cropTopRatio).roundToInt().coerceIn(0, height - 1)
    val roiHeight = (height - startY).coerceAtLeast(1)
    val values = ArrayList<Int>(columns * rows)

    repeat(rows) { row ->
        repeat(columns) { column ->
            val x = (((column + 0.5f) / columns) * width)
                .roundToInt()
                .coerceIn(0, width - 1)
            val y = startY + ((((row + 0.5f) / rows) * roiHeight)
                .roundToInt()
                .coerceIn(0, roiHeight - 1))
            values += getPixel(x, y).luminance()
        }
    }

    return FrameSignature(values)
}

private fun Int.luminance(): Int {
    return ((Color.red(this) * 0.299f) +
        (Color.green(this) * 0.587f) +
        (Color.blue(this) * 0.114f))
        .roundToInt()
        .coerceIn(0, 255)
}
