package com.zibete.driverassistant.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TripOfferTextParserTest {
    private val parser = TripOfferTextParser()

    @Test
    fun parsesArsAmountMinutesAndDecimalKm() {
        val result = parser.parse("ARS 5.127 41 min 8.0 km")

        assertNotNull(result)
        assertEquals(5127.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(41.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(8.0, result?.tripKm ?: 0.0, 0.001)
    }

    @Test
    fun parsesDollarAmountMinutesAndIntegerKm() {
        val result = parser.parse("${'$'} 5127 41 min 8 km")

        assertNotNull(result)
        assertEquals(5127.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(41.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(8.0, result?.tripKm ?: 0.0, 0.001)
    }

    @Test
    fun parsesPesosAmountMinutesAndCommaKm() {
        val result = parser.parse("5.127 pesos 41 minutos 8,0 km")

        assertNotNull(result)
        assertEquals(5127.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(41.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(8.0, result?.tripKm ?: 0.0, 0.001)
    }

    @Test
    fun ignoresProfitabilityMetricsWhenParsingTripDistance() {
        val result = parser.parse("Aceptar ${'$'} 5.127 7.503/h 641/km 41 min 8.0 km total")

        assertNotNull(result)
        assertEquals(5127.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(41.0, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(8.0, result?.tripKm ?: 0.0, 0.001)
    }

    @Test
    fun parsesMinuteRangeAsAverage() {
        val result = parser.parse("1-2 min ${'$'} 5.127 8 km")

        assertNotNull(result)
        assertEquals(5127.0, result?.fareAmount ?: 0.0, 0.001)
        assertEquals(1.5, result?.tripMinutes ?: 0.0, 0.001)
        assertEquals(8.0, result?.tripKm ?: 0.0, 0.001)
    }
}

