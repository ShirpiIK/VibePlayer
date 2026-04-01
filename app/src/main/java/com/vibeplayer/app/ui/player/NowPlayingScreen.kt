package com.vibeplayer.app.ui.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.vibeplayer.app.library.Song
import com.vibeplayer.app.player.MusicViewModel
import com.vibeplayer.app.ui.lyrics.LyricsView
import com.vibeplayer.app.lyrics.LyricsState

// ─────────────────────────────────────────────────────────────────────────────
//  Now Playing Screen
//  Two modes: Album Art view | Lyrics view (tap album art to toggle)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NowPlayingScreen(
    onDismiss: () -> Unit,
    viewModel: MusicViewModel = hiltViewModel()
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val lyricsState by viewModel.lyricsState.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()

    // Toggle between album art and lyrics view
    var showLyrics by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Blurred album art background ──────────────────────────────────
        AsyncImage(
            model = currentSong?.albumArtUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 60.dp)
                .graphicsLayer { scaleX = 1.3f; scaleY = 1.3f } // Prevent blur edge artifacts
        )

        // Dark scrim overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )

        // Bottom gradient (for controls readability)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.6f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // ── Main content ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Handle bar
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
                    .clickable { onDismiss() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Album art OR Lyrics (tappable toggle) ─────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = showLyrics,
                    transitionSpec = {
                        fadeIn(tween(400)) + slideInVertically { -30 } togetherWith
                        fadeOut(tween(300)) + slideOutVertically { 30 }
                    },
                    label = "albumArtLyricsSwitch"
                ) { inLyricsMode ->
                    if (inLyricsMode) {
                        LyricsView(
                            lyricsState = lyricsState,
                            currentIndex = currentLyricIndex,
                            modifier = Modifier.fillMaxSize(),
                            accentColor = Color.White
                        )
                    } else {
                        AlbumArtCard(
                            song = currentSong,
                            isPlaying = isPlaying,
                            onClick = {
                                // Only switch to lyrics if lyrics are available
                                if (lyricsState is LyricsState.Synced ||
                                    lyricsState is LyricsState.Plain) {
                                    showLyrics = true
                                }
                            }
                        )
                    }
                }

                // "Back to album art" button (shown only in lyrics mode)
                if (showLyrics) {
                    IconButton(
                        onClick = { showLyrics = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = "Show album art",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Song info ─────────────────────────────────────────────────
            SongInfo(song = currentSong)

            Spacer(modifier = Modifier.height(20.dp))

            // ── Progress bar ──────────────────────────────────────────────
            ProgressBar(
                positionMs = positionMs,
                durationMs = durationMs,
                onSeek = { viewModel.seekTo(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Controls ──────────────────────────────────────────────────
            PlaybackControls(
                isPlaying = isPlaying,
                onPlayPause = { viewModel.togglePlayPause() },
                onSkipNext = { viewModel.skipNext() },
                onSkipPrevious = { viewModel.skipPrevious() },
                onShuffle = { viewModel.toggleShuffle() },
                onRepeat = { viewModel.cycleRepeatMode() }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─── Album Art Card with breathing animation ──────────────────────────────────

@Composable
fun AlbumArtCard(
    song: Song?,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "albumScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .padding(16.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() }
        ) {
            AsyncImage(
                model = song?.albumArtUri,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Lyrics hint if available
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("Lyrics", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

// ─── Song info ────────────────────────────────────────────────────────────────

@Composable
fun SongInfo(song: Song?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song?.title ?: "No song playing",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song?.artist ?: "Unknown Artist",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = { /* TODO: Add to favorites */ }) {
            Icon(
                Icons.Default.FavoriteBorder,
                contentDescription = "Favourite",
                tint = Color.White.copy(alpha = 0.65f)
            )
        }
    }
}

// ─── Progress bar ─────────────────────────────────────────────────────────────

@Composable
fun ProgressBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit
) {
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f

    Column {
        Slider(
            value = progress,
            onValueChange = { onSeek((it * durationMs).toLong()) },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = positionMs.toTimeString(),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = durationMs.toTimeString(),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

// ─── Playback controls ────────────────────────────────────────────────────────

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onShuffle) {
            Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle",
                tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(22.dp))
        }

        IconButton(onClick = onSkipPrevious, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous",
                tint = Color.White, modifier = Modifier.size(36.dp))
        }

        // Play/Pause button — larger
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(68.dp)
                .background(Color.White, CircleShape)
                .clickable { onPlayPause() }
        ) {
            val icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow
            Icon(
                icon,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.Black,
                modifier = Modifier.size(38.dp)
            )
        }

        IconButton(onClick = onSkipNext, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Rounded.SkipNext, contentDescription = "Next",
                tint = Color.White, modifier = Modifier.size(36.dp))
        }

        IconButton(onClick = onRepeat) {
            Icon(Icons.Rounded.Repeat, contentDescription = "Repeat",
                tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(22.dp))
        }
    }
}

// ─── Time formatter ───────────────────────────────────────────────────────────

private fun Long.toTimeString(): String {
    val totalSecs = this / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return "%d:%02d".format(mins, secs)
}
