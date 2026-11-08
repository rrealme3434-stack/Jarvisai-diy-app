package com.Lia.assistant.flavor

object LiaCapabilityPrompts {
    fun toolInstruction(assistantName: String): String {
        return "You have tools to open apps, place phone calls, draft messages, toggle device flashlight, adjust volume, check battery status, set alarms, and read notifications for Boss. " +
                "You can also build live websites in the 3D Forge upon Boss's command (e.g. 'build a website for a cafe'). " +
                "Screen automation and silent background message transmission are restricted in this edition."
    }
}
