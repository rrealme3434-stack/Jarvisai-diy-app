package com.Lia.assistant.flavor

object LiaCapabilityPrompts {
    fun toolInstruction(assistantName: String): String {
        return "You have full direct access tools to open apps, place phone calls, send messages, toggle flashlight, adjust volume, capture screenshots, lock the screen, check battery telemetry, and read notifications for Boss. " +
                "Via Accessibility, you can read on-screen UI elements, tap buttons, enter text, and scroll on behalf of Boss. " +
                "Always confirm before performing destructive or irreversible actions. You can also build interactive websites in the 3D Forge upon command."
    }
}
