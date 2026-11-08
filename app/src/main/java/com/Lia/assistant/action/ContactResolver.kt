package com.Lia.assistant.action

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

sealed class ContactLookupResult {
    data class Found(val name: String, val phoneNumber: String) : ContactLookupResult()
    object NoPermission : ContactLookupResult()
    object NotFound : ContactLookupResult()
}

object ContactResolver {
    private val PHONE_NUMBER_REGEX = Regex("^[+0-9\\s()\\-]{5,}$")

    fun lookup(context: Context, query: String): ContactLookupResult {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return ContactLookupResult.NotFound

        // If it looks like a phone number directly, return it
        val digitsOnly = trimmed.replace(Regex("[^0-9+]"), "")
        if (digitsOnly.length >= 5 && PHONE_NUMBER_REGEX.matches(trimmed)) {
            return ContactLookupResult.Found(name = trimmed, phoneNumber = trimmed)
        }

        // Check READ_CONTACTS permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return ContactLookupResult.NoPermission
        }

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$trimmed%")

        val candidates = mutableListOf<Pair<String, String>>() // (name, number)
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT 5"
            )

            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val name = if (nameIdx >= 0) it.getString(nameIdx) else null
                    val number = if (numIdx >= 0) it.getString(numIdx) else null
                    if (!name.isNullOrBlank() && !number.isNullOrBlank()) {
                        candidates.add(Pair(name, number))
                    }
                }
            }
        } catch (_: Exception) {
            return ContactLookupResult.NotFound
        }

        if (candidates.isEmpty()) return ContactLookupResult.NotFound

        val lowerQuery = trimmed.lowercase()
        // Rank: exact > prefix > contains
        val best = candidates.minByOrNull { (name, _) ->
            val lower = name.lowercase()
            when {
                lower == lowerQuery -> 0
                lower.startsWith(lowerQuery) -> 1
                lower.contains(lowerQuery) -> 2
                else -> 3
            }
        }

        return if (best != null) {
            ContactLookupResult.Found(best.first, best.second)
        } else {
            ContactLookupResult.NotFound
        }
    }
}
