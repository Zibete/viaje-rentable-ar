package com.zibete.driverassistant.ocr

import java.text.Normalizer
import java.util.Locale

class TripOfferPresenceValidator {
    fun hasActiveOffer(rawText: String?): Boolean {
        if (rawText.isNullOrBlank()) return false

        val lines = rawText.lineSequence()
            .map { line -> line.trim() }
            .filter { line -> line.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return false

        val bottomStartIndex = (lines.size * BOTTOM_SECTION_RATIO).toInt()
            .coerceIn(0, lines.lastIndex)

        return lines.withIndex().any { (index, line) ->
            index >= bottomStartIndex && line.hasOfferActionMarker()
        }
    }

    private fun String.hasOfferActionMarker(): Boolean {
        val normalized = normalizeForMarker()
        val words = normalized.split(" ").filter { it.isNotBlank() }
        val compact = normalized.filter { it in 'a'..'z' }

        return "aceptar" in words ||
            compact in EMPAREJAR_MARKER_VARIANTS
    }

    private fun String.normalizeForMarker(): String {
        val withoutAccents = Normalizer.normalize(lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")

        return withoutAccents
            .replace('0', 'o')
            .replace('1', 'l')
            .replace(Regex("[^a-z]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private companion object {
        private const val BOTTOM_SECTION_RATIO = 0.55f
        val EMPAREJAR_MARKER_VARIANTS = setOf(
            "emparejar",
            "empajar",
            "empareiar",
            "enparejar",
            "ejar"
        )
    }
}
