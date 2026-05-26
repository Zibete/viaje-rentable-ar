package com.zibete.driverassistant.ocr

class DestinationTextNormalizer {
    fun normalize(rawText: String?): String? {
        val cleaned = rawText
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.replace(Regex("[\\u0000-\\u001F]+"), " ")
            ?.trim(' ', '-', ':', ';', ',', '.')
            ?.takeIf { it.length >= MIN_DESTINATION_LENGTH }
            ?: return null

        return cleaned
            .replace(safeSanMiguelRegex, "San Miguel")
            .replace(numberTrailingLetterORegex, "0")
            .replace(Regex("\\s+"), " ")
            .trim()
            .takeIf { it.length >= MIN_DESTINATION_LENGTH }
    }

    private companion object {
        const val MIN_DESTINATION_LENGTH = 4
        val safeSanMiguelRegex = Regex("""(?i)\bSan\s+Migue[I|]\b""")
        val numberTrailingLetterORegex = Regex("""(?<=\d)[Oo](?=\b)""")
    }
}
