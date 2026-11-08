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
                description = "Compose and send a message via WhatsApp or SMS.",
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
                name = "read_screen",
                description = "Read visible elements and text on current screen via accessibility.",
                parameters = emptyMap()
            )
        )

        array.put(
            functionDecl(
                name = "tap_text",
                description = "Tap on a visible button or text on screen.",
                parameters = mapOf("text" to "Text or label to tap"),
                required = listOf("text")
            )
        )

        array.put(
            functionDecl(
                name = "type_text",
                description = "Type text into focused input field.",
                parameters = mapOf("text" to "Text to insert"),
                required = listOf("text")
            )
        )

        array.put(
            functionDecl(
                name = "scroll_screen",
                description = "Scroll screen content 'down' or 'up'.",
                parameters = mapOf("direction" to "Direction: 'down' or 'up'"),
                required = listOf("direction")
            )
        )

        array.put(
            functionDecl(
                name = "take_screenshot",
                description = "Capture an instant screenshot of current screen.",
                parameters = emptyMap()
            )
        )

        array.put(
            functionDecl(
                name = "lock_screen",
                description = "Lock the phone screen immediately.",
                parameters = emptyMap()
            )
        )

        array.put(
            functionDecl(
                name = "go_back",
                description = "Trigger system Back navigation action.",
                parameters = emptyMap()
            )
        )

        array.put(
            functionDecl(
                name = "go_home",
                description = "Trigger system Home navigation action.",
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
