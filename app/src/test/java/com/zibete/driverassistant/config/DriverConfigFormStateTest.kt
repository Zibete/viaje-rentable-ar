package com.zibete.driverassistant.config

import com.zibete.driverassistant.zones.AvoidZonePolicy
import com.zibete.driverassistant.zones.AvoidZoneRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverConfigFormStateTest {
    @Test
    fun acceptsCommaDecimal() {
        val result = baseForm.copy(minArsPerKm = "600,5")
            .toDriverConfig(baseConfig)

        val config = (result as DriverConfigFormValidationResult.Valid).config
        assertEquals(600.5, config.minArsPerKm, 0.001)
    }

    @Test
    fun acceptsDotDecimal() {
        val result = baseForm.copy(minArsPerKm = "600.5")
            .toDriverConfig(baseConfig)

        val config = (result as DriverConfigFormValidationResult.Valid).config
        assertEquals(600.5, config.minArsPerKm, 0.001)
    }

    @Test
    fun rejectsBlankValue() {
        val result = baseForm.copy(minArsPerKm = "")
            .toDriverConfig(baseConfig)

        assertTrue(result is DriverConfigFormValidationResult.Invalid)
    }

    @Test
    fun rejectsNegativeValue() {
        val result = baseForm.copy(minArsPerKm = "-1")
            .toDriverConfig(baseConfig)

        assertTrue(result is DriverConfigFormValidationResult.Invalid)
    }

    @Test
    fun convertsValidFormToDriverConfig() {
        val result = DriverConfigFormState(
            minArsPerKm = "650",
            minArsPerHour = "7500",
            minNetProfit = "1200",
            costPerKm = "300",
            costPerMinute = "35",
            reviewTolerancePercent = "8"
        ).toDriverConfig(baseConfig)

        val config = (result as DriverConfigFormValidationResult.Valid).config
        assertEquals(650.0, config.minArsPerKm, 0.001)
        assertEquals(7500.0, config.minArsPerHour, 0.001)
        assertEquals(1200.0, config.minNetProfit, 0.001)
        assertEquals(300.0, config.costPerKm, 0.001)
        assertEquals(35.0, config.costPerMinute, 0.001)
        assertEquals(8.0, config.reviewTolerancePercent, 0.001)
    }

    @Test
    fun preservesAvoidZonesWhenSaving() {
        val result = baseForm.toDriverConfig(baseConfig)

        val config = (result as DriverConfigFormValidationResult.Valid).config
        assertEquals(baseConfig.avoidZones, config.avoidZones)
    }

    @Test
    fun emptyHomeAddressClearsCoordinates() {
        val currentConfig = baseConfig.copy(
            homeAddress = "Av. Siempre Viva 742",
            homeLatitude = -34.458,
            homeLongitude = -58.914,
            homeAddressLabel = "Av. Siempre Viva 742",
            homeResolvedAtMillis = 1_700_000_000_000L,
            homeGeocodingProvider = "test"
        )

        val result = DriverConfigFormState.fromConfig(currentConfig)
            .copy(homeAddress = " ")
            .toDriverConfig(currentConfig)

        val config = (result as DriverConfigFormValidationResult.Valid).config
        assertEquals(null, config.homeAddress)
        assertEquals(null, config.homeLatitude)
        assertEquals(null, config.homeLongitude)
        assertEquals(null, config.toHomeLocationOrNull())
    }

    @Test
    fun keepsResolvedHomeCoordinatesWhenAddressIsUnchanged() {
        val currentConfig = baseConfig.copy(
            homeAddress = "Av. Siempre Viva 742",
            homeLatitude = -34.458,
            homeLongitude = -58.914,
            homeAddressLabel = "Av. Siempre Viva 742",
            homeResolvedAtMillis = 1_700_000_000_000L,
            homeGeocodingProvider = "test"
        )

        val result = DriverConfigFormState.fromConfig(currentConfig)
            .copy(homeAddress = "  Av. Siempre Viva 742  ")
            .toDriverConfig(currentConfig)

        val config = (result as DriverConfigFormValidationResult.Valid).config
        assertEquals("Av. Siempre Viva 742", config.homeAddress)
        assertEquals(-34.458, config.homeLatitude ?: 0.0, 0.001)
        assertEquals(-58.914, config.homeLongitude ?: 0.0, 0.001)
    }

    @Test
    fun changingHomeAddressInvalidatesPreviousCoordinates() {
        val currentConfig = baseConfig.copy(
            homeAddress = "Av. Siempre Viva 742",
            homeLatitude = -34.458,
            homeLongitude = -58.914,
            homeAddressLabel = "Av. Siempre Viva 742",
            homeResolvedAtMillis = 1_700_000_000_000L,
            homeGeocodingProvider = "test"
        )

        val result = DriverConfigFormState.fromConfig(currentConfig)
            .copy(homeAddress = "Calle Nueva 123, Pilar")
            .toDriverConfig(currentConfig)

        val config = (result as DriverConfigFormValidationResult.Valid).config
        assertEquals("Calle Nueva 123, Pilar", config.homeAddress)
        assertEquals(null, config.homeLatitude)
        assertEquals(null, config.homeLongitude)
        assertEquals(null, config.homeAddressLabel)
        assertEquals(null, config.homeResolvedAtMillis)
        assertEquals(null, config.homeGeocodingProvider)
    }

    private val baseConfig = DriverConfig.default().copy(
        avoidZones = listOf(
            AvoidZoneRule(
                id = "zona-1",
                name = "Zona 1",
                keywords = listOf("zona"),
                policy = AvoidZonePolicy.REVIEW
            )
        )
    )

    private val baseForm = DriverConfigFormState.fromConfig(baseConfig)
}
