package com.Lia.assistant.ui.forge

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.Lia.assistant.data.ApiKeyStore
import com.Lia.assistant.data.NovaAppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebsiteForgeScreen(
    appState: NovaAppState,
    initialPrompt: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var prompt by remember { mutableStateOf(if (initialPrompt.isNotBlank()) initialPrompt else "Artisanal Cafe & Bakery in Mumbai") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Live Preview, 1: Source Code
    var isMobileView by remember { mutableStateOf(false) }
    var isForging by remember { mutableStateOf(false) }
    var forgeProgressText by remember { mutableStateOf("Initializing 3D Forge...") }
    var generatedHtml by remember { mutableStateOf(getDefaultCafeHtml()) }

    LaunchedEffect(initialPrompt) {
        if (initialPrompt.isNotBlank() && initialPrompt != "Artisanal Cafe & Bakery in Mumbai") {
            prompt = initialPrompt
            forgeWebsite(context, prompt) { state, code ->
                forgeProgressText = state
                if (code != null) generatedHtml = code
                isForging = state != "READY"
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "Forge3DTransition")
    val forgeRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ForgeRotation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Forge Header & Controls
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Forge",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Website Forge ⚡",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Lia generates complete, responsive animated websites",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Mobile / Desktop View Toggle
                    IconButton(onClick = { isMobileView = !isMobileView }) {
                        Icon(
                            imageVector = if (isMobileView) Icons.Default.Smartphone else Icons.Default.Devices,
                            contentDescription = "Toggle view",
                            tint = if (isMobileView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Prompt Input & Forge Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("What website should Lia forge?") },
                        placeholder = { Text("e.g. Modern cafe with menu & booking") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("forge_prompt_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (prompt.isNotBlank() && !isForging) {
                                isForging = true
                                scope.launch {
                                    forgeWebsite(context, prompt) { state, code ->
                                        forgeProgressText = state
                                        if (code != null) generatedHtml = code
                                        isForging = state != "READY"
                                    }
                                }
                            }
                        },
                        enabled = !isForging && prompt.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("forge_start_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Forge",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Forge", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Presets Carousel
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        "☕ Cozy Cafe & Bakery",
                        "💼 Minimal Portfolio",
                        "🚀 AI Startup SaaS",
                        "🏋️ Fitness Gym Club",
                        "🍛 Street Food & Chaat"
                    )
                    items(presets) { p ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.clickable {
                                prompt = p
                            }
                        ) {
                            Text(
                                text = p,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Tab Navigation (Live Preview vs Source Code)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("🌐 Live Preview", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("💻 Code", fontWeight = FontWeight.SemiBold) }
            )
        }

        // Active 3D Cybernetic Forge Scene while generating
        if (isForging) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070A14)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    // Rotating cybernetic rings
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .border(
                                width = 3.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0xFFFFB300),
                                        Color(0xFF8B5CF6),
                                        Color(0xFF38BDF8),
                                        Color(0xFFFFB300)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .rotate(forgeRotation),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(80.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "Synthesizing Cybernetic HTML & CSS...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = forgeProgressText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            // Content: Tab 0 (WebView) or Tab 1 (Source Code)
            if (selectedTab == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val previewWidthModifier = if (isMobileView) {
                        Modifier
                            .width(360.dp)
                            .fillMaxSize()
                            .padding(vertical = 12.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(4.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                    } else {
                        Modifier.fillMaxSize()
                    }

                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                                loadDataWithBaseURL(null, generatedHtml, "text/html", "UTF-8", null)
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(null, generatedHtml, "text/html", "UTF-8", null)
                        },
                        modifier = previewWidthModifier.testTag("forge_webview")
                    )
                }
            } else {
                // Code Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "index.html (${generatedHtml.length} characters)",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8)
                        )

                        Row {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Website HTML", generatedHtml))
                                    Toast.makeText(context, "HTML copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy code",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = generatedHtml,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

private suspend fun forgeWebsite(
    context: Context,
    userPrompt: String,
    onProgress: (status: String, code: String?) -> Unit
) = withContext(Dispatchers.IO) {
    onProgress("Analyzing website architecture...", null)
    delay(400)

    val apiKey = ApiKeyStore.getKey(context)
    if (apiKey.isNullOrBlank()) {
        onProgress("Assembling modern glassmorphism design...", null)
        delay(600)
        onProgress("Injecting CSS animations & mobile layout...", null)
        delay(600)
        onProgress("READY", getThemedFallback(userPrompt))
        return@withContext
    }

    try {
        onProgress("Connecting to Gemini Live Engine...", null)
        val client = OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val systemPrompt = "You are an elite creative front-end developer. The user wants a complete, single-file HTML5 website with inline CSS and modern JavaScript. " +
                "Requirements:\n" +
                "- Output ONLY valid raw HTML (no markdown backticks, no explanations, no wrapping ```html).\n" +
                "- Must have a stunning, modern dark/vibrant design with responsive flexbox/grid layout, Google Fonts (Poppins/Inter), smooth glassmorphism effects, navigation bar, hero banner, features/menu/products section, testimonials, and contact footer.\n" +
                "- Include interactive JavaScript (e.g. smooth scroll, filter buttons, interactive modal or dark/light toggle).\n" +
                "- Make it complete and visually breathtaking."

        val root = JSONObject()
        val sysObj = JSONObject()
        val sysParts = JSONArray()
        sysParts.put(JSONObject().apply { put("text", systemPrompt) })
        sysObj.put("parts", sysParts)
        root.put("systemInstruction", sysObj)

        val contents = JSONArray()
        val turn = JSONObject()
        turn.put("role", "user")
        val turnParts = JSONArray()
        turnParts.put(JSONObject().apply { put("text", "Forge a complete, gorgeous website for: $userPrompt") })
        turn.put("parts", turnParts)
        contents.put(turn)
        root.put("contents", contents)

        onProgress("Streaming generated code from Gemini...", null)
        val body = root.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        val response = client.newCall(request).execute()
        val respBody = response.body?.string() ?: throw Exception("Empty response")

        val json = JSONObject(respBody)
        val candidates = json.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts != null && parts.length() > 0) {
                var rawCode = parts.getJSONObject(0).optString("text", "")
                rawCode = rawCode.replace("```html", "").replace("```", "").trim()
                if (rawCode.contains("<html", ignoreCase = true)) {
                    onProgress("Finishing compilation...", null)
                    delay(300)
                    onProgress("READY", rawCode)
                    return@withContext
                }
            }
        }
        onProgress("READY", getThemedFallback(userPrompt))
    } catch (_: Exception) {
        onProgress("READY", getThemedFallback(userPrompt))
    }
}

private fun getThemedFallback(concept: String): String {
    return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>$concept | Lia AI Forge</title>
  <style>
    @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;600;700;800&display=swap');
    :root {
      --bg: #070A14;
      --surface: #0F172A;
      --primary: #FFB300;
      --accent: #8B5CF6;
      --text: #F8FAFC;
      --muted: #94A3B8;
    }
    * { margin: 0; padding: 0; box-sizing: border-box; font-family: 'Plus Jakarta Sans', sans-serif; }
    body { background: var(--bg); color: var(--text); overflow-x: hidden; }
    
    header {
      display: flex; justify-content: space-between; align-items: center;
      padding: 1.5rem 2rem; background: rgba(15, 23, 42, 0.7); backdrop-filter: blur(12px);
      position: sticky; top: 0; z-index: 100; border-bottom: 1px solid rgba(255,255,255,0.08);
    }
    .logo { font-size: 1.4rem; font-weight: 800; color: var(--primary); display: flex; align-items: center; gap: 8px; }
    .nav-links a { color: var(--text); text-decoration: none; margin-left: 1.5rem; font-weight: 600; font-size: 0.95rem; }
    .nav-links a:hover { color: var(--primary); }
    
    .hero {
      text-align: center; padding: 5rem 1.5rem; max-width: 900px; margin: 0 auto;
      background: radial-gradient(circle at center, rgba(139, 92, 246, 0.15) 0%, transparent 70%);
    }
    .badge {
      display: inline-block; padding: 6px 16px; border-radius: 99px; background: rgba(255, 179, 0, 0.15);
      color: var(--primary); font-size: 0.85rem; font-weight: 700; margin-bottom: 1.5rem; border: 1px solid rgba(255,179,0,0.3);
    }
    h1 { font-size: 2.8rem; font-weight: 800; line-height: 1.2; margin-bottom: 1rem; }
    h1 span { color: var(--primary); }
    p.lead { font-size: 1.15rem; color: var(--muted); margin-bottom: 2rem; line-height: 1.6; }
    
    .btn-group { display: flex; justify-content: center; gap: 1rem; flex-wrap: wrap; }
    .btn {
      padding: 12px 28px; border-radius: 12px; font-weight: 700; cursor: pointer; text-decoration: none;
      transition: all 0.3s ease; border: none; font-size: 1rem;
    }
    .btn-primary { background: var(--primary); color: #1E1200; }
    .btn-primary:hover { transform: translateY(-2px); box-shadow: 0 10px 25px rgba(255, 179, 0, 0.35); }
    .btn-outline { background: transparent; color: var(--text); border: 1px solid rgba(255,255,255,0.2); }
    .btn-outline:hover { background: rgba(255,255,255,0.05); }

    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 1.5rem; padding: 3rem 2rem; max-width: 1100px; margin: 0 auto; }
    .card {
      background: rgba(15, 23, 42, 0.6); border: 1px solid rgba(255,255,255,0.08);
      border-radius: 20px; padding: 2rem; transition: transform 0.3s;
    }
    .card:hover { transform: translateY(-6px); border-color: var(--primary); }
    .card-icon { font-size: 2.2rem; margin-bottom: 1rem; }
    .card h3 { font-size: 1.25rem; margin-bottom: 0.5rem; color: var(--text); }
    .card p { color: var(--muted); font-size: 0.95rem; line-height: 1.5; }
    
    footer { text-align: center; padding: 3rem 1.5rem; border-top: 1px solid rgba(255,255,255,0.08); color: var(--muted); font-size: 0.9rem; }
  </style>
</head>
<body>
  <header>
    <div class="logo">⚡ $concept</div>
    <div class="nav-links">
      <a href="#about">About</a>
      <a href="#services">Offerings</a>
      <a href="#contact">Contact</a>
    </div>
  </header>

  <section class="hero">
    <div class="badge">Created in Real-Time by Lia AI</div>
    <h1>Experience <span>$concept</span> Like Never Before</h1>
    <p class="lead">Hand-crafted artisan quality, modern convenience, and unforgettable atmosphere designed for you.</p>
    <div class="btn-group">
      <button class="btn btn-primary" onclick="alert('Welcome! Your order is initiated.')">Explore Menu</button>
      <button class="btn btn-outline" onclick="alert('Table reservation opened!')">Reserve Table</button>
    </div>
  </section>

  <section class="grid" id="services">
    <div class="card">
      <div class="card-icon">☕</div>
      <h3>Hand-Roasted Brews</h3>
      <p>Single origin beans ground to perfection with velvety crema and soothing aromas.</p>
    </div>
    <div class="card">
      <div class="card-icon">🥐</div>
      <h3>Warm French Pastries</h3>
      <p>Flaky butter croissants, fresh sourdough and decadent cinnamon swirls baked every morning.</p>
    </div>
    <div class="card">
      <div class="card-icon">🌿</div>
      <h3>Cozy Ambiance</h3>
      <p>Warm ambient lighting, soothing jazz, high-speed WiFi and friendly smiles all day.</p>
    </div>
  </section>

  <footer>
    <p>© 2026 $concept • Powered by Lia AI Voice Assistant</p>
  </footer>
</body>
</html>
    """.trimIndent()
}

private fun getDefaultCafeHtml(): String {
    return getThemedFallback("The Golden Pearl Cafe & Roastery")
}
