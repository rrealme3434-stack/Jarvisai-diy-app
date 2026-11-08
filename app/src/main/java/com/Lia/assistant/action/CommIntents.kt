package com.Lia.assistant.action

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import java.net.URLEncoder

object CommIntents {
    fun call(context: Context, phoneNumber: String): String {
        val clean = phoneNumber.trim()
        val hasCallPhone = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPhone) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$clean"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(intent)
            if (hasCallPhone) "calling" else "dialer_opened_no_permission"
        } catch (_: Exception) {
            "failed_to_open_dialer"
        }
    }

    fun message(context: Context, recipient: String, messageText: String, preferWhatsApp: Boolean = false): String {
        val digits = recipient.replace(Regex("[^0-9+]"), "")

        if (preferWhatsApp || digits.length >= 7) {
            // Check if WhatsApp is installed
            val pm = context.packageManager
            val waPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
            val installedWa = waPackages.firstOrNull { pkg ->
                try {
                    pm.getPackageInfo(pkg, 0)
                    true
                } catch (_: Exception) {
                    false
                }
            }

            if (installedWa != null) {
                try {
                    val encoded = URLEncoder.encode(messageText, "UTF-8")
                    val uri = Uri.parse("https://wa.me/$digits?text=$encoded")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage(installedWa)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return "composer_opened"
                } catch (_: Exception) {
                    // Fall back to SMS
                }
            }
        }

        // Default SMS fallback
        return try {
            val uri = Uri.parse("smsto:$recipient")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "composer_opened"
        } catch (_: Exception) {
            "failed_to_open_messaging"
        }
    }
}
