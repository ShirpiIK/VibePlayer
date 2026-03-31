package com.vibeplayer.app.lyrics

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

// ─── LrcLib API Models ────────────────────────────────────────────────────────

data class LrcLibResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("trackName") val trackName: String,
    @SerializedName("artistName") val artistName: String,
    @SerializedName("albumName") val albumName: String?,
    @SerializedName("duration") val duration: Double?,
    @SerializedName("syncedLyrics") val syncedLyrics: String?,   // LRC format
    @SerializedName("plainLyrics") val plainLyrics: String?       // Plain text fallback
)

// ─── Retrofit API Interface ───────────────────────────────────────────────────

interface LrcLibApi {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String? = null,
        @Query("duration") duration: Int? = null
    ): LrcLibResponse?

    @GET("api/search")
    suspend fun searchLyrics(
        @Query("q") query: String
    ): List<LrcLibResponse>
}

// ─── Lyrics State ─────────────────────────────────────────────────────────────

sealed class LyricsState {
    object Loading : LyricsState()
    object NotFound : LyricsState()
    data class Synced(val lines: List<LyricLine>) : LyricsState()
    data class Plain(val text: String) : LyricsState()
}

// ─── Repository ───────────────────────────────────────────────────────────────

@Singleton
class LyricsRepository @Inject constructor(
    private val api: LrcLibApi,
    private val parser: LrcParser,
    private val dao: LyricsCacheDao
) {

    /**
     * Fetch lyrics for a song. Checks local cache first, then LrcLib API.
     */
    suspend fun getLyrics(
        trackName: String,
        artistName: String,
        albumName: String? = null,
        durationSec: Int? = null
    ): LyricsState {
        val cacheKey = "${artistName}_${trackName}".lowercase().replace(" ", "_")

        // 1. Check Room cache
        val cached = dao.getLyrics(cacheKey)
        if (cached != null) {
            return if (cached.syncedLyrics != null)
                LyricsState.Synced(parser.parse(cached.syncedLyrics))
            else if (cached.plainLyrics != null)
                LyricsState.Plain(cached.plainLyrics)
            else
                LyricsState.NotFound
        }

        // 2. Fetch from LrcLib
        return try {
            val response = api.getLyrics(
                trackName = trackName,
                artistName = artistName,
                albumName = albumName,
                duration = durationSec
            )

            if (response == null) {
                // Try search fallback
                val results = api.searchLyrics("$artistName $trackName")
                val best = results.firstOrNull()
                handleResponse(best, cacheKey)
            } else {
                handleResponse(response, cacheKey)
            }
        } catch (e: Exception) {
            LyricsState.NotFound
        }
    }

    private suspend fun handleResponse(
        response: LrcLibResponse?,
        cacheKey: String
    ): LyricsState {
        if (response == null) {
            dao.insert(LyricsCacheEntity(cacheKey, null, null))
            return LyricsState.NotFound
        }

        // Save to cache
        dao.insert(LyricsCacheEntity(
            key = cacheKey,
            syncedLyrics = response.syncedLyrics,
            plainLyrics = response.plainLyrics
        ))

        return when {
            response.syncedLyrics != null ->
                LyricsState.Synced(parser.parse(response.syncedLyrics))
            response.plainLyrics != null ->
                LyricsState.Plain(response.plainLyrics)
            else -> LyricsState.NotFound
        }
    }
}
