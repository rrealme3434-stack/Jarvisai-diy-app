package com.Lia.assistant.flavor

import com.Lia.assistant.action.FunctionDeclHelper.functionDecl
import org.json.JSONArray

object LiaToolCatalog {
    fun declarations(): JSONArray {
        val array = JSONArray()

        array.put(
            functionDecl(
                name = "open_app",
                description = "Open an installed application on Boss's Android phone.",
                parameters = mapOf("app_name" to "Name of the app to launch (e.g. YouTube, WhatsApp, Camera)"),
                required = listOf("app_name")
            )
        )

        array.put(
            functionDecl(
                name = "call_contact",
                description = "Initiate a phone call to a contact or phone number.",
                parameters = mapOf("query" to "Contact name or phone number to call"),
                required = listOf("query")
            )
        )

        array.put(
            functionDecl(
                name = "message_contact",
                description = "Open the SMS or WhatsApp composer with a recipient and pre-filled message text.",
                parameters = mapOf(
                    "recipient" to "Contact name or phone number",
                    "message" to "Text content to send",
                    "app" to "Optional preferred platform: 'whatsapp' or 'sms'"
                ),
                required = listOf("recipient", "message")
            )
        )

        array.put(
            functionDecl(
                name = "toggle_flashlight",
                description = "Turn device flashlight/torch ON or OFF.",
                parameters = mapOf("state" to "Desired state: 'on' or 'off'"),
                required = listOf("state")
            )
        )

        array.put(
            functionDecl(
                name = "adjust_volume",
                description = "Adjust music and phone speaker volume: 'up', 'down', 'mute', or 'max'.",
                parameters = mapOf("action" to "Direction: 'up', 'down', 'mute', 'max'"),
                required = listOf("action")
            )
        )

        array.put(
            functionDecl(
                name = "get_battery",
                description = "Get battery charge level and charging telemetry status.",
                parameters = emptyMap()
            )
        )

        array.put(
            functionDecl(
                name = "set_alarm",
                description = "Set an alarm clock for Boss.",
                parameters = mapOf(
                    "hour" to "Hour (0-23)",
                    "minute" to "Minute (0-59)",
                    "label" to "Optional alarm label"
                ),
                required = listOf("hour", "minute")
            )
        )

        array.put(
            functionDecl(
                name = "read_notifications",
                description = "Read unread incoming notifications and messages.",
                parameters = emptyMap()
            )
        )

        array.put(
            functionDecl(
                name = "build_website",
                description = "Build and preview a modern, interactive website in the 3D Forge.",
                parameters = mapOf("prompt" to "Description of the website to generate"),
                required = listOf("prompt")
            )
        )

        return array
    }
}
