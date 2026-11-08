package com.Lia.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

object JarvisSpeaker : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingSpeech: String? = null
    private var pendingLocale: Locale? = null

    fun init(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.setPitch(0.95f)
            tts?.setSpeechRate(1.0f)
            pendingSpeech?.let { text ->
                speak(text, pendingLocale ?: Locale.US)
                pendingSpeech = null
                pendingLocale = null
            }
        }
    }

    fun speak(text: String, locale: Locale = Locale.US, onStart: (() -> Unit)? = null, onDone: (() -> Unit)? = null) {
        if (!isInitialized || tts == null) {
            pendingSpeech = text
            pendingLocale = locale
            return
        }

        try {
            tts?.language = locale
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onStart?.invoke()
                }

                override fun onDone(utteranceId: String?) {
                    onDone?.invoke()
                }

                override fun onError(utteranceId: String?) {
                    onDone?.invoke()
                }
            })
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_speech_${System.currentTimeMillis()}")
        } catch (_: Exception) {}
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }
}
