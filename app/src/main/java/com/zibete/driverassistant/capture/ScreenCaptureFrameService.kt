package com.zibete.driverassistant.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureFrameService : Service() {
    private var mediaProjection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var captureThread: HandlerThread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_CAPTURE_ONCE) {
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
            publishError("Falta el permiso autorizado de MediaProjection.")
            stopSelf()
            return START_NOT_STICKY
        }

        ensureNotificationChannel()
        startCaptureForeground()
        captureFrame(resultCode, resultData)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releaseCaptureResources()
        super.onDestroy()
    }

    private fun captureFrame(resultCode: Int, resultData: Intent) {
        val manager = getSystemService(MediaProjectionManager::class.java)
        val projection = runCatching { manager.getMediaProjection(resultCode, resultData) }
            .onFailure { error ->
                publishError(error.message ?: "No se pudo crear MediaProjection.")
                stopSelf()
            }
            .getOrNull() ?: return

        mediaProjection = projection
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.takeIf { it > 0 } ?: FALLBACK_CAPTURE_SIZE
        val height = metrics.heightPixels.takeIf { it > 0 } ?: FALLBACK_CAPTURE_SIZE
        val densityDpi = metrics.densityDpi.takeIf { it > 0 } ?: DisplayMetrics.DENSITY_DEFAULT
        val finished = AtomicBoolean(false)
        captureThread = HandlerThread("ScreenCaptureOnce").apply { start() }
        val captureHandler = Handler(requireNotNull(captureThread).looper)

        fun finish(sessionIntent: Intent) {
            if (!finished.compareAndSet(false, true)) {
                return
            }

            sendBroadcast(sessionIntent.setPackage(packageName))
            stopSelf()
        }

        projectionCallback = object : MediaProjection.Callback() {
            override fun onStop() {
                if (!finished.get()) {
                    finish(buildErrorIntent("La sesion de captura se detuvo antes de obtener un frame."))
                }
            }
        }
        projection.registerCallback(requireNotNull(projectionCallback), captureHandler)

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1).apply {
            setOnImageAvailableListener(
                { reader ->
                    val image = runCatching { reader.acquireLatestImage() }.getOrNull()
                    if (image == null) {
                        finish(buildErrorIntent("No se pudo leer el frame capturado."))
                        return@setOnImageAvailableListener
                    }

                    val resultIntent = Intent(ACTION_CAPTURE_RESULT).apply {
                        putExtra(EXTRA_STATUS, ScreenCaptureStatus.CAPTURE_AVAILABLE.name)
                        putExtra(EXTRA_FRAME_WIDTH, image.width)
                        putExtra(EXTRA_FRAME_HEIGHT, image.height)
                        putExtra(EXTRA_CAPTURED_AT_MILLIS, System.currentTimeMillis())
                    }
                    image.close()
                    finish(resultIntent)
                },
                captureHandler
            )
        }

        runCatching {
            virtualDisplay = projection.createVirtualDisplay(
                "DriverAssistantFrameCapture",
                width,
                height,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                captureHandler
            )
        }.onFailure { error ->
            finish(buildErrorIntent(error.message ?: "No se pudo iniciar la captura de pantalla."))
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Captura de pantalla",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Captura un frame autorizado para analisis local."
            setShowBadge(false)
        }

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun startCaptureForeground() {
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
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Capturando pantalla")
            .setContentText("Tomando un frame autorizado para analisis local.")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun publishError(message: String) {
        sendBroadcast(buildErrorIntent(message).setPackage(packageName))
    }

    private fun buildErrorIntent(message: String): Intent {
        return Intent(ACTION_CAPTURE_RESULT).apply {
            putExtra(EXTRA_STATUS, ScreenCaptureStatus.ERROR.name)
            putExtra(EXTRA_ERROR_MESSAGE, message)
        }
    }

    private fun releaseCaptureResources() {
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
    }

    companion object {
        const val ACTION_CAPTURE_ONCE = "com.zibete.driverassistant.capture.CAPTURE_ONCE"
        const val ACTION_CAPTURE_RESULT = "com.zibete.driverassistant.capture.CAPTURE_RESULT"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_FRAME_WIDTH = "extra_frame_width"
        const val EXTRA_FRAME_HEIGHT = "extra_frame_height"
        const val EXTRA_CAPTURED_AT_MILLIS = "extra_captured_at_millis"
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"

        private const val NOTIFICATION_CHANNEL_ID = "screen_capture_frame_service"
        private const val NOTIFICATION_ID = 4201
        private const val FALLBACK_CAPTURE_SIZE = 1
    }
}
