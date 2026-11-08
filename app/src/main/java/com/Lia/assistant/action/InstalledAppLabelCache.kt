package com.Lia.assistant.action

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object InstalledAppLabelCache {
    private const val TTL_MS = 15 * 60 * 1000L // 15 minutes
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, String>() // lowercase label -> packageName
    private var lastLoadedTime = 0L

    fun prewarm(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            get(context)
        }
    }

    suspend fun get(context: Context): Map<String, String> {
        val now = System.currentTimeMillis()
        mutex.withLock {
            if (cache.isNotEmpty() && (now - lastLoadedTime) < TTL_MS) {
                return HashMap(cache)
            }
            cache.clear()
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }

            for (info in resolveInfos) {
                val label = info.loadLabel(pm)?.toString()?.trim()?.lowercase()
                val pkg = info.activityInfo?.packageName
                if (!label.isNullOrEmpty() && !pkg.isNullOrEmpty()) {
                    cache[label] = pkg
                }
            }
            lastLoadedTime = now
            return HashMap(cache)
        }
    }
}
