package com.vibeplayer.app.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.vibeplayer.app.library.Song
import com.vibeplayer.app.library.SongScanner
import com.vibeplayer.app.lyrics.LrcParser
import com.vibeplayer.app.lyrics.LyricLine
import com.vibeplayer.app.lyrics.LyricsRepository
import com.vibeplayer.app.lyrics.LyricsState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songScanner: SongScanner,
    private val lyricsRepository: LyricsRepository,
    private val lrcParser: LrcParser
) : ViewModel() {

    // ── Playback State ──────────────────────────────────────────────────────
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    // ── Song Library ────────────────────────────────────────────────────────
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    // ── Lyrics ──────────────────────────────────────────────────────────────
    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Loading)
    val lyricsState: StateFlow<LyricsState> = _lyricsState

    private val _currentLyricIndex = MutableStateFlow(-1)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex

    // ── MediaController ─────────────────────────────────────────────────────
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    init {
        loadSongs()
        connectToService()
        startPositionTracking()
    }

    private fun loadSongs() {
        viewModelScope.launch {
            _songs.value = songScanner.scanSongs()
        }
    }

    private fun connectToService() {
        val token = SessionToken(
            context,
            ComponentName(context, MusicPlayerService::class.java)
        )
        controllerFuture = MediaController.Builder(context, token).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            setupPlayerListener()
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                val song = _songs.value.find {
                    it.title == metadata.title?.toString()
                }
                if (song != null && song != _currentSong.value) {
                    _currentSong.value = song
                    fetchLyricsForSong(song)
                }
            }
        })
    }

    // ── Position Tracking (every 100ms for smooth lyrics sync) ──────────────
    private fun startPositionTracking() {
        viewModelScope.launch {
            while (true) {
                delay(100)
                controller?.let { ctrl ->
                    _positionMs.value = ctrl.currentPosition
                    _durationMs.value = ctrl.duration.coerceAtLeast(0)

                    // Update active lyric line
                    val state = _lyricsState.value
                    if (state is LyricsState.Synced) {
                        _currentLyricIndex.value = lrcParser.getCurrentIndex(
                            state.lines,
                            ctrl.currentPosition
                        )
                    }
                }
            }
        }
    }

    // ── Playback Controls ────────────────────────────────────────────────────

    fun playSong(song: Song) {
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(song.path))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.albumArtUri)
                    .build()
            )
            .build()

        controller?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
        }

        _currentSong.value = song
        fetchLyricsForSong(song)
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        val items = songs.map { song ->
            MediaItem.Builder()
                .setUri(Uri.parse(song.path))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.albumArtUri)
                        .build()
                ).build()
        }

        controller?.apply {
            setMediaItems(items, startIndex, 0)
            prepare()
            play()
        }
    }

    fun togglePlayPause() {
        controller?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun skipNext() = controller?.seekToNextMediaItem()
    fun skipPrevious() = controller?.seekToPreviousMediaItem()

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeatMode() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    // ── Lyrics ───────────────────────────────────────────────────────────────

    private fun fetchLyricsForSong(song: Song) {
        _lyricsState.value = LyricsState.Loading
        _currentLyricIndex.value = -1

        viewModelScope.launch {
            _lyricsState.value = lyricsRepository.getLyrics(
                trackName = song.title,
                artistName = song.artist,
                albumName = song.album,
                durationSec = (song.durationMs / 1000).toInt()
            )
        }
    }

    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
