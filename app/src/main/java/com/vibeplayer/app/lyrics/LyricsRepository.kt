package com.vibeplayer.app.lyrics

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

// ─── Models ───────────────────────────────────────────────────────────────────

data class LrcLibResponse(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("trackName") val trackName: String = "",
    @SerializedName("artistName") val artistName: String = "",
    @SerializedName("albumName") val albumName: String = "",
    @SerializedName("duration") val duration: Double = 0.0,
    @SerializedName("syncedLyrics") val syncedLyrics: String = "",
    @SerializedName("plainLyrics") val plainLyrics: String = ""
)

// ─── API — NO nullable return types (KSP requirement) ─────────────────────────

interface LrcLibApi {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String = "",
        @Query("duration") duration: Int = 0
    ): LrcLibResponse

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
    suspend fun getLyrics(
        trackName: String,
        artistName: String,
        albumName: String = "",
        durationSec: Int = 0
    ): LyricsState {
        val cacheKey = "${artistName}_${trackName}".lowercase().replace(" ", "_")

        // 1. Check Room cache
        val cached = dao.getLyrics(cacheKey)
        if (cached != null) {
            return when {
                cached.syncedLyrics.isNotBlank() ->
                    LyricsState.Synced(parser.parse(cached.syncedLyrics))
                cached.plainLyrics.isNotBlank() ->
                    LyricsState.Plain(cached.plainLyrics)
                else -> LyricsState.NotFound
            }
        }

        // 2. Fetch from LrcLib
        return try {
            val response = api.getLyrics(trackName, artistName, albumName, durationSec)
            handleResponse(response, cacheKey)
        } catch (e: Exception) {
            // Try search fallback
            try {
                val results = api.searchLyrics("$artistName $trackName")
                if (results.isNotEmpty()) handleResponse(results.first(), cacheKey)
                else LyricsState.NotFound
            } catch (e2: Exception) {
                LyricsState.NotFound
            }
        }
    }

    private suspend fun handleResponse(
        response: LrcLibResponse,
        cacheKey: String
    ): LyricsState {
        dao.insert(LyricsCacheEntity(
            key = cacheKey,
            syncedLyrics = response.syncedLyrics,
            plainLyrics = response.plainLyrics
        ))
        return when {
            response.syncedLyrics.isNotBlank() ->
                LyricsState.Synced(parser.parse(response.syncedLyrics))
            response.plainLyrics.isNotBlank() ->
                LyricsState.Plain(response.plainLyrics)
            else -> LyricsState.NotFound
        }
    }
}
