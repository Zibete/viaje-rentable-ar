package com.zibete.driverassistant.location

import com.google.android.gms.location.Priority
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FusedDriverLocationProviderTest {
    @Test
    fun lastLocationNullAndCurrentLocationNullFallsBackToOneShotUpdate() = runBlocking {
        val oneShotLocation = location(accuracyMeters = 18f)
        val locationClient = FakeFusedDriverLocationClient(
            lastLocation = null,
            currentLocation = null,
            singleUpdateLocation = oneShotLocation
        )
        val provider = provider(locationClient = locationClient)

        val result = provider.getLastKnownLocation()

        assertEquals(DriverLocationStatus.AVAILABLE, result.status)
        assertEquals(oneShotLocation, result.location)
        assertEquals(1, locationClient.lastLocationCalls)
        assertEquals(1, locationClient.currentLocationCalls)
        assertEquals(1, locationClient.singleUpdateCalls)
        assertEquals(Priority.PRIORITY_HIGH_ACCURACY, locationClient.currentLocationPriority)
        assertEquals(Priority.PRIORITY_HIGH_ACCURACY, locationClient.singleUpdatePriority)
    }

    @Test
    fun lastLocationAvailableDoesNotAskForFallbacks() = runBlocking {
        val lastLocation = location(accuracyMeters = 25f)
        val locationClient = FakeFusedDriverLocationClient(lastLocation = lastLocation)
        val provider = provider(locationClient = locationClient)

        val result = provider.getLastKnownLocation()

        assertEquals(DriverLocationStatus.AVAILABLE, result.status)
        assertEquals(lastLocation, result.location)
        assertEquals(1, locationClient.lastLocationCalls)
        assertEquals(0, locationClient.currentLocationCalls)
        assertEquals(0, locationClient.singleUpdateCalls)
    }

    @Test
    fun allLocationSourcesNullReturnsUnavailable() = runBlocking {
        val locationClient = FakeFusedDriverLocationClient(
            lastLocation = null,
            currentLocation = null,
            singleUpdateLocation = null
        )
        val provider = provider(locationClient = locationClient)

        val result = provider.getLastKnownLocation()

        assertEquals(DriverLocationStatus.LOCATION_UNAVAILABLE, result.status)
        assertEquals(1, locationClient.lastLocationCalls)
        assertEquals(1, locationClient.currentLocationCalls)
        assertEquals(1, locationClient.singleUpdateCalls)
    }

    @Test
    fun missingPermissionDoesNotAskLocationClient() = runBlocking {
        val locationClient = FakeFusedDriverLocationClient(lastLocation = location())
        val provider = provider(
            locationClient = locationClient,
            hasPermission = false
        )

        val result = provider.getLastKnownLocation()

        assertEquals(DriverLocationStatus.PERMISSION_MISSING, result.status)
        assertEquals(0, locationClient.lastLocationCalls)
        assertEquals(0, locationClient.currentLocationCalls)
        assertEquals(0, locationClient.singleUpdateCalls)
    }

    @Test
    fun freshCachedLocationAvoidsFallbackLookups() = runBlocking {
        val cachedLocation = location(accuracyMeters = 12f)
        val cache = DriverLocationCache()
        cache.update(cachedLocation)
        val locationClient = FakeFusedDriverLocationClient()
        val provider = provider(
            locationClient = locationClient,
            locationCache = cache
        )

        val result = provider.getLastKnownLocation()

        assertEquals(DriverLocationStatus.AVAILABLE, result.status)
        assertEquals(cachedLocation, result.location)
        assertEquals(0, locationClient.lastLocationCalls)
        assertEquals(0, locationClient.currentLocationCalls)
        assertEquals(0, locationClient.singleUpdateCalls)
    }

    @Test
    fun startAndStopLocationUpdatesCachesReceivedLocation() {
        val trackedLocation = location(accuracyMeters = 30f)
        val cache = DriverLocationCache()
        val locationClient = FakeFusedDriverLocationClient(trackedUpdateLocation = trackedLocation)
        val provider = provider(
            locationClient = locationClient,
            locationCache = cache
        )

        provider.startLocationUpdates()
        provider.stopLocationUpdates()

        assertEquals(1, locationClient.startUpdatesCalls)
        assertEquals(1, locationClient.stopUpdatesCalls)
        assertEquals(Priority.PRIORITY_BALANCED_POWER_ACCURACY, locationClient.trackingPriority)
        assertEquals(trackedLocation, cache.getFreshLocation()?.location)
    }

    private fun provider(
        locationClient: FakeFusedDriverLocationClient,
        hasPermission: Boolean = true,
        locationCache: DriverLocationCache = DriverLocationCache()
    ): FusedDriverLocationProvider {
        return FusedDriverLocationProvider(
            permissionChecker = FakeDriverLocationPermissionChecker(hasPermission),
            locationClient = locationClient,
            locationCache = locationCache
        )
    }

    private fun location(accuracyMeters: Float? = null): DriverLocation {
        return DriverLocation(
            latitude = -34.5,
            longitude = -58.4,
            accuracyMeters = accuracyMeters,
            capturedAtMillis = 1_700_000_000_000L
        )
    }

    private class FakeDriverLocationPermissionChecker(
        private val hasPermission: Boolean
    ) : DriverLocationPermissionChecker {
        override fun hasLocationPermission(): Boolean = hasPermission
    }

    private class FakeFusedDriverLocationClient(
        private val lastLocation: DriverLocation? = null,
        private val currentLocation: DriverLocation? = null,
        private val singleUpdateLocation: DriverLocation? = null,
        private val trackedUpdateLocation: DriverLocation? = null
    ) : FusedDriverLocationClient {
        var lastLocationCalls = 0
            private set
        var currentLocationCalls = 0
            private set
        var singleUpdateCalls = 0
            private set
        var currentLocationPriority: Int? = null
            private set
        var singleUpdatePriority: Int? = null
            private set
        var startUpdatesCalls = 0
            private set
        var stopUpdatesCalls = 0
            private set
        var trackingPriority: Int? = null
            private set

        override suspend fun getLastLocation(): DriverLocation? {
            lastLocationCalls += 1
            return lastLocation
        }

        override suspend fun getCurrentLocation(priority: Int, timeoutMillis: Long): DriverLocation? {
            currentLocationCalls += 1
            currentLocationPriority = priority
            return currentLocation
        }

        override suspend fun requestSingleUpdate(priority: Int, timeoutMillis: Long): DriverLocation? {
            singleUpdateCalls += 1
            singleUpdatePriority = priority
            return singleUpdateLocation
        }

        override fun startLocationUpdates(
            priority: Int,
            intervalMillis: Long,
            minIntervalMillis: Long,
            onLocation: (DriverLocation) -> Unit
        ): DriverLocationUpdatesHandle {
            startUpdatesCalls += 1
            trackingPriority = priority
            trackedUpdateLocation?.let(onLocation)
            return DriverLocationUpdatesHandle {
                stopUpdatesCalls += 1
            }
        }
    }
}
