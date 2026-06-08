package com.zibete.driverassistant.capture

import com.zibete.driverassistant.location.DriverLocationTracker

internal class ScreenCaptureMonitorLocationController(
    private val locationTracker: DriverLocationTracker
) {
    private var isStarted = false

    fun start() {
        if (isStarted) return

        locationTracker.startLocationUpdates()
        isStarted = true
    }

    fun stop() {
        if (!isStarted) return

        locationTracker.stopLocationUpdates()
        isStarted = false
    }
}
