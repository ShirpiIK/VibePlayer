package com.vibeplayer.app.lyrics

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parsed LRC line with timestamp in milliseconds.
 */
data class LyricLine(
    val timeMs: Long,
    val text: String
)

@Singleton
class LrcParser @Inject constructor() {

    private val timestampRegex = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]\\s*(.*)")

    fun parse(lrcText: String): List<LyricLine> {
        if (lrcText.isBlank()) return emptyList()

        return lrcText
            .lineSequence()
            .mapNotNull { rawLine ->
                val match = timestampRegex.find(rawLine.trim()) ?: return@mapNotNull null

                val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                val seconds = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
                val fractionRaw = match.groupValues[3]
                val text = match.groupValues[4]

                val millisFromFraction = when (fractionRaw.length) {
                    0 -> 0L
                    1 -> fractionRaw.toLong() * 100L
                    2 -> fractionRaw.toLong() * 10L
                    else -> fractionRaw.take(3).toLong()
                }

                LyricLine(
                    timeMs = (minutes * 60_000L) + (seconds * 1_000L) + millisFromFraction,
                    text = text
                )
            }
            .sortedBy { it.timeMs }
            .toList()
    }

    fun getCurrentIndex(lines: List<LyricLine>, currentPositionMs: Long): Int {
        if (lines.isEmpty() || currentPositionMs < 0) return -1

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
}
