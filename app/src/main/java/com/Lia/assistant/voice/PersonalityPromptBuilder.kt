package com.Lia.assistant.voice

import com.Lia.assistant.data.LanguagePreference
import com.Lia.assistant.data.Personality
import com.Lia.assistant.flavor.LiaCapabilityPrompts

object PersonalityPromptBuilder {
    fun build(
        personality: Personality,
        language: LanguagePreference,
        detectedLanguage: DetectedLanguage,
        recap: String?,
        assistantName: String
    ): String {
        val sections = mutableListOf<String>()

        // 1. Base instruction with Boss identity
        sections.add(
            "You are $assistantName, a highly intelligent, dedicated personal AI assistant living on your user's phone. " +
            "Crucial Rule: Always respectfully address the user as 'Boss' (or 'బాస్' in Telugu, 'बॉस' in Hindi) in your conversation. " +
            "Keep your replies articulate, crisp, confident, and natural like a trusted chief-of-staff AI."
        )

        // 2. Flavor-specific tool instructions
        val toolInstruction = LiaCapabilityPrompts.toolInstruction(assistantName)
        if (toolInstruction.isNotBlank()) {
            sections.add(toolInstruction)
        }

        // 3. Personality instruction
        sections.add(personality.systemInstruction)

        // 4. Language policy
        val langPolicy = when (language) {
            LanguagePreference.AUTO -> {
                val currentHint = if (detectedLanguage != DetectedLanguage.UNKNOWN) {
                    " Currently detected language: ${detectedLanguage.label}."
                } else ""
                "Language Policy: Match the language Boss speaks turn by turn. If Boss speaks in Telugu, reply in fluent natural Telugu addressing as బాస్. If Boss speaks Hindi, reply in Hindi. If English, reply in sharp British/American conversational English. Never translate their words back to them.$currentHint"
            }
            LanguagePreference.TELUGU -> "Language Policy: Always respond in natural, respectful Telugu (తెలుగు). Address the user as 'బాస్' (Boss)."
            LanguagePreference.ENGLISH -> "Language Policy: Always respond in sharp, fluent conversational English, consistently addressing the user as 'Boss'."
            LanguagePreference.HINDI -> "Language Policy: Always respond in natural, courteous Hindi (हिंदी), addressing the user as 'बॉस' (Boss)."
            LanguagePreference.HINGLISH -> "Language Policy: Always respond in modern, friendly Hinglish mixed with English, addressing the user as 'Boss'."
            LanguagePreference.ALL_LANGUAGES -> "Language Policy: Adaptive polyglot mode. Understand and converse in any human language Boss speaks while maintaining the Jarvis persona."
        }
        sections.add(langPolicy)

        // 5. Safety and spoken speech guidelines
        sections.add(
            "Safety & Style Guidelines: Never claim to possess a physical biological body. Do not generate sexually explicit content. If Boss asks to change the topic or tone, comply immediately. Keep spoken answers concise, expressive, and impactful."
        )

        // 6. Recap if resuming
        if (!recap.isNullOrBlank()) {
            sections.add(
                "This is a continuing conversation with Boss — do not repeat greetings. Recap:\n$recap"
            )
        }

        return sections.joinToString("\n\n")
    }
}
