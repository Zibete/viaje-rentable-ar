package com.zibete.driverassistant.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager

class ScreenCaptureManager(
    context: Context
) {
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

    fun stopSession(): ScreenCaptureSession {
        currentSession = ScreenCaptureSession(status = ScreenCaptureStatus.STOPPED)
        return currentSession
    }
}
