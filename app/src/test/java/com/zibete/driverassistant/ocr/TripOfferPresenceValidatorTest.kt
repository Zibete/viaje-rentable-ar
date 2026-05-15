package com.zibete.driverassistant.ocr

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripOfferPresenceValidatorTest {
    private val validator = TripOfferPresenceValidator()

    @Test
    fun acceptsEmparejarNearBottom() {
        val rawText = """
            UberX
            7124 ARS
            A 6 min (2.5 km) de distancia
            Viaje de 24 min (12.2 km)
            Florida, Jose C. Paz
            Emparejar
        """.trimIndent()

        assertTrue(validator.hasActiveOffer(rawText))
    }

    @Test
    fun acceptsAceptarNearBottom() {
        val rawText = """
            UberX
            5472 ARS
            A 3 min (1.3 km) de distancia
            Viaje de 26 min (11.9 km)
            Aceptar
        """.trimIndent()

        assertTrue(validator.hasActiveOffer(rawText))
    }

    @Test
    fun acceptsEmparejarOcrVariantsNearBottom() {
        listOf(
            "Ejar",
            "Empajar",
            "Emparej ar",
            "Empareiar",
            "Enparejar"
        ).forEach { actionMarker ->
            val rawText = """
                2 Comfort Exclusivo
                8780 ARS
                A6 min (2.5 km) de distancia
                Viaje de 26 min (12.2 km)
                $actionMarker
            """.trimIndent()

            assertTrue(actionMarker, validator.hasActiveOffer(rawText))
        }
    }

    @Test
    fun rejectsScreenWithoutActionMarker() {
        val rawText = """
            UberX
            5472 ARS
            A 3 min (1.3 km) de distancia
            Viaje de 26 min (11.9 km)
        """.trimIndent()

        assertFalse(validator.hasActiveOffer(rawText))
    }

    @Test
    fun rejectsGenericBottomWordsThatAreNotActionMarkers() {
        val rawText = """
            2 Comfort Exclusivo
            8780 ARS
            A6 min (2.5 km) de distancia
            Viaje de 26 min (12.2 km)
            Ver detalles
        """.trimIndent()

        assertFalse(validator.hasActiveOffer(rawText))
    }

    @Test
    fun rejectsAcceptTextOutsideBottomSection() {
        val rawText = """
            ACEPTAR
            Overlay anterior
            UberX
            5472 ARS
            A 3 min (1.3 km) de distancia
            Viaje de 26 min (11.9 km)
            Florida, Jose C. Paz
            X
        """.trimIndent()

        assertFalse(validator.hasActiveOffer(rawText))
    }
}
