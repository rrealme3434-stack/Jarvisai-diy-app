package com.Lia.assistant.data

import android.content.Context
import android.content.SharedPreferences

object ApiKeyStore {
    private const val PREFS_NAME = "nova_prefs"
    private const val KEY_GEMINI = "gemini_api_key"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getKey(context: Context): String? {
        val key = prefs(context).getString(KEY_GEMINI, null)?.trim()
        return if (!key.isNullOrEmpty()) key else null
    }

    fun saveKey(context: Context, key: String) {
        val trimmed = key.trim()
        prefs(context).edit().putString(KEY_GEMINI, trimmed).apply()
    }

    fun hasKey(context: Context): Boolean {
        return !getKey(context).isNullOrEmpty()
    }

    fun clearKey(context: Context) {
        prefs(context).edit().remove(KEY_GEMINI).apply()
    }
}
