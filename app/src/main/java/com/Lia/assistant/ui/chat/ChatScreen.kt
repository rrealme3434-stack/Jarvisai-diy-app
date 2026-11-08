package com.Lia.assistant.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Lia.assistant.data.ApiKeyStore
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.device.JarvisOfflineBrain
import com.Lia.assistant.voice.LanguageDetector
import com.Lia.assistant.voice.PersonalityPromptBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun ChatScreen(
    appState: NovaAppState,
    onNavigateToVoice: () -> Unit,
    onNavigateToForge: (prompt: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                isUser = false,
                text = "All systems nominal, Boss. I am ${appState.assistantName}, standing by. Ask me anything, command device hardware, or say 'Build a cafe website'!"
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage(textToSend: String) {
        val query = textToSend.trim()
        if (query.isEmpty() || isSending) return

        if (query.contains("website", ignoreCase = true) && (query.contains("banao", ignoreCase = true) || query.contains("build", ignoreCase = true))) {
            messages.add(ChatMessage(isUser = true, text = query))
            messages.add(ChatMessage(isUser = false, text = "Initiating Website Forge for you, Boss! The holographic build scene is compiling your site..."))
            onNavigateToForge(query)
            inputText = ""
            return
        }

        messages.add(ChatMessage(isUser = true, text = query))
        inputText = ""
        isSending = true

        scope.launch {
            try {
                // First check on-device offline brain for instant execution or offline mode
                val offlineRes = JarvisOfflineBrain.processQuery(context, query)
                if (offlineRes.handled) {
                    messages.add(ChatMessage(isUser = false, text = offlineRes.spokenReply))
                    isSending = false
                    return@launch
                }

                val apiKey = ApiKeyStore.getKey(context)
                if (apiKey.isNullOrBlank()) {
                    // Smart Offline fallback when no key is set
                    val reply = when (LanguageDetector.detect(query)) {
                        com.Lia.assistant.voice.DetectedLanguage.TELUGU -> "నేను సిద్ధంగా ఉన్నాను బాస్. మీ ప్రశ్నకు సహాయపడటానికి క్లౌడ్ కీ జోడించండి లేదా డివైజ్ కమాండ్స్ ఉపయోగించండి."
                        com.Lia.assistant.voice.DetectedLanguage.HINDI -> "जी बॉस, डिवाइस कंट्रोल्स पूरी तरह सक्रिय हैं। ऑनलाइन AI के लिए सेटिंग्स में की जोड़ें।"
                        else -> "All local device protocols are active, Boss. To connect to full Gemini cloud reasoning, add your API key in Settings."
                    }
                    messages.add(ChatMessage(isUser = false, text = reply))
                    isSending = false
                    return@launch
                }

                val detected = LanguageDetector.detect(query)
                val systemPrompt = PersonalityPromptBuilder.build(
                    personality = appState.personality,
                    language = appState.language,
                    detectedLanguage = detected,
                    recap = null,
                    assistantName = appState.assistantName
                )

                val responseText = executeGeminiText(apiKey, systemPrompt, messages.toList())
                messages.add(ChatMessage(isUser = false, text = responseText))
            } catch (e: Exception) {
                // Fallback gracefully without crash
                messages.add(ChatMessage(isUser = false, text = "Systems online, Boss. Note: ${e.message ?: "Cloud link busy, standing by"}"))
            } finally {
                isSending = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Chat Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Jarvis",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${appState.assistantName} Protocol",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Serving Boss • ${appState.language.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateToVoice) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Switch to Voice",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            messages.clear()
                            messages.add(
                                ChatMessage(
                                    isUser = false,
                                    text = "Neural memory reset, Boss. How may I assist you?"
                                )
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear Chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(messages, key = { it.id }) { msg ->
                val isUser = msg.isUser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        color = if (isUser) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .testTag(if (isUser) "user_message_bubble" else "assistant_message_bubble")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )

                            if (!isUser) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy message",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Jarvis message", msg.text))
                                                Toast.makeText(context, "Copied to clipboard, Boss", Toast.LENGTH_SHORT).show()
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isSending) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Jarvis processing, Boss...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Quick Suggestion Chips (Multilingual + Device commands)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf(
                "Turn on flashlight",
                "లైట్ ఆన్ చేయి",
                "Battery percentage, Boss",
                "Lia, ek cafe ki website banao",
                "ఎలా ఉన్నావు జార్విస్?",
                "Increase volume",
                "Who are you, Jarvis?"
            )
            items(suggestions) { text ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { sendMessage(text) }
                ) {
                    Text(
                        text = text,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Input Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Command ${appState.assistantName}, Boss...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_text_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { sendMessage(inputText) },
                    enabled = inputText.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank() && !isSending) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank() && !isSending) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private suspend fun executeGeminiText(
    apiKey: String,
    systemPrompt: String,
    history: List<ChatMessage>
): String = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

    val root = JSONObject()

    val sysObj = JSONObject()
    val sysParts = JSONArray()
    sysParts.put(JSONObject().apply { put("text", systemPrompt) })
    sysObj.put("parts", sysParts)
    root.put("systemInstruction", sysObj)

    val contents = JSONArray()
    for (msg in history.takeLast(10)) {
        val c = JSONObject()
        c.put("role", if (msg.isUser) "user" else "model")
        val parts = JSONArray()
        parts.put(JSONObject().apply { put("text", msg.text) })
        c.put("parts", parts)
        contents.put(c)
    }
    root.put("contents", contents)

    val body = root.toString().toRequestBody("application/json".toMediaType())
    val request = Request.Builder().url(url).post(body).build()

    val response = client.newCall(request).execute()
    val respBody = response.body?.string() ?: throw Exception("Empty response from Gemini")

    if (!response.isSuccessful) {
        val errObj = try { JSONObject(respBody).optJSONObject("error")?.optString("message") } catch (_: Exception) { null }
        throw Exception(errObj ?: "API Error ${response.code}")
    }

    val json = JSONObject(respBody)
    val candidates = json.optJSONArray("candidates")
    if (candidates != null && candidates.length() > 0) {
        val content = candidates.getJSONObject(0).optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        if (parts != null && parts.length() > 0) {
            return@withContext parts.getJSONObject(0).optString("text", "No text generated.")
        }
    }

    return@withContext "No response content received, Boss."
}
