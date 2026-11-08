package com.Lia.assistant.action

import android.content.Context
import android.content.Intent

object AppLauncher {
    suspend fun findApp(context: Context, query: String): Pair<String, String>? {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) return null

        val appMap = InstalledAppLabelCache.get(context)

        // 1. Exact match
        if (appMap.containsKey(cleanQuery)) {
            return Pair(cleanQuery, appMap[cleanQuery]!!)
        }

        val queryWords = cleanQuery.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val candidates = mutableListOf<Pair<String, String>>()

        // 2. Candidates with key length >= 3 where key startsWith query or query startsWith key
        // or every query word prefix-matches some key word.
        for ((label, pkg) in appMap) {
            if (label.length < 3) continue

            val labelWords = label.split(Regex("\\s+")).filter { it.isNotEmpty() }

            val prefixCondition = label.startsWith(cleanQuery) || cleanQuery.startsWith(label)
            val allWordsMatch = queryWords.isNotEmpty() && queryWords.all { qWord ->
                labelWords.any { lWord -> lWord.startsWith(qWord) }
            }

            if (prefixCondition || allWordsMatch) {
                candidates.add(Pair(label, pkg))
            }
        }

        if (candidates.isEmpty()) return null

        // Pick the closest label length to the query
        return candidates.minByOrNull { Math.abs(it.first.length - cleanQuery.length) }
    }

    fun launch(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
