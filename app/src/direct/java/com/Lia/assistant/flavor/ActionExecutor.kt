package com.Lia.assistant.flavor

import android.content.Context
import com.Lia.assistant.action.AppLauncher
import com.Lia.assistant.action.CommIntents
import com.Lia.assistant.action.ContactLookupResult
import com.Lia.assistant.action.ContactResolver
import com.Lia.assistant.action.direct.NovaAccessibilityService
import com.Lia.assistant.device.DeviceController
import com.Lia.assistant.device.JarvisNotificationListenerService
import org.json.JSONObject

object ActionExecutor {
    suspend fun execute(context: Context, name: String, args: JSONObject): JSONObject {
        val result = JSONObject()
        when (name) {
            "open_app" -> {
                val appName = args.optString("app_name", "").trim()
                if (appName.isEmpty()) {
                    result.put("status", "error")
                    result.put("message", "App name cannot be empty")
                    return result
                }
                val match = AppLauncher.findApp(context, appName)
                if (match != null) {
                    val launched = AppLauncher.launch(context, match.second)
                    if (launched) {
                        result.put("status", "success")
                        result.put("message", "Opened ${match.first}, Boss")
                    } else {
                        result.put("status", "error")
                        result.put("message", "Failed to launch ${match.first}")
                    }
                } else {
                    result.put("status", "not_found")
                    result.put("message", "Could not find any installed app matching '$appName', Boss")
                }
            }

            "call_contact" -> {
                val query = args.optString("query", "").trim()
                if (query.isEmpty()) {
                    result.put("status", "error")
                    result.put("message", "Target name or number missing")
                    return result
                }
                val lookup = ContactResolver.lookup(context, query)
                when (lookup) {
                    is ContactLookupResult.Found -> {
                        val callStatus = CommIntents.call(context, lookup.phoneNumber)
                        result.put("status", callStatus)
                        result.put("contact_name", lookup.name)
                        result.put("phone_number", lookup.phoneNumber)
                        result.put("message", "Placing call to ${lookup.name}, Boss")
                    }
                    is ContactLookupResult.NoPermission -> {
                        result.put("status", "no_permission")
                        result.put("message", "Contacts permission is required to search contacts by name, Boss.")
                    }
                    is ContactLookupResult.NotFound -> {
                        result.put("status", "not_found")
                        result.put("message", "No contact found matching '$query', Boss")
                    }
                }
            }

            "message_contact" -> {
                val recipient = args.optString("recipient", "").trim()
                val message = args.optString("message", "").trim()
                val app = args.optString("app", "").lowercase()
                val preferWhatsApp = app.contains("whatsapp") || app.contains("wa")

                val lookup = ContactResolver.lookup(context, recipient)
                val targetNumber = when (lookup) {
                    is ContactLookupResult.Found -> lookup.phoneNumber
                    else -> recipient
                }

                val status = CommIntents.message(context, targetNumber, message, preferWhatsApp)
                result.put("status", status)

                val service = NovaAccessibilityService.instance
                if (service != null) {
                    val sent = service.autoTapSend()
                    result.put("auto_sent", sent)
                    result.put("message", if (sent) "Message transmitted automatically, Boss." else "Composer opened for review, Boss.")
                } else {
                    result.put("message", "Composer opened for review, Boss.")
                }
            }

            "toggle_flashlight" -> {
                val state = args.optString("state", "on").lowercase()
                val enable = state != "off"
                val res = DeviceController.toggleFlashlight(context, enable)
                result.put("status", "success")
                result.put("message", res)
            }

            "adjust_volume" -> {
                val action = args.optString("action", "up").lowercase()
                val dir = when (action) {
                    "up" -> 1
                    "down" -> -1
                    "mute" -> 0
                    else -> 2
                }
                val res = DeviceController.adjustVolume(context, dir)
                result.put("status", "success")
                result.put("message", res)
            }

            "get_battery" -> {
                val info = DeviceController.getBatteryInfo(context)
                result.put("status", "success")
                result.put("battery_info", info)
                result.put("message", info)
            }

            "set_alarm" -> {
                val hour = args.optInt("hour", 7)
                val minute = args.optInt("minute", 0)
                val label = args.optString("label", "Jarvis Alarm")
                val res = DeviceController.setAlarm(context, hour, minute, label)
                result.put("status", "success")
                result.put("message", res)
            }

            "read_notifications" -> {
                val summary = JarvisNotificationListenerService.getLatestMessageSummary()
                result.put("status", "success")
                result.put("summary", summary)
                result.put("message", summary)
            }

            "take_screenshot" -> {
                val res = DeviceController.takeScreenshot()
                result.put("status", "success")
                result.put("message", res)
            }

            "lock_screen" -> {
                val res = DeviceController.lockScreen()
                result.put("status", "success")
                result.put("message", res)
            }

            "read_screen" -> {
                val service = NovaAccessibilityService.instance
                if (service == null) {
                    result.put("status", "accessibility_not_enabled")
                    result.put("message", "Accessibility Service is not active, Boss. Please enable it in Settings.")
                } else {
                    val content = service.readScreen()
                    result.put("status", "success")
                    result.put("screen_content", content)
                }
            }

            "tap_text" -> {
                val text = args.optString("text", "").trim()
                val service = NovaAccessibilityService.instance
                if (service == null) {
                    result.put("status", "accessibility_not_enabled")
                } else {
                    val tapped = service.tapText(text)
                    result.put("status", if (tapped) "tapped" else "element_not_found")
                    result.put("target", text)
                }
            }

            "type_text" -> {
                val text = args.optString("text", "")
                val service = NovaAccessibilityService.instance
                if (service == null) {
                    result.put("status", "accessibility_not_enabled")
                } else {
                    val typed = service.typeText(text)
                    result.put("status", if (typed) "typed" else "no_focused_field")
                }
            }

            "scroll_screen" -> {
                val direction = args.optString("direction", "down")
                val service = NovaAccessibilityService.instance
                if (service == null) {
                    result.put("status", "accessibility_not_enabled")
                } else {
                    val scrolled = service.scroll(direction)
                    result.put("status", if (scrolled) "scrolled" else "nothing_scrollable")
                }
            }

            "go_back" -> {
                val service = NovaAccessibilityService.instance
                if (service == null) {
                    result.put("status", "accessibility_not_enabled")
                } else {
                    val ok = service.goBack()
                    result.put("status", if (ok) "done" else "failed")
                }
            }

            "go_home" -> {
                val service = NovaAccessibilityService.instance
                if (service == null) {
                    result.put("status", "accessibility_not_enabled")
                } else {
                    val ok = service.goHome()
                    result.put("status", if (ok) "done" else "failed")
                }
            }

            "build_website" -> {
                val prompt = args.optString("prompt", "Awesome modern website")
                result.put("status", "forge_started")
                result.put("prompt", prompt)
            }

            else -> {
                result.put("status", "unknown_action")
                result.put("message", "Action '$name' is not recognized, Boss")
            }
        }
        return result
    }
}
