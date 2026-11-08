package com.Lia.assistant.ui.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.data.Personality
import com.Lia.assistant.device.DeviceController
import com.Lia.assistant.ui.orb.EdgeGlowEffect
import com.Lia.assistant.ui.orb.PearlOrb3D
import com.Lia.assistant.voice.ConversationMemory
import com.Lia.assistant.voice.NovaOrbState
import com.Lia.assistant.voice.VoiceForegroundService
import com.Lia.assistant.voice.VoiceLatencyTracker
import com.Lia.assistant.voice.VoiceSessionManager

@Composable
fun VoiceScreen(
    appState: NovaAppState,
    onNavigateToSettings: () -> Unit,
    onNavigateToForge: (prompt: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val orbState by VoiceSessionManager.state.collectAsState()
    val amplitude by VoiceSessionManager.amplitude.collectAsState()
    val isMuted by VoiceSessionManager.muted.collectAsState()
    val statusText by VoiceSessionManager.statusText.collectAsState()
    val isActive by VoiceSessionManager.isActive.collectAsState()
    val detectedLang by VoiceSessionManager.detectedLanguage.collectAsState()
    val reconnectCount by VoiceLatencyTracker.reconnectCount.collectAsState()

    var showPersonalityPicker by remember { mutableStateOf(false) }
    var torchActive by remember { mutableStateOf(DeviceController.isFlashlightOn()) }

    val recentTurns = remember(orbState, statusText) {
        ConversationMemory.getTurns().takeLast(2)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Holographic screen edge glow
        EdgeGlowEffect(
            state = orbState,
            amplitude = amplitude,
            reducedMotion = appState.reducedMotion
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top HUD Telemetry Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Assistant Name & HUD Protocol Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isActive) MaterialTheme.colorScheme.primary else Color(0xFF64748B))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${appState.assistantName} Mark VII",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Quick Flashlight & Telemetry HUD buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val reply = DeviceController.toggleFlashlight(context)
                                torchActive = DeviceController.isFlashlightOn()
                                VoiceSessionManager.handleVoiceCommandDirectly(if (torchActive) "torch on" else "torch off")
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (torchActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = if (torchActive) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = "Flashlight",
                                tint = if (torchActive) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Personality Chip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { showPersonalityPicker = !showPersonalityPicker }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("personality_chip")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Psychology,
                                contentDescription = "Personality",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = appState.personality.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Telemetry line: Battery + Language + Reconnects
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = "Battery",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Power: 92% • All Systems Nominal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = "Language",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (detectedLang.label != "Unknown") "Lang: ${detectedLang.label}" else "Lang: ${appState.language.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                }
            }

            // Quick Personality Selector if expanded
            AnimatedVisibility(visible = showPersonalityPicker, enter = fadeIn(), exit = fadeOut()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Personality.entries) { p ->
                        val isSelected = p == appState.personality
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                appState.applyPersonality(p)
                                showPersonalityPicker = false
                            }
                        ) {
                            Text(
                                text = p.displayName,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Center: 3D Holographic Arc Reactor Orb & Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PearlOrb3D(
                    state = orbState,
                    amplitude = amplitude,
                    reducedMotion = appState.reducedMotion
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when (orbState) {
                        NovaOrbState.SPEAKING -> MaterialTheme.colorScheme.primary
                        NovaOrbState.LISTENING -> Color(0xFF67EEFF)
                        NovaOrbState.THINKING -> MaterialTheme.colorScheme.secondary
                        NovaOrbState.ERROR -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center
                )

                // Conversation HUD snippet
                if (recentTurns.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            recentTurns.forEach { turn ->
                                Text(
                                    text = "${if (turn.role.name == "USER") "Boss" else appState.assistantName}: ${turn.text}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (turn.role.name == "USER") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            // Bottom Action Controls
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Quick Command Chips (English, Telugu, Hindi, System commands)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val commands = listOf(
                        "Turn on Flashlight",
                        "లైట్ ఆన్ చేయి",
                        "Battery status, Boss",
                        "Lia, ek cafe ki website banao",
                        "Volume up",
                        "ఎలా ఉన్నావు జార్విస్?",
                        "Open YouTube",
                        "Take screenshot"
                    )
                    items(commands) { cmd ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable {
                                if (cmd.contains("website", ignoreCase = true)) {
                                    onNavigateToForge(cmd)
                                } else {
                                    VoiceSessionManager.handleVoiceCommandDirectly(cmd)
                                }
                            }
                        ) {
                            Text(
                                text = cmd,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Start / Stop Voice Button & Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute / Unmute
                    IconButton(
                        onClick = { VoiceSessionManager.toggleMute() },
                        enabled = isActive,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                if (isMuted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Main Glowing Arc Reactor Launch Button
                    Button(
                        onClick = {
                            if (isActive) {
                                VoiceForegroundService.stop(context)
                            } else {
                                VoiceForegroundService.start(context)
                            }
                        },
                        modifier = Modifier
                            .height(60.dp)
                            .width(185.dp)
                            .testTag("voice_toggle_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(30.dp)
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isActive) "Stop" else "Start Jarvis",
                            tint = if (isActive) Color.White else MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isActive) "Disengage" else "Wake Jarvis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.White else MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Volume Quick Adjust
                    IconButton(
                        onClick = {
                            DeviceController.adjustVolume(context, 1)
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("volume_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Volume Up",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
