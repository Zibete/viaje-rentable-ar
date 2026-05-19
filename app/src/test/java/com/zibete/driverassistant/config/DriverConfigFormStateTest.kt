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
