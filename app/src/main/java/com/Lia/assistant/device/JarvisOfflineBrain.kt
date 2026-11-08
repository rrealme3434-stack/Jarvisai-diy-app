package com.Lia.assistant.device

import android.content.Context
import com.Lia.assistant.action.AppLauncher
import com.Lia.assistant.action.CommIntents
import com.Lia.assistant.action.ContactLookupResult
import com.Lia.assistant.action.ContactResolver
import com.Lia.assistant.voice.DetectedLanguage
import com.Lia.assistant.voice.LanguageDetector

object JarvisOfflineBrain {

    data class ProcessResult(
        val handled: Boolean,
        val spokenReply: String,
        val isDeviceAction: Boolean = false
    )

    suspend fun processQuery(context: Context, query: String): ProcessResult {
        val clean = query.trim().lowercase()
        val detected = LanguageDetector.detect(clean)

        // 1. Flashlight / Torch
        if (clean.contains("torch on") || clean.contains("flashlight on") ||
            clean.contains("turn on flashlight") || clean.contains("లైట్ ఆన్") ||
            clean.contains("torch chalu") || clean.contains("batti jalao")
        ) {
            val res = DeviceController.toggleFlashlight(context, true)
            val reply = when (detected) {
                DetectedLanguage.TELUGU -> "ఫ్లాష్‌లైట్ ఆన్ చేశాను, బాస్."
                DetectedLanguage.HINDI -> "टॉर्च चालू कर दी गई है, बॉस।"
                else -> res
            }
            return ProcessResult(true, reply, true)
        }

        if (clean.contains("torch off") || clean.contains("flashlight off") ||
            clean.contains("turn off flashlight") || clean.contains("లైట్ ఆఫ్") ||
            clean.contains("torch band") || clean.contains("batti bujhao")
        ) {
            val res = DeviceController.toggleFlashlight(context, false)
            val reply = when (detected) {
                DetectedLanguage.TELUGU -> "ఫ్లాష్‌లైట్ ఆఫ్ చేశాను, బాస్."
                DetectedLanguage.HINDI -> "टॉर्च बंद कर दी गई है, बॉस।"
                else -> res
            }
            return ProcessResult(true, reply, true)
        }

        // 2. Volume Controls
        if (clean.contains("volume up") || clean.contains("increase volume") ||
            clean.contains("sound penchu") || clean.contains("aawaz badhao")
        ) {
            val res = DeviceController.adjustVolume(context, 1)
            val reply = when (detected) {
                DetectedLanguage.TELUGU -> "వాల్యూమ్ పెంచాను, బాస్."
                DetectedLanguage.HINDI -> "वॉल्यूम बढ़ा दिया गया है, बॉस।"
                else -> res
            }
            return ProcessResult(true, reply, true)
        }

        if (clean.contains("volume down") || clean.contains("decrease volume") ||
            clean.contains("sound thagginchu") || clean.contains("aawaz kam karo")
        ) {
            val res = DeviceController.adjustVolume(context, -1)
            val reply = when (detected) {
                DetectedLanguage.TELUGU -> "వాల్యూమ్ తగ్గించాను, బాస్."
                DetectedLanguage.HINDI -> "वॉल्यूम कम कर दिया गया है, बॉस।"
                else -> res
            }
            return ProcessResult(true, reply, true)
        }

        if (clean.contains("mute") || clean.contains("silent") || clean.contains("shant")) {
            val res = DeviceController.adjustVolume(context, 0)
            return ProcessResult(true, res, true)
        }

        // 3. Battery Telemetry
        if (clean.contains("battery") || clean.contains("charge") ||
            clean.contains("చార్జింగ్") || clean.contains("charging")
        ) {
            val res = DeviceController.getBatteryInfo(context)
            return ProcessResult(true, res, true)
        }

        // 4. Media Controls
        if (clean.contains("pause music") || clean.contains("stop music") || clean.contains("paata aapu")) {
            val res = DeviceController.controlMedia(context, "pause")
            return ProcessResult(true, res, true)
        }
        if (clean.contains("play music") || clean.contains("resume music") || clean.contains("paata paadu")) {
            val res = DeviceController.controlMedia(context, "play")
            return ProcessResult(true, res, true)
        }
        if (clean.contains("next song") || clean.contains("next track")) {
            val res = DeviceController.controlMedia(context, "next")
            return ProcessResult(true, res, true)
        }

        // 5. Screenshot & Lock
        if (clean.contains("screenshot") || clean.contains("screen shot")) {
            val res = DeviceController.takeScreenshot()
            return ProcessResult(true, res, true)
        }
        if (clean.contains("lock screen") || clean.contains("lock phone") || clean.contains("screen lock")) {
            val res = DeviceController.lockScreen()
            return ProcessResult(true, res, true)
        }

        // 6. Network & Wi-Fi / Bluetooth panels
        if (clean.contains("wifi") || clean.contains("wi-fi")) {
            val res = DeviceController.openWifiSettings(context)
            return ProcessResult(true, res, true)
        }
        if (clean.contains("bluetooth")) {
            val res = DeviceController.openBluetoothSettings(context)
            return ProcessResult(true, res, true)
        }
        if (clean.contains("hotspot")) {
            val res = DeviceController.openHotspotSettings(context)
            return ProcessResult(true, res, true)
        }

        // 7. App Launching ("open <app>")
        if (clean.startsWith("open ") || clean.startsWith("launch ") || clean.contains("kholo") || clean.contains("open cheyi")) {
            val targetApp = clean.replace("open ", "")
                .replace("launch ", "")
                .replace("kholo", "")
                .replace("open cheyi", "")
                .trim()
            if (targetApp.isNotEmpty()) {
                val match = AppLauncher.findApp(context, targetApp)
                if (match != null) {
                    val launched = AppLauncher.launch(context, match.second)
                    if (launched) {
                        val reply = when (detected) {
                            DetectedLanguage.TELUGU -> "${match.first} ఓపెన్ చేస్తున్నాను, బాస్."
                            DetectedLanguage.HINDI -> "${match.first} खोल रहा हूँ, बॉस।"
                            else -> "Opening ${match.first} now, Boss."
                        }
                        return ProcessResult(true, reply, true)
                    }
                }
            }
        }

        // 8. Calling ("call <name>")
        if (clean.startsWith("call ") || clean.contains("call cheyi") || clean.contains("ko phone lagao")) {
            val targetName = clean.replace("call ", "")
                .replace("call cheyi", "")
                .replace("ko phone lagao", "")
                .trim()
            if (targetName.isNotEmpty()) {
                val lookup = ContactResolver.lookup(context, targetName)
                when (lookup) {
                    is ContactLookupResult.Found -> {
                        CommIntents.call(context, lookup.phoneNumber)
                        val reply = "Calling ${lookup.name} for you, Boss."
                        return ProcessResult(true, reply, true)
                    }
                    is ContactLookupResult.NoPermission -> {
                        return ProcessResult(true, "I need contacts access to search contacts by name, Boss.", false)
                    }
                    is ContactLookupResult.NotFound -> {
                        return ProcessResult(true, "Could not locate contact '$targetName' in address book, Boss.", false)
                    }
                }
            }
        }

        // 9. Conversational Jarvis Greetings & Inquiries
        if (clean.contains("who are you") || clean.contains("nuvvu evaru") || clean.contains("kaun ho")) {
            val reply = when (detected) {
                DetectedLanguage.TELUGU -> "నేను జార్విస్, మీ పర్సనల్ ఏఐ అసిస్టెంట్ ని బాస్. మీకు ఎలా సహాయపడమంటారు?"
                DetectedLanguage.HINDI -> "मैं जार्विस हूँ, आपका पर्सनल एआई असिस्टेंट। हुक्म कीजिए बॉस।"
                else -> "I am Jarvis, your personal AI assistant. Always online and at your service, Boss."
            }
            return ProcessResult(true, reply, false)
        }

        if (clean.contains("how are you") || clean.contains("ela unnaru") || clean.contains("ela unnava") || clean.contains("kaise ho")) {
            val reply = when (detected) {
                DetectedLanguage.TELUGU -> "నేను అద్భుతంగా ఉన్నాను బాస్! సిస్టమ్స్ అన్నీ నార్మల్ గా పనిచేస్తున్నాయి. మీకేం కావాలి?"
                DetectedLanguage.HINDI -> "मैं बिल्कुल तैयार और सक्रिय हूँ बॉस। सभी सिस्टम्स सुचारू रूप से कार्य कर रहे हैं।"
                else -> "All systems running at optimal peak efficiency, Boss. How may I assist you today?"
            }
            return ProcessResult(true, reply, false)
        }

        if (clean.contains("jarvis") || clean.contains("hey jarvis") || clean.contains("hello") || clean.contains("hi") || clean.contains("namaste")) {
            val reply = when (detected) {
                DetectedLanguage.TELUGU -> "చెప్పండి బాస్, నేను సిద్ధంగా ఉన్నాను."
                DetectedLanguage.HINDI -> "जी बॉस, कहिए। मैं आपकी सेवा में हाज़िर हूँ।"
                else -> "Yes Boss, I am listening. What are your orders?"
            }
            return ProcessResult(true, reply, false)
        }

        return ProcessResult(false, "", false)
    }
}
