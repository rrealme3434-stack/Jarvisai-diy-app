package com.Lia.assistant.voice

enum class DetectedLanguage(val label: String) {
    TELUGU("Telugu"),
    HINDI("Hindi"),
    HINGLISH("Hinglish"),
    ENGLISH("English"),
    UNKNOWN("Unknown")
}

object LanguageDetector {
    private val HINGLISH_MARKERS = hashSetOf(
        "hai", "hain", "ho", "kya", "kyun", "kaise", "nahi", "haan", "mujhe",
        "tumhe", "aap", "mera", "yaar", "bhai", "accha", "theek", "matlab",
        "karna", "karo", "raha", "chahiye", "batao", "suno", "dekho", "abhi",
        "phir", "lekin", "bahut", "thoda", "kuch", "sab", "wala", "meri",
        "mere", "tere", "tera", "hum", "tum", "kar", "liya", "diya",
        "hoga", "gaya", "gayi", "rahi", "bolo", "bhi", "toh", "kaun", "kahan"
    )

    private val TELUGU_LATIN_MARKERS = hashSetOf(
        "enti", "ela", "unnava", "unnaru", "cheppu", "kavali", "ledu", "avunu",
        "chudu", "pettuko", "chesuko", "vastara", "ippudu", "repu", "nenu",
        "nuvvu", "meeru", "baga", "chala", "anduke", "emi", "ekkada", "enduku"
    )

    fun detect(text: String): DetectedLanguage {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return DetectedLanguage.UNKNOWN

        // Check for Telugu script (U+0C00 to U+0C7F)
        for (ch in trimmed) {
            if (ch in '\u0C00'..'\u0C7F') {
                return DetectedLanguage.TELUGU
            }
        }

        // Check for Devanagari script (U+0900 to U+097F)
        for (ch in trimmed) {
            if (ch in '\u0900'..'\u097F') {
                return DetectedLanguage.HINDI
            }
        }

        val words = trimmed.lowercase()
            .split(Regex("[^a-zA-Z']+"))
            .filter { it.isNotEmpty() }

        if (words.isEmpty()) return DetectedLanguage.UNKNOWN

        var teluguHits = 0
        var hinglishHits = 0
        for (w in words) {
            if (TELUGU_LATIN_MARKERS.contains(w)) teluguHits++
            if (HINGLISH_MARKERS.contains(w)) hinglishHits++
        }

        if (teluguHits >= 1) return DetectedLanguage.TELUGU

        val wordCount = words.size
        return if (wordCount <= 4) {
            if (hinglishHits >= 1) DetectedLanguage.HINGLISH else DetectedLanguage.ENGLISH
        } else {
            val ratio = hinglishHits.toDouble() / wordCount.toDouble()
            if (hinglishHits >= 2 || ratio >= 0.15) {
                DetectedLanguage.HINGLISH
            } else {
                DetectedLanguage.ENGLISH
            }
        }
    }
}
