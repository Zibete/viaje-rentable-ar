package com.zibete.driverassistant.location

import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FusedDriverLocationProvider(
    context: Context,
    private val permissionChecker: LocationPermissionChecker = LocationPermissionChecker(context)
) : DriverLocationProvider {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    override suspend fun getLastKnownLocation(): DriverLocationResult {
        if (!permissionChecker.hasLocationPermission()) {
            return DriverLocationResult.permissionMissing()
        }

        return runCatching {
            fusedLocationClient.lastLocation.awaitResult()
        }.fold(
            onSuccess = { location ->
                location?.toDriverLocationResult() ?: DriverLocationResult.unavailable()
            },
            onFailure = { error ->
                DriverLocationResult.error(error.message)
            }
        )
    }

    private fun Location.toDriverLocationResult(): DriverLocationResult {
        return DriverLocationResult.available(
            DriverLocation(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = if (hasAccuracy()) accuracy else null,
                capturedAtMillis = time.takeIf { it > 0L }
            )
        )
    }

    private suspend fun Task<Location>.awaitResult(): Location? {
        return suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { location ->
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }
            addOnFailureListener { error ->
                if (continuation.isActive) {
                    continuation.resumeWith(Result.failure(error))
                }
            }
            addOnCanceledListener {
                continuation.cancel()
            }
        }
    }
}
