package com.zibete.driverassistant.overlay

import android.content.Context
import android.net.Uri
import android.provider.Settings

class OverlayPermissionHelper {
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun buildOverlaySettingsUri(context: Context): Uri {
        return Uri.parse("package:${context.packageName}")
    }
}

