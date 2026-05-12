package com.zibete.driverassistant.capture

import android.content.Intent

data class ScreenCaptureSession(
    val status: ScreenCaptureStatus,
    val resultCode: Int? = null,
    val resultData: Intent? = null,
    val lastFrame: ScreenCaptureFrame? = null,
    val errorMessage: String? = null
)
