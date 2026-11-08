package com.Lia.assistant

import android.app.Application
import com.Lia.assistant.action.InstalledAppLabelCache

class LiaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        InstalledAppLabelCache.prewarm(this)
    }
}
