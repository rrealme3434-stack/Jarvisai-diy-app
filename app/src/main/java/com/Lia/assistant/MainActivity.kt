package com.Lia.assistant

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.Lia.assistant.data.ApiKeyStore
import com.Lia.assistant.data.AssistantBrand
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.ui.chat.ChatScreen
import com.Lia.assistant.ui.forge.WebsiteForgeScreen
import com.Lia.assistant.ui.settings.SettingsScreen
import com.Lia.assistant.ui.theme.LiaAssistantTheme
import com.Lia.assistant.ui.voice.VoiceScreen
import com.Lia.assistant.voice.VoiceSessionManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        setContent {
            val scope = rememberCoroutineScope()
            val appState = remember { NovaAppState(this, scope) }

            LiaAssistantTheme(darkTheme = appState.themeMode == "DARK") {
                JarvisAppContent(appState)
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Voice : Screen("voice", "Jarvis HUD", Icons.Default.Mic)
    object Chat : Screen("chat", "Chat", Icons.Default.Chat)
    object Forge : Screen("forge", "Web Forge", Icons.Default.AutoAwesome)
    object Settings : Screen("settings", "Protocols", Icons.Default.Settings)
}

@Composable
fun JarvisAppContent(appState: NovaAppState) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(Screen.Voice.route) }
    var forgePrompt by remember { mutableStateOf("") }
    var showOnboarding by remember { mutableStateOf(!appState.hasOnboarded) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionsLauncher.launch(needed.toTypedArray())
        }
    }

    LaunchedEffect(Unit) {
        VoiceSessionManager.onWebsiteForgeRequested = { prompt ->
            forgePrompt = prompt
            currentScreen = Screen.Forge.route
        }
    }

    if (currentScreen != Screen.Voice.route) {
        BackHandler {
            currentScreen = Screen.Voice.route
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                val screens = listOf(Screen.Voice, Screen.Chat, Screen.Forge, Screen.Settings)
                screens.forEach { screen ->
                    val isSelected = currentScreen == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen.route },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Voice.route -> VoiceScreen(
                    appState = appState,
                    onNavigateToSettings = { currentScreen = Screen.Settings.route },
                    onNavigateToForge = { prompt ->
                        forgePrompt = prompt
                        currentScreen = Screen.Forge.route
                    }
                )
                Screen.Chat.route -> ChatScreen(
                    appState = appState,
                    onNavigateToVoice = { currentScreen = Screen.Voice.route },
                    onNavigateToForge = { prompt ->
                        forgePrompt = prompt
                        currentScreen = Screen.Forge.route
                    }
                )
                Screen.Forge.route -> WebsiteForgeScreen(
                    appState = appState,
                    initialPrompt = forgePrompt
                )
                Screen.Settings.route -> SettingsScreen(
                    appState = appState
                )
            }

            // Onboarding Welcome Modal
            if (showOnboarding) {
                JarvisOnboardingDialog(
                    assistantName = appState.assistantName,
                    hasApiKey = ApiKeyStore.hasKey(context),
                    onDismiss = {
                        appState.applyHasOnboarded(true)
                        showOnboarding = false
                    },
                    onOpenSettings = {
                        appState.applyHasOnboarded(true)
                        showOnboarding = false
                        currentScreen = Screen.Settings.route
                    }
                )
            }
        }
    }
}

@Composable
fun JarvisOnboardingDialog(
    assistantName: String,
    hasApiKey: Boolean,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Arc Reactor",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "$assistantName Online",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Welcome aboard, Boss",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Your futuristic AI assistant equipped with real-time Gemini Live voice, Telugu & multilingual speech, offline device hardware controls, and 3D Website Forge.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "⚡ Key Capabilities:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Say 'Hey Jarvis' or tap Wake Jarvis", style = MaterialTheme.typography.bodySmall)
                        Text("• Telugu voice & text: 'నువ్వు ఎవరు జార్విస్?'", style = MaterialTheme.typography.bodySmall)
                        Text("• Offline device controls: Flashlight, Volume, Battery & Apps", style = MaterialTheme.typography.bodySmall)
                        Text("• 'Lia, ek cafe ki website banao' to forge live sites", style = MaterialTheme.typography.bodySmall)
                        Text("• Lock screen voice execution & alert reading", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Engage Jarvis, Boss")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Configure Key")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
