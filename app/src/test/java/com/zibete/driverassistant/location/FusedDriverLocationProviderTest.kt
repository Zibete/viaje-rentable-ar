package com.zibete.driverassistant.location

import com.google.android.gms.location.Priority
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FusedDriverLocationProviderTest {
    @Test
    fun lastLocationNullAndCurrentLocationNullFallsBackToOneShotUpdate() = runBlocking {
        val oneShotLocation = DriverLocation(
            latitude = -34.5,
            longitude = -58.4,
            accuracyMeters = 18f,
            capturedAtMillis = 1_700_000_000_000L
        )
        val locationClient = FakeFusedDriverLocationClient(
            lastLocation = null,
            currentLocation = null,
            singleUpdateLocation = oneShotLocation
        )
        val provider = FusedDriverLocationProvider(
            permissionChecker = FakeDriverLocationPermissionChecker(hasPermission = true),
            locationClient = locationClient
        )

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
        val lastLocation = DriverLocation(
            latitude = -34.5,
            longitude = -58.4,
            accuracyMeters = 25f,
            capturedAtMillis = 1_700_000_000_000L
        )
        val locationClient = FakeFusedDriverLocationClient(lastLocation = lastLocation)
        val provider = FusedDriverLocationProvider(
            permissionChecker = FakeDriverLocationPermissionChecker(hasPermission = true),
            locationClient = locationClient
        )

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
        val provider = FusedDriverLocationProvider(
            permissionChecker = FakeDriverLocationPermissionChecker(hasPermission = true),
            locationClient = locationClient
        )

        val result = provider.getLastKnownLocation()

        assertEquals(DriverLocationStatus.LOCATION_UNAVAILABLE, result.status)
        assertEquals(1, locationClient.lastLocationCalls)
        assertEquals(1, locationClient.currentLocationCalls)
        assertEquals(1, locationClient.singleUpdateCalls)
    }

    @Test
    fun missingPermissionDoesNotAskLocationClient() = runBlocking {
        val locationClient = FakeFusedDriverLocationClient(
            lastLocation = DriverLocation(
                latitude = -34.5,
                longitude = -58.4,
                accuracyMeters = null,
                capturedAtMillis = null
            )
        )
        val provider = FusedDriverLocationProvider(
            permissionChecker = FakeDriverLocationPermissionChecker(hasPermission = false),
            locationClient = locationClient
        )

        val result = provider.getLastKnownLocation()

        assertEquals(DriverLocationStatus.PERMISSION_MISSING, result.status)
        assertEquals(0, locationClient.lastLocationCalls)
        assertEquals(0, locationClient.currentLocationCalls)
        assertEquals(0, locationClient.singleUpdateCalls)
    }

    private class FakeDriverLocationPermissionChecker(
        private val hasPermission: Boolean
    ) : DriverLocationPermissionChecker {
        override fun hasLocationPermission(): Boolean = hasPermission
    }

    private class FakeFusedDriverLocationClient(
        private val lastLocation: DriverLocation? = null,
        private val currentLocation: DriverLocation? = null,
        private val singleUpdateLocation: DriverLocation? = null
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
    }
}
