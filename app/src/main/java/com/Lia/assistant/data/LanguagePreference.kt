package com.Lia.assistant.data

enum class LanguagePreference(val displayName: String, val description: String) {
    AUTO("Auto Detect", "Follows whatever you speak (English, Telugu, Hindi, Hinglish)"),
    ENGLISH("English", "Fluent conversational English addressing you as Boss"),
    TELUGU("Telugu (తెలుగు)", "సహజమైన తెలుగులో పూర్తి వాయిస్ సహాయం మరియు స్పందనలు"),
    HINDI("Hindi (हिंदी)", "सटीक और सम्मानजनक हिंदी संवाद"),
    HINGLISH("Hinglish", "Conversational blend of Hindi and English"),
    ALL_LANGUAGES("All Languages", "Adaptive polyglot intelligence for any global language");

    companion object {
        fun fromName(name: String?): LanguagePreference {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: AUTO
        }
    }
}
