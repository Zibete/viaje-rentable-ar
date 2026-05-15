package com.zibete.driverassistant.ocr

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
        return normalized.contains("emparejar") || normalized.contains("aceptar")
    }

    private fun String.normalizeForMarker(): String {
        return lowercase()
            .replace('á', 'a')
            .replace('é', 'e')
            .replace('í', 'i')
            .replace('ó', 'o')
            .replace('ú', 'u')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private companion object {
        private const val BOTTOM_SECTION_RATIO = 0.55f
    }
}
