package com.zibete.driverassistant.location

import com.zibete.driverassistant.home.DestinationLocation
import com.zibete.driverassistant.home.HomeDirectionStatus
import com.zibete.driverassistant.home.HomeLocation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverLocationHomeDirectionResolverTest {
    private val home = HomeLocation(
        latitude = 0.0,
        longitude = 0.1,
        address = "Casa",
        label = null,
        resolvedAtMillis = null,
        provider = null
    )
    private val destination = DestinationLocation(
        latitude = 0.0,
        longitude = 0.05,
        sourceText = "Destino",
        confidence = 0.9
    )

    @Test
    fun fakeProviderSuccessProducesAvailableHomeDirection() = runBlocking {
        val provider = FakeDriverLocationProvider(
            DriverLocationResult.available(
                DriverLocation(
                    latitude = 0.0,
                    longitude = 0.0,
                    accuracyMeters = 20f,
                    capturedAtMillis = 1_700_000_000_000L
                )
            )
        )

        val result = DriverLocationHomeDirectionResolver(provider).calculate(
            homeLocation = home,
            destinationText = "Destino",
            destinationLocation = destination
        )

        assertEquals(HomeDirectionStatus.AVAILABLE, result.status)
        assertTrue(requireNotNull(result.score) >= 9)
    }

    @Test
    fun missingPermissionProducesMissingCurrentLocation() = runBlocking {
        val provider = FakeDriverLocationProvider(DriverLocationResult.permissionMissing())

        val result = DriverLocationHomeDirectionResolver(provider).calculate(
            homeLocation = home,
            destinationText = "Destino",
            destinationLocation = destination
        )

        assertEquals(HomeDirectionStatus.MISSING_CURRENT_LOCATION, result.status)
        assertEquals("Casa ?", result.shortLabel)
    }

    @Test
    fun unavailableLocationProducesMissingCurrentLocation() = runBlocking {
        val provider = FakeDriverLocationProvider(DriverLocationResult.unavailable())

        val result = DriverLocationHomeDirectionResolver(provider).calculate(
            homeLocation = home,
            destinationText = "Destino",
            destinationLocation = destination
        )

        assertEquals(HomeDirectionStatus.MISSING_CURRENT_LOCATION, result.status)
    }

    @Test
    fun missingDestinationDoesNotAskProvider() = runBlocking {
        val provider = FakeDriverLocationProvider(DriverLocationResult.error("should not be called"))

        val result = DriverLocationHomeDirectionResolver(provider).calculate(
            homeLocation = home,
            destinationText = null,
            destinationLocation = null
        )

        assertEquals(HomeDirectionStatus.MISSING_DESTINATION_TEXT, result.status)
        assertEquals(0, provider.callCount)
    }

    @Test
    fun missingHomeDoesNotAskProvider() = runBlocking {
        val provider = FakeDriverLocationProvider(DriverLocationResult.error("should not be called"))

        val result = DriverLocationHomeDirectionResolver(provider).calculate(
            homeLocation = null,
            destinationText = "Destino",
            destinationLocation = destination
        )

        assertEquals(HomeDirectionStatus.MISSING_HOME_LOCATION, result.status)
        assertEquals(0, provider.callCount)
    }

    private class FakeDriverLocationProvider(
        private val result: DriverLocationResult
    ) : DriverLocationProvider {
        var callCount = 0
            private set

        override suspend fun getLastKnownLocation(): DriverLocationResult {
            callCount += 1
            return result
        }
    }
}
