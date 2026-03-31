package com.vibeplayer.app.lyrics

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents a single line of synced lyrics
 */
data class LyricLine(
    val timestampMs: Long,   // Timestamp in milliseconds
    val text: String         // Lyric text for this line
)

/**
 * Parses .lrc format files into LyricLine list
 *
 * LRC format example:
 * [00:12.34] First line of lyrics
 * [00:18.50] Second line of lyrics
 */
@Singleton
class LrcParser @Inject constructor() {

    private val lrcLineRegex = Regex("""^\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)$""")
    private val metadataRegex = Regex("""^\[(ar|ti|al|by|offset):.*\]$""")

    /**
     * Parse a full LRC string into sorted list of LyricLines
     */
    fun parse(lrcContent: String): List<LyricLine> {
        if (lrcContent.isBlank()) return emptyList()

        return lrcContent.lines()
            .asSequence()
            .filter { it.isNotBlank() }
            .filter { !metadataRegex.matches(it.trim()) }
            .flatMap { line -> parseLine(line.trim()) }
            .filter { it.text.isNotBlank() }
            .sortedBy { it.timestampMs }
            .toList()
    }

    /**
     * A single line can have multiple timestamps: [00:12.34][01:05.23]Same lyrics
     */
    private fun parseLine(line: String): List<LyricLine> {
        val results = mutableListOf<LyricLine>()
        var remaining = line

        while (remaining.startsWith("[")) {
            val endBracket = remaining.indexOf("]")
            if (endBracket == -1) break

            val tag = remaining.substring(1, endBracket)
            val afterTag = remaining.substring(endBracket + 1)

            val timeMs = parseTimestamp(tag)
            if (timeMs != null) {
                // If what follows is another timestamp tag, text is empty for now
                val text = if (afterTag.startsWith("[")) "" else afterTag.trim()
                results.add(LyricLine(timeMs, text))
            }

            remaining = afterTag
            if (!remaining.startsWith("[")) {
                // Update all results that have empty text with the trailing text
                val text = remaining.trim()
                if (text.isNotBlank()) {
                    results.replaceAll { if (it.text.isEmpty()) it.copy(text = text) else it }
                }
                break
            }
        }

        return results
    }

    private fun parseTimestamp(tag: String): Long? {
        val match = Regex("""^(\d{2}):(\d{2})\.(\d{2,3})$""").matchEntire(tag) ?: return null
        val minutes = match.groupValues[1].toLongOrNull() ?: return null
        val seconds = match.groupValues[2].toLongOrNull() ?: return null
        val millis = match.groupValues[3].let {
            // Normalize 2-digit or 3-digit centiseconds
            if (it.length == 2) it.toLong() * 10 else it.toLong()
        }
        return (minutes * 60 * 1000) + (seconds * 1000) + millis
    }

    /**
     * Find the current active lyric index based on playback position
     */
    fun getCurrentIndex(lyrics: List<LyricLine>, positionMs: Long): Int {
        if (lyrics.isEmpty()) return -1

        var activeIndex = 0
        for (i in lyrics.indices) {
            if (lyrics[i].timestampMs <= positionMs) {
                activeIndex = i
            } else {
                break
            }
        }
        return activeIndex
    }
}
