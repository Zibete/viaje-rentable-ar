package com.zibete.driverassistant.ocr

data class OcrTextResult(
    val status: OcrStatus,
    val rawText: String? = null,
    val errorMessage: String? = null
)
