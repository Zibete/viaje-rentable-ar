package com.zibete.driverassistant.location

import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.zibete.driverassistant.debug.DriverAssistantDebugLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class FusedDriverLocationProvider internal constructor(
    private val permissionChecker: DriverLocationPermissionChecker,
    private val locationClient: FusedDriverLocationClient
) : DriverLocationProvider {
    constructor(context: Context) : this(
        permissionChecker = LocationPermissionChecker(context),
        locationClient = AndroidFusedDriverLocationClient(context.applicationContext)
    )

    override suspend fun getLastKnownLocation(): DriverLocationResult {
        val hasPermission = permissionChecker.hasLocationPermission()
        DriverAssistantDebugLogger.log(
            "home location permission",
            "hasLocationPermission=$hasPermission"
        )
        if (!hasPermission) {
            return DriverLocationResult.permissionMissing()
        }

        val lastLocationResult = runCatching {
            locationClient.getLastLocation()
        }
        val lastLocation = lastLocationResult.getOrNull()
        if (lastLocation != null) {
            DriverAssistantDebugLogger.log("home current location", "source=lastLocation, available=true")
            return DriverLocationResult.available(lastLocation)
        }
        lastLocationResult.exceptionOrNull()?.let { error ->
            DriverAssistantDebugLogger.log(
                "home current location",
                "source=lastLocation, available=false, error=${error.message}"
            )
        }

        DriverAssistantDebugLogger.log(
            "home current location",
            "source=lastLocation, available=false, fallback=currentLocation"
        )

        val currentLocationResult = runCatching {
            locationClient.getCurrentLocation(
                priority = Priority.PRIORITY_HIGH_ACCURACY,
                timeoutMillis = CURRENT_LOCATION_TIMEOUT_MILLIS
            )
        }
        val currentLocation = currentLocationResult.getOrNull()
        if (currentLocation != null) {
            DriverAssistantDebugLogger.log("home current location", "source=currentLocation, available=true")
            return DriverLocationResult.available(currentLocation)
        }
        DriverAssistantDebugLogger.log(
            "home current location",
            "source=currentLocation, available=false, fallback=oneShotUpdate"
        )
        currentLocationResult.exceptionOrNull()?.let { error ->
            DriverAssistantDebugLogger.log(
                "home current location",
                "source=currentLocation, available=false, error=${error.message}"
            )
        }

        val oneShotLocationResult = runCatching {
            locationClient.requestSingleUpdate(
                priority = Priority.PRIORITY_HIGH_ACCURACY,
                timeoutMillis = ONE_SHOT_LOCATION_TIMEOUT_MILLIS
            )
        }
        return oneShotLocationResult.fold(
            onSuccess = { location ->
                DriverAssistantDebugLogger.log("home current location", "source=oneShotUpdate, available=${location != null}")
                location?.let(DriverLocationResult::available) ?: DriverLocationResult.unavailable()
            },
            onFailure = { error ->
                DriverAssistantDebugLogger.log(
                    "home current location",
                    "source=oneShotUpdate, available=false, error=${error.message}"
                )
                DriverLocationResult.error(error.message)
            }
        )
    }

    private companion object {
        const val CURRENT_LOCATION_TIMEOUT_MILLIS = 6_000L
        const val ONE_SHOT_LOCATION_TIMEOUT_MILLIS = 8_000L
    }
}

internal interface FusedDriverLocationClient {
    suspend fun getLastLocation(): DriverLocation?

    suspend fun getCurrentLocation(priority: Int, timeoutMillis: Long): DriverLocation?

    suspend fun requestSingleUpdate(priority: Int, timeoutMillis: Long): DriverLocation?
}

private class AndroidFusedDriverLocationClient(
    context: Context
) : FusedDriverLocationClient {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    override suspend fun getLastLocation(): DriverLocation? {
        return fusedLocationClient.lastLocation.awaitTaskResult()?.toDriverLocation()
    }

    override suspend fun getCurrentLocation(priority: Int, timeoutMillis: Long): DriverLocation? {
        val cancellationTokenSource = CancellationTokenSource()
        return withTimeoutOrNull(timeoutMillis) {
            fusedLocationClient.getCurrentLocation(
                priority,
                cancellationTokenSource.token
            ).awaitTaskResult()
        }.also { location ->
            if (location == null) {
                cancellationTokenSource.cancel()
            }
        }?.toDriverLocation()
    }

    override suspend fun requestSingleUpdate(priority: Int, timeoutMillis: Long): DriverLocation? {
        val location = withTimeoutOrNull(timeoutMillis) {
            awaitSingleLocationUpdate(priority)
        }
        return location?.toDriverLocation()
    }

    private suspend fun awaitSingleLocationUpdate(priority: Int): Location? {
        return suspendCancellableCoroutine { continuation ->
            lateinit var callback: LocationCallback
            callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: result.locations.firstOrNull()
                    if (location != null && continuation.isActive) {
                        continuation.resume(location)
                        fusedLocationClient.removeLocationUpdates(callback)
                    }
                }
            }

            val request = LocationRequest.Builder(priority, ONE_SHOT_UPDATE_INTERVAL_MILLIS)
                .setMaxUpdates(1)
                .setWaitForAccurateLocation(false)
                .build()

            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            ).addOnFailureListener { error ->
                if (continuation.isActive) {
                    continuation.resumeWith(Result.failure(error))
                }
            }.addOnCanceledListener {
                continuation.cancel()
            }

            continuation.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }
    }

    private fun Location.toDriverLocation(): DriverLocation {
        return DriverLocation(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = if (hasAccuracy()) accuracy else null,
            capturedAtMillis = time.takeIf { it > 0L }
        )
    }

    private suspend fun <T> Task<T>.awaitTaskResult(): T {
        return suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
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

    private companion object {
        const val ONE_SHOT_UPDATE_INTERVAL_MILLIS = 2_000L
    }
}
