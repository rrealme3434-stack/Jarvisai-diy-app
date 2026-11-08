package com.Lia.assistant.device

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class JarvisNotification(
    val id: String,
    val packageName: String,
    val appTitle: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class JarvisNotificationListenerService : NotificationListenerService() {

    companion object {
        private val _notifications = MutableStateFlow<List<JarvisNotification>>(emptyList())
        val notifications: StateFlow<List<JarvisNotification>> = _notifications.asStateFlow()

        fun getLatestMessageSummary(): String {
            val list = _notifications.value.takeLast(3)
            if (list.isEmpty()) return "No unread notifications at the moment, Boss."
            val sb = StringBuilder("Here are your latest alerts, Boss:\n")
            list.forEach {
                sb.append("• From ${it.title} on ${it.appTitle}: ${it.text}\n")
            }
            return sb.toString().trim()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val pm = packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            sbn.packageName
        }

        val item = JarvisNotification(
            id = "${sbn.packageName}_${sbn.id}_${sbn.postTime}",
            packageName = sbn.packageName,
            appTitle = appName,
            title = title,
            text = text,
            timestamp = sbn.postTime
        )

        val current = _notifications.value.toMutableList()
        current.add(item)
        while (current.size > 20) {
            current.removeAt(0)
        }
        _notifications.value = current
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}
