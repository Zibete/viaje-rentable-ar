package com.zibete.driverassistant.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

internal interface DriverLocationPermissionChecker {
    fun hasLocationPermission(): Boolean
}

class LocationPermissionChecker(
    context: Context
) : DriverLocationPermissionChecker {
    private val appContext = context.applicationContext

    override fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }
}
