package com.zibete.driverassistant.location

import android.content.Context
import android.location.Location
import android.os.Looper
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
    private val locationClient: FusedDriverLocationClient,
    private val locationCache: DriverLocationCache = DriverLocationCache()
) : DriverLocationProvider, DriverLocationTracker {
    private var updatesHandle: DriverLocationUpdatesHandle? = null

    constructor(
        context: Context,
        locationCache: DriverLocationCache = DriverLocationCache()
    ) : this(
        permissionChecker = LocationPermissionChecker(context),
        locationClient = AndroidFusedDriverLocationClient(context.applicationContext),
        locationCache = locationCache
    )

    override fun startLocationUpdates() {
        if (updatesHandle != null) {
            DriverAssistantDebugLogger.log("location cache started", "alreadyStarted=true")
            return
        }

        val hasPermission = permissionChecker.hasLocationPermission()
        DriverAssistantDebugLogger.log(
            "location cache started",
            "hasLocationPermission=$hasPermission, priority=BALANCED_POWER_ACCURACY, intervalMs=$TRACKING_INTERVAL_MILLIS"
        )
        if (!hasPermission) return

        updatesHandle = locationClient.startLocationUpdates(
            priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            intervalMillis = TRACKING_INTERVAL_MILLIS,
            minIntervalMillis = TRACKING_MIN_INTERVAL_MILLIS
        ) { location ->
            val updated = locationCache.update(location)
            DriverAssistantDebugLogger.log(
                "cached location update",
                "available=$updated, accuracyMeters=${location.accuracyMeters}, capturedAtMillis=${location.capturedAtMillis}"
            )
        }
    }

    override fun stopLocationUpdates() {
        val hadUpdates = updatesHandle != null
        updatesHandle?.stop()
        updatesHandle = null
        DriverAssistantDebugLogger.log("location cache stopped", "hadUpdates=$hadUpdates")
    }

    override suspend fun getLastKnownLocation(): DriverLocationResult {
        val hasPermission = permissionChecker.hasLocationPermission()
        DriverAssistantDebugLogger.log(
            "home location permission",
            "hasLocationPermission=$hasPermission"
        )
        if (!hasPermission) {
            return DriverLocationResult.permissionMissing()
        }

        val cachedLocation = locationCache.getFreshLocation()
        DriverAssistantDebugLogger.log(
            "cached location available",
            "available=${cachedLocation != null}, ageMs=${cachedLocation?.ageMillis}"
        )
        if (cachedLocation != null) {
            DriverAssistantDebugLogger.log("home current location", "source=cachedLocation, available=true")
            return DriverLocationResult.available(cachedLocation.location)
        }

        val lastLocationResult = runCatching {
            locationClient.getLastLocation()
        }
        val lastLocation = lastLocationResult.getOrNull()
        if (lastLocation != null) {
            DriverAssistantDebugLogger.log("home current location", "source=lastLocation, available=true")
            locationCache.update(lastLocation)
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
            locationCache.update(currentLocation)
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
                location?.let { locationCache.update(it) }
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
        const val TRACKING_INTERVAL_MILLIS = 15_000L
        const val TRACKING_MIN_INTERVAL_MILLIS = 8_000L
    }
}

internal interface FusedDriverLocationClient {
    suspend fun getLastLocation(): DriverLocation?

    suspend fun getCurrentLocation(priority: Int, timeoutMillis: Long): DriverLocation?

    suspend fun requestSingleUpdate(priority: Int, timeoutMillis: Long): DriverLocation?

    fun startLocationUpdates(
        priority: Int,
        intervalMillis: Long,
        minIntervalMillis: Long,
        onLocation: (DriverLocation) -> Unit
    ): DriverLocationUpdatesHandle
}

internal fun interface DriverLocationUpdatesHandle {
    fun stop()
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

    override fun startLocationUpdates(
        priority: Int,
        intervalMillis: Long,
        minIntervalMillis: Long,
        onLocation: (DriverLocation) -> Unit
    ): DriverLocationUpdatesHandle {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations
                    .map { location -> location.toDriverLocation() }
                    .forEach(onLocation)
            }
        }
        val request = LocationRequest.Builder(priority, intervalMillis)
            .setMinUpdateIntervalMillis(minIntervalMillis)
            .setMaxUpdateDelayMillis(intervalMillis * 2)
            .setWaitForAccurateLocation(false)
            .build()

        fusedLocationClient.requestLocationUpdates(
            request,
            callback,
            Looper.getMainLooper()
        ).addOnFailureListener { error ->
            DriverAssistantDebugLogger.log(
                "location cache start failed",
                "error=${error.message}"
            )
        }

        return DriverLocationUpdatesHandle {
            fusedLocationClient.removeLocationUpdates(callback)
        }
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
