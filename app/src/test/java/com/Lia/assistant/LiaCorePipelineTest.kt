package com.Lia.assistant

import com.Lia.assistant.data.LanguagePreference
import com.Lia.assistant.data.Personality
import com.Lia.assistant.voice.DetectedLanguage
import com.Lia.assistant.voice.EchoGuard
import com.Lia.assistant.voice.LanguageDetector
import com.Lia.assistant.voice.PersonalityPromptBuilder
import com.Lia.assistant.voice.ToolCallDeduplicator
import com.Lia.assistant.voice.VoiceLatencyTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LiaCorePipelineTest {

    @Before
    fun setUp() {
        EchoGuard.reset()
        VoiceLatencyTracker.resetReconnects()
    }

    @Test
    fun testEveryPersonalityAndLanguageBuilds() {
        for (personality in Personality.entries) {
            for (lang in LanguagePreference.entries) {
                val prompt = PersonalityPromptBuilder.build(
                    personality = personality,
                    language = lang,
                    detectedLanguage = DetectedLanguage.UNKNOWN,
                    recap = null,
                    assistantName = "Jarvis"
                )
                assertTrue("Prompt should not be blank", prompt.isNotBlank())
                assertTrue("Prompt should contain assistant name", prompt.contains("Jarvis"))
                assertTrue("Prompt should contain Boss address", prompt.contains("Boss"))
                assertTrue("Prompt should contain personality name", prompt.contains(personality.displayName, ignoreCase = true))
                assertFalse("Prompt should not contain recap when null", prompt.contains("Recap:"))
            }
        }
    }

    @Test
    fun testLanguageDetectorIncludesTelugu() {
        // Telugu script
        assertEquals(DetectedLanguage.TELUGU, LanguageDetector.detect("మీరు ఎలా ఉన్నారు?"))
        assertEquals(DetectedLanguage.TELUGU, LanguageDetector.detect("నువ్వు ఎవరు జార్విస్?"))

        // Romanized Telugu
        assertEquals(DetectedLanguage.TELUGU, LanguageDetector.detect("enti ela unnavu"))

        // Hindi
        assertEquals(DetectedLanguage.HINDI, LanguageDetector.detect("नमस्ते, आप कैसे हैं?"))

        // Hinglish
        assertEquals(DetectedLanguage.HINGLISH, LanguageDetector.detect("kya haal hai yaar"))

        // English
        assertEquals(DetectedLanguage.ENGLISH, LanguageDetector.detect("Jarvis, what is the battery level?"))

        // Blank
        assertEquals(DetectedLanguage.UNKNOWN, LanguageDetector.detect("   "))
    }

    @Test
    fun testToolCallDeduplicator() {
        val dedup = ToolCallDeduplicator(maxSize = 3)

        assertFalse("First time seeing call-1", dedup.isDuplicateOrRecord("call-1"))
        assertTrue("Duplicate call-1 should be true", dedup.isDuplicateOrRecord("call-1"))

        assertFalse(dedup.isDuplicateOrRecord("call-2"))
        assertFalse(dedup.isDuplicateOrRecord("call-3"))
        assertFalse(dedup.isDuplicateOrRecord("call-4"))

        assertFalse(dedup.isDuplicateOrRecord("call-1"))
    }

    @Test
    fun testEchoGuardMuteCycle() {
        assertFalse(EchoGuard.isMicMuted())

        EchoGuard.notifyPlaybackStarted()
        assertTrue(EchoGuard.isMicMuted())

        EchoGuard.notifyPlaybackStarted()
        assertTrue(EchoGuard.isMicMuted())

        EchoGuard.notifyPlaybackStopped()
        assertTrue(EchoGuard.isMicMuted())

        EchoGuard.forceUnmute()
        assertFalse(EchoGuard.isMicMuted())
    }

    @Test
    fun testVoiceLatencyTracker() {
        assertEquals(0, VoiceLatencyTracker.reconnectCount.value)
        VoiceLatencyTracker.recordReconnect()
        assertEquals(1, VoiceLatencyTracker.reconnectCount.value)

        VoiceLatencyTracker.markUserTurnStarted(1000L)
        VoiceLatencyTracker.markFirstAudioReceived(1350L)
        assertEquals(350L, VoiceLatencyTracker.lastResponseLatencyMs)

        VoiceLatencyTracker.markFirstAudioReceived(900L)
        assertEquals(350L, VoiceLatencyTracker.lastResponseLatencyMs)
    }
}
