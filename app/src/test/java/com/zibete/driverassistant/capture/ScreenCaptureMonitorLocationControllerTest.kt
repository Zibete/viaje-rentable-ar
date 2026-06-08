package com.zibete.driverassistant.capture

import com.zibete.driverassistant.location.DriverLocationTracker
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenCaptureMonitorLocationControllerTest {
    @Test
    fun startsAndStopsTrackingOnce() {
        val tracker = FakeDriverLocationTracker()
        val controller = ScreenCaptureMonitorLocationController(tracker)

        controller.start()
        controller.start()
        controller.stop()
        controller.stop()

        assertEquals(1, tracker.startCalls)
        assertEquals(1, tracker.stopCalls)
    }

    private class FakeDriverLocationTracker : DriverLocationTracker {
        var startCalls = 0
            private set
        var stopCalls = 0
            private set

        override fun startLocationUpdates() {
            startCalls += 1
        }

        override fun stopLocationUpdates() {
            stopCalls += 1
        }
    }
}
