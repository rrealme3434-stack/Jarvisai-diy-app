package com.Lia.assistant.voice

import android.content.Context
import com.Lia.assistant.action.InstalledAppLabelCache
import com.Lia.assistant.data.ApiKeyStore
import com.Lia.assistant.data.AssistantBrand
import com.Lia.assistant.data.LanguagePreference
import com.Lia.assistant.data.Personality
import com.Lia.assistant.data.PersonalityRepository
import com.Lia.assistant.device.JarvisOfflineBrain
import com.Lia.assistant.flavor.ActionExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.Locale

object VoiceSessionManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionMutex = Mutex()
    private val reconnectMutex = Mutex()

    private var currentEpoch = 0L
    private var client: GeminiLiveClient? = null
    private var audioIO: LiveAudioIO? = null
    private var reconnectJob: Job? = null
    private var playbackSilenceJob: Job? = null
    private var reconnectAttempts = 0

    var onWebsiteForgeRequested: ((prompt: String) -> Unit)? = null

    private val _state = MutableStateFlow(NovaOrbState.IDLE)
    val state: StateFlow<NovaOrbState> = _state.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    private val _statusText = MutableStateFlow("Jarvis Standby • Say 'Hey Jarvis'")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _currentPersonality = MutableStateFlow(Personality.NORMAL)
    val currentPersonality: StateFlow<Personality> = _currentPersonality.asStateFlow()

    private val _currentLanguage = MutableStateFlow(LanguagePreference.AUTO)
    val currentLanguage: StateFlow<LanguagePreference> = _currentLanguage.asStateFlow()

    private val _detectedLanguage = MutableStateFlow(DetectedLanguage.UNKNOWN)
    val detectedLanguage: StateFlow<DetectedLanguage> = _detectedLanguage.asStateFlow()

    private val _vadActive = MutableStateFlow(false)
    val vadActive: StateFlow<Boolean> = _vadActive.asStateFlow()

    private var appContext: Context? = null
    private var isOfflineMode = false

    fun start(context: Context) {
        if (_isActive.value) return
        appContext = context.applicationContext
        JarvisSpeaker.init(context)

        val key = ApiKeyStore.getKey(context)

        _isActive.value = true
        _lastError.value = null
        ConversationMemory.clear()
        reconnectAttempts = 0
        VoiceLatencyTracker.resetReconnects()
        InstalledAppLabelCache.prewarm(context)

        createAudioIO()

        if (key.isNullOrBlank()) {
            // Automatic Offline Fallback: Jarvis is always functional!
            isOfflineMode = true
            _state.value = NovaOrbState.LISTENING
            _statusText.value = "Jarvis Offline Core • Ready, Boss"
            audioIO?.startRecording()
            speakGreeting(context, "All systems nominal, Boss. Offline protocol active.")
            return
        }

        isOfflineMode = false
        _statusText.value = "Connecting to Jarvis Neural Cloud..."
        _state.value = NovaOrbState.CONNECTING

        scope.launch {
            sessionMutex.withLock {
                connectSession(isResuming = false)
            }
        }
    }

    private fun speakGreeting(context: Context, text: String) {
        scope.launch(Dispatchers.Main) {
            JarvisSpeaker.speak(
                text = text,
                onStart = {
                    _state.value = NovaOrbState.SPEAKING
                    _statusText.value = "Jarvis speaking"
                },
                onDone = {
                    _state.value = NovaOrbState.LISTENING
                    _statusText.value = "Listening for 'Hey Jarvis'..."
                }
            )
        }
    }

    private fun createAudioIO() {
        audioIO?.release()
        audioIO = LiveAudioIO(
            onMicChunk = { bytes ->
                if (_isActive.value && !_muted.value && !EchoGuard.isMicMuted()) {
                    if (!isOfflineMode && client != null) {
                        client?.sendAudio(bytes)
                    }
                }
            },
            onMicAmplitude = { amp ->
                if (_state.value == NovaOrbState.LISTENING) {
                    _amplitude.value = amp
                    _vadActive.value = amp > 0.08f
                }
            }
        )
    }

    private suspend fun connectSession(isResuming: Boolean) {
        val ctx = appContext ?: return
        val apiKey = ApiKeyStore.getKey(ctx)

        if (apiKey.isNullOrBlank()) {
            isOfflineMode = true
            _state.value = NovaOrbState.LISTENING
            _statusText.value = "Jarvis Offline Mode • Ready, Boss"
            audioIO?.startRecording()
            return
        }

        currentEpoch++
        val epoch = currentEpoch

        client?.disconnect()

        val personality = PersonalityRepository.getPersonality(ctx)
        val language = PersonalityRepository.getLanguage(ctx)
        val assistantName = PersonalityRepository.getAssistantName(ctx)
        val voiceName = PersonalityRepository.getVoiceName(ctx)

        _currentPersonality.value = personality
        _currentLanguage.value = language

        val recap = if (isResuming) ConversationMemory.recap() else null
        val prompt = PersonalityPromptBuilder.build(
            personality = personality,
            language = language,
            detectedLanguage = _detectedLanguage.value,
            recap = recap,
            assistantName = assistantName
        )

        val newClient = GeminiLiveClient(
            apiKey = apiKey,
            listener = object : GeminiLiveClient.Listener {
                override fun onSetupComplete() {
                    if (epoch != currentEpoch) return
                    reconnectAttempts = 0
                    isOfflineMode = false
                    _state.value = NovaOrbState.LISTENING
                    _statusText.value = "Jarvis online • Listening, Boss"
                    audioIO?.startRecording()
                }

                override fun onAudioChunk(bytes: ByteArray) {
                    if (epoch != currentEpoch) return
                    if (_state.value != NovaOrbState.SPEAKING) {
                        EchoGuard.notifyPlaybackStarted()
                        _state.value = NovaOrbState.SPEAKING
                        _statusText.value = "$assistantName speaking, Boss"
                        VoiceLatencyTracker.markFirstAudioReceived()
                    }
                    val rms = audioIO?.playChunk(bytes) ?: 0f
                    _amplitude.value = rms

                    playbackSilenceJob?.cancel()
                    playbackSilenceJob = scope.launch {
                        delay(350L)
                        if (epoch == currentEpoch && _state.value == NovaOrbState.SPEAKING) {
                            EchoGuard.notifyPlaybackStopped()
                            _state.value = NovaOrbState.LISTENING
                            _statusText.value = "Listening, Boss..."
                        }
                    }
                }

                override fun onInputTranscript(text: String) {
                    if (epoch != currentEpoch) return
                    ConversationMemory.addTurn(MemoryRole.USER, text)
                    val detected = LanguageDetector.detect(text)
                    _detectedLanguage.value = detected
                    VoiceLatencyTracker.markUserTurnStarted()

                    // Check for offline execution interception
                    scope.launch {
                        val offlineRes = JarvisOfflineBrain.processQuery(ctx, text)
                        if (offlineRes.handled && offlineRes.isDeviceAction) {
                            _statusText.value = offlineRes.spokenReply
                        }
                    }
                }

                override fun onOutputTranscript(text: String) {
                    if (epoch != currentEpoch) return
                    ConversationMemory.addTurn(MemoryRole.ASSISTANT, text)
                }

                override fun onToolCall(id: String, name: String, args: JSONObject) {
                    if (epoch != currentEpoch) return
                    _state.value = NovaOrbState.THINKING
                    _statusText.value = "Executing $name for Boss..."

                    if (name == "build_website") {
                        val webPrompt = args.optString("prompt", "High tech website for Boss")
                        onWebsiteForgeRequested?.invoke(webPrompt)
                        val reply = JSONObject().apply {
                            put("result", "forge_started")
                            put("message", "Website forge initiated for Boss")
                        }
                        client?.sendToolResponse(id, name, reply)
                        _state.value = NovaOrbState.LISTENING
                        return
                    }

                    scope.launch {
                        val result = ActionExecutor.execute(ctx, name, args)
                        if (epoch == currentEpoch) {
                            client?.sendToolResponse(id, name, result)
                            _state.value = NovaOrbState.LISTENING
                        }
                    }
                }

                override fun onTurnComplete() {
                    if (epoch != currentEpoch) return
                    playbackSilenceJob?.cancel()
                    EchoGuard.notifyPlaybackStopped()
                    _state.value = NovaOrbState.LISTENING
                    _statusText.value = "Listening, Boss..."
                    VoiceLatencyTracker.markTurnCompleted()
                }

                override fun onInterrupted() {
                    if (epoch != currentEpoch) return
                    playbackSilenceJob?.cancel()
                    audioIO?.flushPlayback()
                    JarvisSpeaker.stop()
                    EchoGuard.notifyPlaybackStopped()
                    _state.value = NovaOrbState.LISTENING
                    _statusText.value = "Listening, Boss..."
                }

                override fun onError(message: String) {
                    if (epoch != currentEpoch) return
                    handleConnectionDrop(message)
                }

                override fun onClosed(code: Int, reason: String) {
                    if (epoch != currentEpoch) return
                    handleConnectionDrop("Code $code: $reason")
                }
            }
        )

        client = newClient
        newClient.connect(prompt, voiceName)
    }

    private fun handleConnectionDrop(reason: String) {
        if (!_isActive.value) return
        reconnectAttempts++
        VoiceLatencyTracker.recordReconnect()

        if (reconnectAttempts > 3) {
            // Seamless automatic fallback to on-device offline brain: NEVER DIE!
            isOfflineMode = true
            _state.value = NovaOrbState.LISTENING
            _statusText.value = "Jarvis Offline Protocol Active, Boss"
            audioIO?.startRecording()
            return
        }

        _state.value = NovaOrbState.CONNECTING
        _statusText.value = "Re-establishing neural link ($reconnectAttempts/3)..."

        scope.launch {
            reconnectMutex.withLock {
                delay(1200L)
                sessionMutex.withLock {
                    if (_isActive.value) {
                        connectSession(isResuming = true)
                    }
                }
            }
        }
    }

    fun handleVoiceCommandDirectly(query: String) {
        val ctx = appContext ?: return
        scope.launch {
            _state.value = NovaOrbState.THINKING
            _statusText.value = "Processing command, Boss..."

            val offlineRes = JarvisOfflineBrain.processQuery(ctx, query)
            if (offlineRes.handled) {
                val locale = when (LanguageDetector.detect(query)) {
                    DetectedLanguage.TELUGU -> Locale("te", "IN")
                    DetectedLanguage.HINDI -> Locale("hi", "IN")
                    else -> Locale.US
                }
                scope.launch(Dispatchers.Main) {
                    JarvisSpeaker.speak(
                        text = offlineRes.spokenReply,
                        locale = locale,
                        onStart = {
                            _state.value = NovaOrbState.SPEAKING
                            _statusText.value = "Jarvis speaking"
                        },
                        onDone = {
                            _state.value = NovaOrbState.LISTENING
                            _statusText.value = "Listening, Boss..."
                        }
                    )
                }
            } else {
                _state.value = NovaOrbState.LISTENING
                _statusText.value = "Listening, Boss..."
            }
        }
    }

    fun refreshInstructions(context: Context) {
        appContext = context.applicationContext
        if (_isActive.value && !isOfflineMode) {
            scope.launch {
                sessionMutex.withLock {
                    connectSession(isResuming = true)
                }
            }
        }
    }

    fun toggleMute() {
        _muted.value = !_muted.value
    }

    fun setMuted(muted: Boolean) {
        _muted.value = muted
    }

    fun stop() {
        currentEpoch++
        _isActive.value = false
        reconnectJob?.cancel()
        playbackSilenceJob?.cancel()
        reconnectJob = null
        playbackSilenceJob = null

        JarvisSpeaker.stop()

        client?.disconnect()
        client = null

        audioIO?.release()
        audioIO = null

        EchoGuard.reset()
        ConversationMemory.clear()

        _state.value = NovaOrbState.IDLE
        _statusText.value = "Jarvis standing by, Boss"
        _amplitude.value = 0f
        _vadActive.value = false
    }
}
