package com.zibete.driverassistant.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import com.zibete.driverassistant.ocr.OcrStatus

class ScreenCaptureManager(
    context: Context
) {
    private val appContext = context.applicationContext
    private val mediaProjectionManager = context.getSystemService(MediaProjectionManager::class.java)
    private var currentSession: ScreenCaptureSession = ScreenCaptureSession(
        status = ScreenCaptureStatus.STOPPED
    )

    fun buildPermissionIntent(): Intent {
        return mediaProjectionManager.createScreenCaptureIntent()
    }

    fun handlePermissionResult(resultCode: Int, data: Intent?): ScreenCaptureSession {
        currentSession = if (resultCode == Activity.RESULT_OK && data != null) {
            ScreenCaptureSession(
                status = ScreenCaptureStatus.AUTHORIZED,
                resultCode = resultCode,
                resultData = data
            )
        } else {
            ScreenCaptureSession(
                status = ScreenCaptureStatus.ERROR,
                errorMessage = "Permiso de captura cancelado o no autorizado."
            )
        }
        return currentSession
    }

    fun captureOnce(): ScreenCaptureSession {
        val resultCode = currentSession.resultCode
        val resultData = currentSession.resultData
        if (resultCode == null || resultData == null || currentSession.status == ScreenCaptureStatus.STOPPED) {
            currentSession = currentSession.copy(
                status = ScreenCaptureStatus.ERROR,
                errorMessage = "La captura no esta autorizada. Solicita permiso de captura primero."
            )
            return currentSession
        }

        val serviceIntent = Intent(appContext, ScreenCaptureFrameService::class.java).apply {
            action = ScreenCaptureFrameService.ACTION_CAPTURE_ONCE
            putExtra(ScreenCaptureFrameService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenCaptureFrameService.EXTRA_RESULT_DATA, resultData)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            appContext.startForegroundService(serviceIntent)
        } else {
            appContext.startService(serviceIntent)
        }

        currentSession = currentSession.copy(
            status = ScreenCaptureStatus.PENDING,
            lastFrame = null,
            ocrStatus = OcrStatus.PROCESSING,
            recognizedText = null,
            ocrErrorMessage = null,
            errorMessage = null
        )
        return currentSession
    }

    fun handleCaptureResult(intent: Intent): ScreenCaptureSession {
        val status = intent.getStringExtra(ScreenCaptureFrameService.EXTRA_STATUS)
            ?.let { runCatching { ScreenCaptureStatus.valueOf(it) }.getOrNull() }
            ?: ScreenCaptureStatus.ERROR
        val frame = if (status == ScreenCaptureStatus.CAPTURE_AVAILABLE) {
            ScreenCaptureFrame(
                width = intent.getIntExtra(ScreenCaptureFrameService.EXTRA_FRAME_WIDTH, 0),
                height = intent.getIntExtra(ScreenCaptureFrameService.EXTRA_FRAME_HEIGHT, 0),
                capturedAtMillis = intent.getLongExtra(
                    ScreenCaptureFrameService.EXTRA_CAPTURED_AT_MILLIS,
                    System.currentTimeMillis()
                )
            )
        } else {
            null
        }

        currentSession = currentSession.copy(
            status = status,
            resultCode = null,
            resultData = null,
            lastFrame = frame,
            ocrStatus = intent.getStringExtra(ScreenCaptureFrameService.EXTRA_OCR_STATUS)
                ?.let { runCatching { OcrStatus.valueOf(it) }.getOrNull() }
                ?: OcrStatus.IDLE,
            recognizedText = intent.getStringExtra(ScreenCaptureFrameService.EXTRA_RECOGNIZED_TEXT),
            ocrErrorMessage = intent.getStringExtra(ScreenCaptureFrameService.EXTRA_OCR_ERROR_MESSAGE),
            errorMessage = intent.getStringExtra(ScreenCaptureFrameService.EXTRA_ERROR_MESSAGE)
        )
        return currentSession
    }

    fun stopSession(): ScreenCaptureSession {
        appContext.stopService(Intent(appContext, ScreenCaptureFrameService::class.java))
        currentSession = ScreenCaptureSession(status = ScreenCaptureStatus.STOPPED)
        return currentSession
    }
}
