package com.zibete.driverassistant.location

interface DriverLocationProvider {
    suspend fun getLastKnownLocation(): DriverLocationResult
}
