package com.Lia.assistant.voice

enum class MemoryRole {
    USER, ASSISTANT
}

data class MemoryTurn(
    val role: MemoryRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

object ConversationMemory {
    private const val MAX_TURNS = 12
    private const val MAX_RECAP_CHARS = 1600

    private val turns = mutableListOf<MemoryTurn>()

    @Synchronized
    fun addTurn(role: MemoryRole, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        turns.add(MemoryTurn(role, clean))
        while (turns.size > MAX_TURNS) {
            turns.removeAt(0)
        }
    }

    @Synchronized
    fun getTurns(): List<MemoryTurn> {
        return turns.toList()
    }

    @Synchronized
    fun recap(): String? {
        if (turns.isEmpty()) return null
        val sb = StringBuilder()
        for (turn in turns) {
            val prefix = if (turn.role == MemoryRole.USER) "User: " else "You: "
            sb.append(prefix).append(turn.text).append("\n")
        }
        val full = sb.toString().trim()
        if (full.isEmpty()) return null
        return if (full.length > MAX_RECAP_CHARS) {
            full.takeLast(MAX_RECAP_CHARS)
        } else {
            full
        }
    }

    @Synchronized
    fun clear() {
        turns.clear()
    }
}
