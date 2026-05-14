package com.zibete.driverassistant.capture

enum class ScreenCaptureMonitorStatus {
    STOPPED,
    WAITING_PERMISSION,
    MONITORING,
    ANALYZING,
    OFFER_DETECTED,
    NO_OFFER_DETECTED,
    ERROR
}
