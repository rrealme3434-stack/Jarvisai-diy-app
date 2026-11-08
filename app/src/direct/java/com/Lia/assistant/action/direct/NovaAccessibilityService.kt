package com.Lia.assistant.action.direct

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.Lia.assistant.device.DeviceController
import kotlinx.coroutines.delay

class NovaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        DeviceController.screenshotHandler = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val ok = performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                if (ok) "Capturing screenshot for Boss." else "Failed to capture screenshot."
            } else {
                "Screenshot requires Android 9+, Boss."
            }
        }

        DeviceController.lockScreenHandler = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val ok = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                if (ok) "Device locked, Boss." else "Failed to lock screen."
            } else {
                "Lock screen requires Android 9+, Boss."
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (instance == this) {
            instance = null
            DeviceController.screenshotHandler = null
            DeviceController.lockScreenHandler = null
        }
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: NovaAccessibilityService? = null
            private set

        fun isEnabled(context: Context): Boolean {
            if (instance != null) return true
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val expectedServiceName = "${context.packageName}/${NovaAccessibilityService::class.java.name}"
            return enabledServices.contains(expectedServiceName) || enabledServices.contains(NovaAccessibilityService::class.java.simpleName)
        }
    }

    fun readScreen(): String {
        val root = rootInActiveWindow ?: return "No active window available"
        val sb = StringBuilder()
        traverseNodes(root, sb, 0)
        return if (sb.isEmpty()) "Screen appears empty or content is not exposed" else sb.toString().trim()
    }

    private fun traverseNodes(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int) {
        if (depth > 12) return

        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val isClickable = node.isClickable
        val isEditable = node.isEditable

        val label = when {
            !text.isNullOrEmpty() && !desc.isNullOrEmpty() -> "$text ($desc)"
            !text.isNullOrEmpty() -> text
            !desc.isNullOrEmpty() -> desc
            else -> null
        }

        if (label != null) {
            val flags = mutableListOf<String>()
            if (isClickable) flags.add("clickable")
            if (isEditable) flags.add("input")
            val flagStr = if (flags.isNotEmpty()) " [${flags.joinToString(", ")}]" else ""
            sb.append("• ").append(label).append(flagStr).append("\n")
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                traverseNodes(child, sb, depth + 1)
            }
        }
    }

    fun tapText(target: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val cleanTarget = target.trim().lowercase()

        val foundNodes = mutableListOf<AccessibilityNodeInfo>()
        findMatchingNodes(root, cleanTarget, foundNodes)

        for (node in foundNodes) {
            var curr: AccessibilityNodeInfo? = node
            while (curr != null) {
                if (curr.isClickable) {
                    val clicked = curr.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (clicked) return true
                }
                curr = curr.parent
            }
        }

        for (node in foundNodes) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                return clickAt(rect.centerX().toFloat(), rect.centerY().toFloat())
            }
        }
        return false
    }

    private fun findMatchingNodes(
        node: AccessibilityNodeInfo,
        query: String,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""

        if (text.contains(query) || desc.contains(query)) {
            results.add(node)
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findMatchingNodes(child, query, results)
            }
        }
    }

    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findFirstEditable(root)
            ?: return false

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun findFirstEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstEditable(child)
            if (found != null) return found
        }
        return null
    }

    fun scroll(direction: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (direction.equals("up", ignoreCase = true) || direction.equals("backward", ignoreCase = true)) {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }
        return performScrollOnFirstScrollable(root, action)
    }

    private fun performScrollOnFirstScrollable(node: AccessibilityNodeInfo, action: Int): Boolean {
        if (node.isScrollable) {
            if (node.performAction(action)) return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (performScrollOnFirstScrollable(child, action)) return true
        }
        return false
    }

    fun goBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun goHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    private fun clickAt(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 80)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    suspend fun autoTapSend(maxTries: Int = 12, delayMs: Long = 400): Boolean {
        val sendLabels = listOf("send", "bhejo", "submit", "arrow_forward", "continue to chat")
        for (attempt in 1..maxTries) {
            delay(delayMs)
            for (label in sendLabels) {
                if (tapText(label)) {
                    return true
                }
            }
        }
        return false
    }
}
