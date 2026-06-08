package com.zibete.driverassistant.home

import com.zibete.driverassistant.calculator.TripOfferInput
import com.zibete.driverassistant.config.DriverConfig
import com.zibete.driverassistant.geocoding.GeocodingBias
import com.zibete.driverassistant.geocoding.GeocodingRepository
import com.zibete.driverassistant.geocoding.GeocodingResult
import com.zibete.driverassistant.geocoding.GeocodingStatus
import com.zibete.driverassistant.location.DriverLocation
import com.zibete.driverassistant.location.DriverLocationCache
import com.zibete.driverassistant.location.DriverLocationProvider
import com.zibete.driverassistant.location.DriverLocationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDirectionScoreResolverTest {
    @Test
    fun resolvesAvailableHomeScore() = runBlocking {
        val geocodingRepository = FakeGeocodingRepository(
            result = destination(latitude = 0.0, longitude = 0.05)
        )
        val locationProvider = FakeDriverLocationProvider(
            DriverLocationResult.available(
                DriverLocation(
                    latitude = 0.0,
                    longitude = 0.0,
                    accuracyMeters = 20f,
                    capturedAtMillis = 1_700_000_000_000L
                )
            )
        )

        val result = HomeDirectionScoreResolver(
            geocodingRepository = geocodingRepository,
            driverLocationProvider = locationProvider
        ).resolve(
            config = resolvedHomeConfig(),
            input = input(destinationText = "Destino")
        )

        assertEquals(HomeDirectionStatus.AVAILABLE, result.status)
        assertTrue(requireNotNull(result.score) >= 9)
        assertEquals(1, geocodingRepository.callCount)
        assertEquals(1, locationProvider.callCount)
    }

    @Test
    fun missingHomeReturnsUnknownWithoutGeocodingOrLocation() = runBlocking {
        val geocodingRepository = FakeGeocodingRepository(destination())
        val locationProvider = FakeDriverLocationProvider(DriverLocationResult.unavailable())

        val result = HomeDirectionScoreResolver(
            geocodingRepository = geocodingRepository,
            driverLocationProvider = locationProvider
        ).resolve(
            config = DriverConfig.default(),
            input = input(destinationText = "Destino")
        )

        assertEquals(HomeDirectionStatus.MISSING_HOME_LOCATION, result.status)
        assertEquals("Casa ?", result.shortLabel)
        assertEquals(0, geocodingRepository.callCount)
        assertEquals(0, locationProvider.callCount)
    }

    @Test
    fun missingDestinationTextReturnsUnknownWithoutGeocodingOrLocation() = runBlocking {
        val geocodingRepository = FakeGeocodingRepository(destination())
        val locationProvider = FakeDriverLocationProvider(DriverLocationResult.unavailable())

        val result = HomeDirectionScoreResolver(
            geocodingRepository = geocodingRepository,
            driverLocationProvider = locationProvider
        ).resolve(
            config = resolvedHomeConfig(),
            input = input(destinationText = null)
        )

        assertEquals(HomeDirectionStatus.MISSING_DESTINATION_TEXT, result.status)
        assertEquals(0, geocodingRepository.callCount)
        assertEquals(0, locationProvider.callCount)
    }

    @Test
    fun missingDriverLocationReturnsUnknownAfterDestinationResolution() = runBlocking {
        val geocodingRepository = FakeGeocodingRepository(destination(latitude = 0.0, longitude = 0.05))
        val locationProvider = FakeDriverLocationProvider(DriverLocationResult.unavailable())

        val result = HomeDirectionScoreResolver(
            geocodingRepository = geocodingRepository,
            driverLocationProvider = locationProvider
        ).resolve(
            config = resolvedHomeConfig(),
            input = input(destinationText = "Destino")
        )

        assertEquals(HomeDirectionStatus.MISSING_CURRENT_LOCATION, result.status)
        assertEquals(1, geocodingRepository.callCount)
        assertEquals(1, locationProvider.callCount)
    }

    @Test
    fun freshCachedLocationProducesScoreWithoutFallbackLookup() = runBlocking {
        val geocodingRepository = FakeGeocodingRepository(
            result = destination(latitude = 0.0, longitude = 0.05)
        )
        val locationProvider = FakeDriverLocationProvider(DriverLocationResult.unavailable())
        val locationCache = DriverLocationCache().apply {
            update(
                DriverLocation(
                    latitude = 0.0,
                    longitude = 0.0,
                    accuracyMeters = 15f,
                    capturedAtMillis = 1_700_000_000_000L
                )
            )
        }

        val result = HomeDirectionScoreResolver(
            geocodingRepository = geocodingRepository,
            driverLocationProvider = locationProvider,
            driverLocationCache = locationCache
        ).resolve(
            config = resolvedHomeConfig(),
            input = input(destinationText = "Destino")
        )

        assertEquals(HomeDirectionStatus.AVAILABLE, result.status)
        assertEquals(1, geocodingRepository.callCount)
        assertEquals(0, locationProvider.callCount)
    }

    @Test
    fun missingCachedLocationKeepsFallbackLookup() = runBlocking {
        val geocodingRepository = FakeGeocodingRepository(
            result = destination(latitude = 0.0, longitude = 0.05)
        )
        val locationProvider = FakeDriverLocationProvider(
            DriverLocationResult.available(
                DriverLocation(
                    latitude = 0.0,
                    longitude = 0.0,
                    accuracyMeters = 15f,
                    capturedAtMillis = 1_700_000_000_000L
                )
            )
        )
        val locationCache = DriverLocationCache()

        val result = HomeDirectionScoreResolver(
            geocodingRepository = geocodingRepository,
            driverLocationProvider = locationProvider,
            driverLocationCache = locationCache
        ).resolve(
            config = resolvedHomeConfig(),
            input = input(destinationText = "Destino")
        )

        assertEquals(HomeDirectionStatus.AVAILABLE, result.status)
        assertEquals(1, geocodingRepository.callCount)
        assertEquals(1, locationProvider.callCount)
    }

    @Test
    fun lowConfidenceDestinationReturnsUnknownWithoutLocationLookup() = runBlocking {
        val geocodingRepository = FakeGeocodingRepository(destination(confidence = 0.4))
        val locationProvider = FakeDriverLocationProvider(DriverLocationResult.unavailable())

        val result = HomeDirectionScoreResolver(
            geocodingRepository = geocodingRepository,
            driverLocationProvider = locationProvider
        ).resolve(
            config = resolvedHomeConfig(),
            input = input(destinationText = "Destino ambiguo")
        )

        assertEquals(HomeDirectionStatus.LOW_CONFIDENCE_DESTINATION, result.status)
        assertEquals("Casa ?", result.shortLabel)
        assertEquals(1, geocodingRepository.callCount)
        assertEquals(0, locationProvider.callCount)
    }

    @Test
    fun unresolvedDestinationReturnsUnknown() = runBlocking {
        val geocodingRepository = FakeGeocodingRepository(GeocodingResult.notFound())
        val locationProvider = FakeDriverLocationProvider(DriverLocationResult.unavailable())

        val result = HomeDirectionScoreResolver(
            geocodingRepository = geocodingRepository,
            driverLocationProvider = locationProvider
        ).resolve(
            config = resolvedHomeConfig(),
            input = input(destinationText = "Destino")
        )

        assertEquals(HomeDirectionStatus.DESTINATION_NOT_RESOLVED, result.status)
        assertEquals(1, geocodingRepository.callCount)
        assertEquals(0, locationProvider.callCount)
    }

    private fun resolvedHomeConfig(): DriverConfig {
        return DriverConfig.default().copy(
            homeAddress = "Casa",
            homeLatitude = 0.0,
            homeLongitude = 0.1,
            homeAddressLabel = "Casa",
            homeResolvedAtMillis = 1_700_000_000_000L,
            homeGeocodingProvider = "test"
        )
    }

    private fun input(destinationText: String?): TripOfferInput {
        return TripOfferInput(
            fareAmount = 7500.0,
            pickupKm = 1.0,
            tripKm = 7.0,
            pickupMinutes = 5.0,
            tripMinutes = 35.0,
            platform = "uber",
            destinationText = destinationText,
            rawText = "raw"
        )
    }

    private fun destination(
        latitude: Double = 0.0,
        longitude: Double = 0.05,
        confidence: Double = 0.9
    ): GeocodingResult {
        return GeocodingResult(
            latitude = latitude,
            longitude = longitude,
            formattedAddress = "Destino",
            confidence = confidence,
            status = GeocodingStatus.SUCCESS
        )
    }

    private class FakeGeocodingRepository(
        private val result: GeocodingResult
    ) : GeocodingRepository {
        var callCount = 0
            private set

        override suspend fun geocode(query: String?, bias: GeocodingBias): GeocodingResult {
            callCount += 1
            return result
        }
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
