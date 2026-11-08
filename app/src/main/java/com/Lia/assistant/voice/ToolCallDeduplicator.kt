package com.Lia.assistant.voice

class ToolCallDeduplicator(private val maxSize: Int = 100) {
    private val seenIds = LinkedHashSet<String>()

    @Synchronized
    fun isDuplicateOrRecord(callId: String): Boolean {
        if (callId.isEmpty()) return false
        if (seenIds.contains(callId)) {
            return true
        }
        seenIds.add(callId)
        if (seenIds.size > maxSize) {
            val oldest = seenIds.iterator().next()
            seenIds.remove(oldest)
        }
        return false
    }

    @Synchronized
    fun clear() {
        seenIds.clear()
    }
}
