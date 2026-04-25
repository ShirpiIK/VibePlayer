package com.vibeplayer.app.lyrics

import javax.inject.Inject
import javax.inject.Singleton

/** Parsed LRC line with timestamp in milliseconds. */
data class LyricLine(
    val timeMs: Long,
    val text: String
)

@Singleton
class LrcParser @Inject constructor() {

    private val timestampRegex = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val metadataRegex = Regex("^\\[(ar|ti|al|by|offset):", RegexOption.IGNORE_CASE)

    fun parse(lrcText: String): List<LyricLine> {
        if (lrcText.isBlank()) return emptyList()

        return lrcText
            .lineSequence()
            .flatMap { rawLine ->
                parseLine(rawLine).asSequence()
            }
            .sortedBy { it.timeMs }
            .toList()
    }

    fun getCurrentIndex(lines: List<LyricLine>, currentPositionMs: Long): Int {
        if (lines.isEmpty() || currentPositionMs < 0L) return -1

        var low = 0
        var high = lines.lastIndex
        var answer = -1

        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timeMs <= currentPositionMs) {
                answer = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        return answer
    }

    private fun parseLine(rawLine: String): List<LyricLine> {
        val line = rawLine.trim()
        if (line.isBlank() || metadataRegex.containsMatchIn(line)) return emptyList()

        val matches = timestampRegex.findAll(line).toList()
        if (matches.isEmpty()) return emptyList()

        val lyricText = timestampRegex.replace(line, "").trim()

        return matches.mapNotNull { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
            val fractionRaw = match.groupValues.getOrElse(3) { "" }

            LyricLine(
                timeMs = (minutes * 60_000L) + (seconds * 1_000L) + fractionToMillis(fractionRaw),
                text = lyricText
            )
        }
    }

    private fun fractionToMillis(fractionRaw: String): Long {
        if (fractionRaw.isBlank()) return 0L

        val digitsOnly = fractionRaw.take(3).padEnd(3, '0')
        return digitsOnly.toLongOrNull() ?: 0L
    }
}