package com.Lia.assistant.voice

import android.util.Base64
import com.Lia.assistant.flavor.LiaToolCatalog
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GeminiLiveClient(
    private val apiKey: String,
    private val listener: Listener
) {
    interface Listener {
        fun onSetupComplete()
        fun onAudioChunk(bytes: ByteArray)
        fun onInputTranscript(text: String)
        fun onOutputTranscript(text: String)
        fun onToolCall(id: String, name: String, args: JSONObject)
        fun onTurnComplete()
        fun onInterrupted()
        fun onError(message: String)
        fun onClosed(code: Int, reason: String)
    }

    companion object {
        const val MODEL = "models/gemini-2.5-flash"
        const val WS_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
    }

    private val deduplicator = ToolCallDeduplicator()
    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(20, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    fun connect(systemInstruction: String, voiceName: String = "Aoede") {
        deduplicator.clear()
        val url = "$WS_URL?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected.set(true)
                sendSetup(systemInstruction, voiceName)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleIncomingMessage(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected.set(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected.set(false)
                listener.onClosed(code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected.set(false)
                listener.onError(t.message ?: "WebSocket connection failed")
            }
        })
    }

    private fun sendSetup(systemInstruction: String, voiceName: String) {
        val root = JSONObject()
        val setup = JSONObject()
        setup.put("model", MODEL)

        val generationConfig = JSONObject()
        val modalities = JSONArray()
        modalities.put("AUDIO")
        generationConfig.put("responseModalities", modalities)

        val speechConfig = JSONObject()
        val voiceConfig = JSONObject()
        val prebuiltVoiceConfig = JSONObject()
        prebuiltVoiceConfig.put("voiceName", voiceName)
        voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig)
        speechConfig.put("voiceConfig", voiceConfig)
        generationConfig.put("speechConfig", speechConfig)

        setup.put("generationConfig", generationConfig)

        val sysInstrObj = JSONObject()
        val partsArray = JSONArray()
        val partText = JSONObject()
        partText.put("text", systemInstruction)
        partsArray.put(partText)
        sysInstrObj.put("parts", partsArray)
        setup.put("systemInstruction", sysInstrObj)

        setup.put("inputAudioTranscription", JSONObject())
        setup.put("outputAudioTranscription", JSONObject())

        val toolArray = JSONArray()
        val toolObj = JSONObject()
        toolObj.put("functionDeclarations", LiaToolCatalog.declarations())
        toolArray.put(toolObj)
        setup.put("tools", toolArray)

        root.put("setup", setup)
        webSocket?.send(root.toString())
    }

    fun sendAudio(pcmBytes: ByteArray) {
        if (!isConnected.get() || webSocket == null) return
        val base64 = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)

        val root = JSONObject()
        val realtimeInput = JSONObject()
        val mediaChunks = JSONArray()
        val chunk = JSONObject()
        chunk.put("mimeType", "audio/pcm;rate=16000")
        chunk.put("data", base64)
        mediaChunks.put(chunk)
        realtimeInput.put("mediaChunks", mediaChunks)

        // Also include audio object for maximum compatibility
        val audioObj = JSONObject()
        audioObj.put("mimeType", "audio/pcm;rate=16000")
        audioObj.put("data", base64)
        realtimeInput.put("audio", audioObj)

        root.put("realtimeInput", realtimeInput)
        webSocket?.send(root.toString())
    }

    fun sendToolResponse(id: String, name: String, resultJson: JSONObject) {
        if (!isConnected.get() || webSocket == null) return
        val root = JSONObject()
        val toolResponse = JSONObject()
        val functionResponses = JSONArray()
        val funcResp = JSONObject()
        funcResp.put("id", id)
        funcResp.put("name", name)
        funcResp.put("response", resultJson)
        functionResponses.put(funcResp)
        toolResponse.put("functionResponses", functionResponses)
        root.put("toolResponse", toolResponse)

        webSocket?.send(root.toString())
    }

    fun disconnect() {
        isConnected.set(false)
        try {
            webSocket?.close(1000, "Normal closure")
        } catch (_: Exception) {}
        webSocket = null
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)

            if (json.has("setupComplete")) {
                listener.onSetupComplete()
                return
            }

            if (json.has("toolCall")) {
                val toolCall = json.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls")
                if (functionCalls != null) {
                    for (i in 0 until functionCalls.length()) {
                        val call = functionCalls.getJSONObject(i)
                        val id = call.optString("id", "")
                        val name = call.optString("name", "")
                        val args = call.optJSONObject("args") ?: JSONObject()

                        if (!deduplicator.isDuplicateOrRecord(id)) {
                            listener.onToolCall(id, name, args)
                        }
                    }
                }
                return
            }

            if (json.has("serverContent")) {
                val serverContent = json.getJSONObject("serverContent")

                if (serverContent.optBoolean("interrupted", false)) {
                    listener.onInterrupted()
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val dataBase64 = inlineData.optString("data", "")
                                if (dataBase64.isNotEmpty()) {
                                    val audioBytes = Base64.decode(dataBase64, Base64.DEFAULT)
                                    listener.onAudioChunk(audioBytes)
                                }
                            }
                        }
                    }
                }

                if (serverContent.has("inputTranscription")) {
                    val inputTrans = serverContent.getJSONObject("inputTranscription")
                    val textTrans = inputTrans.optString("text", "")
                    if (textTrans.isNotEmpty()) {
                        listener.onInputTranscript(textTrans)
                    }
                }

                if (serverContent.has("outputTranscription")) {
                    val outTrans = serverContent.getJSONObject("outputTranscription")
                    val textTrans = outTrans.optString("text", "")
                    if (textTrans.isNotEmpty()) {
                        listener.onOutputTranscript(textTrans)
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    listener.onTurnComplete()
                }
            }
        } catch (_: Exception) {
            // Silently ignore malformed frames or heartbeat frames
        }
    }
}
