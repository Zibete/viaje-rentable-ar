package com.zibete.driverassistant.geocoding

import java.text.Normalizer
import java.util.Locale

class GeocodingTextNormalizer {
    fun normalizeQuery(rawText: String?, bias: GeocodingBias = GeocodingBias.ARGENTINA): String? {
        val normalized = normalizeKey(rawText)
        if (normalized.isNullOrBlank()) return null

        return if (normalized.contains(bias.countryHint, ignoreCase = true)) {
            normalized
        } else {
            "$normalized, ${bias.countryHint}"
        }
    }

    fun normalizeKey(rawText: String?): String? {
        val normalized = rawText
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.trim(',', '.', ';', ':', '-', ' ')
            ?.takeIf { it.length >= MIN_QUERY_LENGTH }
            ?: return null

        val key = Normalizer.normalize(normalized.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9 ,.-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return key.takeIf { it.length >= MIN_QUERY_LENGTH }
    }

    companion object {
        private const val MIN_QUERY_LENGTH = 3
    }
}
