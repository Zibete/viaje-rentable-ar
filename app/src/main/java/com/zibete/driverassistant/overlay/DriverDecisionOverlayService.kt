package com.zibete.driverassistant.overlay

import android.app.Service
import android.content.Intent
import android.os.IBinder

class DriverDecisionOverlayService : Service() {
    private var currentState: OverlayCardState? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        currentState = null
        super.onDestroy()
    }

    fun updateOverlayState(state: OverlayCardState) {
        currentState = state
    }
}

