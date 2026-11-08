package com.Lia.assistant.flavor

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.Lia.assistant.R

object OverlayEdgeGlowController {
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    fun showGlow(context: Context) {
        if (!AccessKeyManager.isOverlayPermissionGranted(context)) return
        if (overlayView != null) return

        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            val view = View(context).apply {
                // Subtle purple/marigold rim glow
                setBackgroundResource(R.drawable.ic_launcher_background)
                alpha = 0.15f
            }
            wm.addView(view, params)
            overlayView = view
        } catch (_: Exception) {}
    }

    fun hideGlow(context: Context) {
        try {
            overlayView?.let { view ->
                windowManager?.removeView(view)
            }
        } catch (_: Exception) {}
        overlayView = null
        windowManager = null
    }

    fun updateState(isSpeaking: Boolean, isListening: Boolean) {
        overlayView?.alpha = when {
            isSpeaking -> 0.35f
            isListening -> 0.20f
            else -> 0.08f
        }
    }
}
