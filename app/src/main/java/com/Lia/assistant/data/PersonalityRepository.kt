package com.Lia.assistant.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.Lia.assistant.voice.VoiceSessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "assistant_settings")

object PersonalityRepository {
    private val KEY_PERSONALITY = stringPreferencesKey("personality")
    private val KEY_LANGUAGE = stringPreferencesKey("language")
    private val KEY_ASSISTANT_NAME = stringPreferencesKey("assistant_name")
    private val KEY_VOICE_NAME = stringPreferencesKey("voice_name")

    fun getPersonalityFlow(context: Context): Flow<Personality> {
        return context.dataStore.data.map { prefs ->
            val name = prefs[KEY_PERSONALITY]
                ?: NovaPreferences.getString(context, NovaPreferences.Keys.PERSONALITY, Personality.NORMAL.name)
            Personality.fromName(name)
        }
    }

    suspend fun getPersonality(context: Context): Personality {
        val prefs = context.dataStore.data.first()
        val name = prefs[KEY_PERSONALITY]
            ?: NovaPreferences.getString(context, NovaPreferences.Keys.PERSONALITY, Personality.NORMAL.name)
        return Personality.fromName(name)
    }

    suspend fun setPersonality(context: Context, personality: Personality) {
        NovaPreferences.putString(context, NovaPreferences.Keys.PERSONALITY, personality.name)
        context.dataStore.edit { it[KEY_PERSONALITY] = personality.name }
        VoiceSessionManager.refreshInstructions(context)
    }

    fun getLanguageFlow(context: Context): Flow<LanguagePreference> {
        return context.dataStore.data.map { prefs ->
            val name = prefs[KEY_LANGUAGE]
                ?: NovaPreferences.getString(context, NovaPreferences.Keys.LANGUAGE, LanguagePreference.AUTO.name)
            LanguagePreference.fromName(name)
        }
    }

    suspend fun getLanguage(context: Context): LanguagePreference {
        val prefs = context.dataStore.data.first()
        val name = prefs[KEY_LANGUAGE]
            ?: NovaPreferences.getString(context, NovaPreferences.Keys.LANGUAGE, LanguagePreference.AUTO.name)
        return LanguagePreference.fromName(name)
    }

    suspend fun setLanguage(context: Context, language: LanguagePreference) {
        NovaPreferences.putString(context, NovaPreferences.Keys.LANGUAGE, language.name)
        context.dataStore.edit { it[KEY_LANGUAGE] = language.name }
        VoiceSessionManager.refreshInstructions(context)
    }

    fun getAssistantNameFlow(context: Context): Flow<String> {
        return context.dataStore.data.map { prefs ->
            val stored = prefs[KEY_ASSISTANT_NAME]
                ?: NovaPreferences.getString(context, NovaPreferences.Keys.ASSISTANT_NAME, AssistantBrand.NAME)
            stored.ifBlank { AssistantBrand.NAME }
        }
    }

    suspend fun getAssistantName(context: Context): String {
        val prefs = context.dataStore.data.first()
        val stored = prefs[KEY_ASSISTANT_NAME]
            ?: NovaPreferences.getString(context, NovaPreferences.Keys.ASSISTANT_NAME, AssistantBrand.NAME)
        return stored.ifBlank { AssistantBrand.NAME }
    }

    suspend fun setAssistantName(context: Context, name: String) {
        val clean = name.trim().ifBlank { AssistantBrand.NAME }
        NovaPreferences.putString(context, NovaPreferences.Keys.ASSISTANT_NAME, clean)
        context.dataStore.edit { it[KEY_ASSISTANT_NAME] = clean }
        VoiceSessionManager.refreshInstructions(context)
    }

    fun getVoiceNameFlow(context: Context): Flow<String> {
        return context.dataStore.data.map { prefs ->
            val voice = prefs[KEY_VOICE_NAME]
                ?: NovaPreferences.getString(context, NovaPreferences.Keys.VOICE_NAME, "Aoede")
            voice.ifBlank { "Aoede" }
        }
    }

    suspend fun getVoiceName(context: Context): String {
        val prefs = context.dataStore.data.first()
        val voice = prefs[KEY_VOICE_NAME]
            ?: NovaPreferences.getString(context, NovaPreferences.Keys.VOICE_NAME, "Aoede")
        return voice.ifBlank { "Aoede" }
    }

    suspend fun setVoiceName(context: Context, voiceName: String) {
        val clean = voiceName.trim().ifBlank { "Aoede" }
        NovaPreferences.putString(context, NovaPreferences.Keys.VOICE_NAME, clean)
        context.dataStore.edit { it[KEY_VOICE_NAME] = clean }
        VoiceSessionManager.refreshInstructions(context)
    }
}
