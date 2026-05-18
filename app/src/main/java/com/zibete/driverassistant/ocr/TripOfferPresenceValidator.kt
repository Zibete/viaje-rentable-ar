package com.zibete.driverassistant.ocr

import java.text.Normalizer
import java.util.Locale

data class TripOfferPresenceResult(
    val isLikelyOffer: Boolean,
    val score: Double,
    val reasons: List<String>,
    val hasActionMarker: Boolean
)

class TripOfferPresenceValidator {
    fun hasActiveOffer(rawText: String?): Boolean {
        return evaluate(rawText).isLikelyOffer
    }

    fun evaluate(rawText: String?): TripOfferPresenceResult {
        if (rawText.isNullOrBlank()) {
            return TripOfferPresenceResult(
                isLikelyOffer = false,
                score = 0.0,
                reasons = listOf("blank text"),
                hasActionMarker = false
            )
        }

        val lines = rawText.lineSequence()
            .map { line -> line.trim() }
            .filter { line -> line.isNotBlank() }
            .toList()
        if (lines.isEmpty()) {
            return TripOfferPresenceResult(
                isLikelyOffer = false,
                score = 0.0,
                reasons = listOf("no text lines"),
                hasActionMarker = false
            )
        }

        val bottomStartIndex = (lines.size * BOTTOM_SECTION_RATIO).toInt()
            .coerceIn(0, lines.lastIndex)
        val hasActionMarker = lines.withIndex().any { (index, line) ->
            index >= bottomStartIndex && line.hasOfferActionMarker()
        }
        if (hasActionMarker) {
            return TripOfferPresenceResult(
                isLikelyOffer = true,
                score = 1.0,
                reasons = listOf("action marker"),
                hasActionMarker = true
            )
        }

        val normalizedText = rawText.normalizeForEvidence()
        if (normalizedText.looksLikeOwnOverlay()) {
            return TripOfferPresenceResult(
                isLikelyOffer = false,
                score = 0.0,
                reasons = listOf("own overlay text"),
                hasActionMarker = false
            )
        }

        val reasons = mutableListOf<String>()
        var score = 0.0
        val hasFare = fareRegex.containsMatchIn(rawText)
        val hasKm = kmRegex.containsMatchIn(rawText)
        val hasMinutes = minuteRegex.containsMatchIn(rawText)
        val hasTripStructure = pickupStructureRegex.containsMatchIn(normalizedText) ||
            tripStructureRegex.containsMatchIn(normalizedText)
        val hasTripWord = tripWordRegex.containsMatchIn(normalizedText)
        val hasPlatform = platformRegex.containsMatchIn(normalizedText)

        if (hasFare) {
            score += 0.35
            reasons += "fare"
        }
        if (hasKm) {
            score += 0.15
            reasons += "km"
        }
        if (hasMinutes) {
            score += 0.15
            reasons += "minutes"
        }
        if (hasTripStructure) {
            score += 0.25
            reasons += "trip structure"
        } else if (hasTripWord) {
            score += 0.15
            reasons += "trip word"
        }
        if (hasPlatform) {
            score += 0.10
            reasons += "platform"
        }

        val hasStrongEvidence = hasFare &&
            hasKm &&
            hasMinutes &&
            (hasTripStructure || hasTripWord) &&
            score >= CONSERVATIVE_SCORE_THRESHOLD

        return TripOfferPresenceResult(
            isLikelyOffer = hasStrongEvidence,
            score = score.coerceAtMost(1.0),
            reasons = if (reasons.isEmpty()) listOf("insufficient evidence") else reasons,
            hasActionMarker = false
        )
    }

    private fun String.hasOfferActionMarker(): Boolean {
        val normalized = normalizeForMarker()
        val words = normalized.split(" ").filter { it.isNotBlank() }
        val compact = normalized.filter { it in 'a'..'z' }

        return "aceptar" in words ||
            compact in EMPAREJAR_MARKER_VARIANTS
    }

    private fun String.normalizeForEvidence(): String {
        val withoutAccents = Normalizer.normalize(lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")

        return withoutAccents
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun String.looksLikeOwnOverlay(): Boolean {
        val hasOwnDecisionTitle = contains("datos incompletos") ||
            contains("aceptar") ||
            contains("rechazar") ||
            contains("revisar")
        val hasOwnMetricLine = contains("/h") && contains("/km")
        return hasOwnDecisionTitle && hasOwnMetricLine
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
        private const val CONSERVATIVE_SCORE_THRESHOLD = 0.75
        val fareRegex = Regex(
            pattern = """(?i)(?:ARS\s*|\$\s*)\d{1,6}(?:[.,]\d{1,3})*|\b\d{1,6}(?:[.,]\d{1,3})*\s*(?:ARS|pesos)\b"""
        )
        val kmRegex = Regex(pattern = """(?i)\b\d+(?:[.,]\d+)?\s*km\b""")
        val minuteRegex = Regex(pattern = """(?i)\b\d+(?:[.,]\d+)?\s*min(?:utos)?\b""")
        val pickupStructureRegex = Regex(
            pattern = """(?i)(?:^|\s)a\s*\d+(?:[.,]\d+)?\s*min(?:utos)?\s*(?:\(\s*)?\d+(?:[.,]\d+)?\s*km"""
        )
        val tripStructureRegex = Regex(
            pattern = """(?i)\bviaje(?:\s+de)?\s+\d+(?:[.,]\d+)?\s*min(?:utos)?\s*(?:\(\s*)?\d+(?:[.,]\d+)?\s*km"""
        )
        val tripWordRegex = Regex(pattern = """(?i)\bviaje\b""")
        val platformRegex = Regex(pattern = """(?i)\b(?:uber|didi|cabify)\b""")
        val EMPAREJAR_MARKER_VARIANTS = setOf(
            "emparejar",
            "empajar",
            "empareiar",
            "enparejar",
            "ejar"
        )
    }
}
