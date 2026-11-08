package com.Lia.assistant.flavor

import android.content.Context

object AccessKeyManager {
    fun isAccessibilityEnabled(context: Context): Boolean = false
    fun isOverlayPermissionGranted(context: Context): Boolean = false
    fun openAccessibilitySettings(context: Context) {}
}
