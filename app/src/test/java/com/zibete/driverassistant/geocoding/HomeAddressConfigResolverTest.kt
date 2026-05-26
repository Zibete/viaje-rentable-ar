package com.zibete.driverassistant.geocoding

import com.zibete.driverassistant.config.DriverConfig
import com.zibete.driverassistant.config.toHomeLocationOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeAddressConfigResolverTest {
    @Test
    fun emptyAddressClearsHomeCoordinates() = runBlocking {
        val delegate = FakeGeocodingRepository(success())
        val resolver = HomeAddressConfigResolver(delegate)

        val resolution = resolver.resolveIfNeeded(
            DriverConfig.default().copy(
                homeAddress = " ",
                homeLatitude = -34.458,
                homeLongitude = -58.914
            )
        )

        assertEquals(HomeAddressConfigResolutionStatus.NO_ADDRESS, resolution.status)
        assertEquals(null, resolution.config.homeAddress)
        assertEquals(null, resolution.config.homeLatitude)
        assertEquals(null, resolution.config.homeLongitude)
        assertEquals(0, delegate.callCount)
    }

    @Test
    fun successfulGeocodingStoresResolvedHomeLocation() = runBlocking {
        val resolver = HomeAddressConfigResolver(
            geocodingRepository = FakeGeocodingRepository(success()),
            clockMillis = { 1_700_000_000_000L }
        )

        val resolution = resolver.resolveIfNeeded(
            DriverConfig.default().copy(
                homeAddress = "Av. Siempre Viva 742, Pilar, Buenos Aires"
            )
        )

        assertEquals(HomeAddressConfigResolutionStatus.RESOLVED, resolution.status)
        assertEquals(-34.458, resolution.config.homeLatitude ?: 0.0, 0.001)
        assertEquals(-58.914, resolution.config.homeLongitude ?: 0.0, 0.001)
        assertEquals("Av. Siempre Viva 742, Pilar", resolution.config.homeAddressLabel)
        assertEquals(1_700_000_000_000L, resolution.config.homeResolvedAtMillis)
        assertEquals("android-geocoder", resolution.config.homeGeocodingProvider)
        assertEquals(-34.458, resolution.config.toHomeLocationOrNull()?.latitude ?: 0.0, 0.001)
    }

    @Test
    fun failedGeocodingLeavesAddressWithoutPartialLocation() = runBlocking {
        val resolver = HomeAddressConfigResolver(
            geocodingRepository = FakeGeocodingRepository(GeocodingResult.notFound())
        )

        val resolution = resolver.resolveIfNeeded(
            DriverConfig.default().copy(
                homeAddress = "Direccion no resuelta"
            )
        )

        assertEquals(HomeAddressConfigResolutionStatus.NOT_RESOLVED, resolution.status)
        assertEquals("Direccion no resuelta", resolution.config.homeAddress)
        assertEquals(null, resolution.config.homeLatitude)
        assertEquals(null, resolution.config.homeLongitude)
        assertEquals(null, resolution.config.toHomeLocationOrNull())
    }

    @Test
    fun lowConfidenceHomeResultLeavesAddressWithoutCoordinates() = runBlocking {
        val resolver = HomeAddressConfigResolver(
            geocodingRepository = FakeGeocodingRepository(success(confidence = 0.4))
        )

        val resolution = resolver.resolveIfNeeded(
            DriverConfig.default().copy(homeAddress = "Pilar")
        )

        assertEquals(HomeAddressConfigResolutionStatus.NOT_RESOLVED, resolution.status)
        assertEquals("Pilar", resolution.config.homeAddress)
        assertEquals(null, resolution.config.homeLatitude)
        assertEquals(null, resolution.config.homeLongitude)
    }

    @Test
    fun alreadyResolvedHomeDoesNotGeocodeAgain() = runBlocking {
        val delegate = FakeGeocodingRepository(success())
        val resolver = HomeAddressConfigResolver(delegate)

        val resolution = resolver.resolveIfNeeded(
            DriverConfig.default().copy(
                homeAddress = "Av. Siempre Viva 742",
                homeLatitude = -34.458,
                homeLongitude = -58.914
            )
        )

        assertEquals(HomeAddressConfigResolutionStatus.ALREADY_RESOLVED, resolution.status)
        assertEquals(0, delegate.callCount)
    }

    private fun success(confidence: Double = 0.9): GeocodingResult {
        return GeocodingResult(
            latitude = -34.458,
            longitude = -58.914,
            formattedAddress = "Av. Siempre Viva 742, Pilar",
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
}
