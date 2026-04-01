package com.vibeplayer.app.ui.lyrics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.vibeplayer.app.lyrics.LyricLine
import com.vibeplayer.app.lyrics.LyricsState
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
//  Apple Music Style Lyrics Screen
//  - Active line: Large, bold, full white
//  - Past lines:  Slightly dimmed, smaller
//  - Future lines: Dim white, smaller
//  - Smooth auto-scroll to active line
//  - Scale + alpha transitions per line
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LyricsView(
    lyricsState: LyricsState,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    accentColor: Color = Color.White
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (lyricsState) {
            is LyricsState.Loading -> LyricsLoadingView()
            is LyricsState.NotFound -> LyricsNotFoundView()
            is LyricsState.Plain -> PlainLyricsView(lyricsState.text)
            is LyricsState.Synced -> SyncedLyricsView(
                lines = lyricsState.lines,
                currentIndex = currentIndex,
                accentColor = accentColor
            )
        }
    }
}

// ─── Synced Lyrics (Apple Music style) ───────────────────────────────────────

@Composable
fun SyncedLyricsView(
    lines: List<LyricLine>,
    currentIndex: Int,
    accentColor: Color
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to the active line with smooth animation
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && currentIndex < lines.size) {
            scope.launch {
                // Scroll so active line is roughly in the middle of screen
                val targetIndex = (currentIndex - 2).coerceAtLeast(0)
                listState.animateScrollToItem(
                    index = targetIndex,
                    scrollOffset = 0
                )
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(vertical = 160.dp) // Top/bottom breathing room
    ) {
        itemsIndexed(lines) { index, line ->
            LyricLineItem(
                text = line.text,
                state = when {
                    index == currentIndex -> LyricLineState.ACTIVE
                    index < currentIndex -> LyricLineState.PAST
                    else -> LyricLineState.FUTURE
                },
                accentColor = accentColor
            )
        }
    }
}

// ─── Individual Lyric Line with Apple Music animation ─────────────────────────

enum class LyricLineState { ACTIVE, PAST, FUTURE }

@Composable
fun LyricLineItem(
    text: String,
    state: LyricLineState,
    accentColor: Color
) {
    // Animate all properties smoothly
    val fontSize by animateFloatAsState(
        targetValue = when (state) {
            LyricLineState.ACTIVE -> 28f
            LyricLineState.PAST   -> 20f
            LyricLineState.FUTURE -> 20f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fontSize"
    )

    val alpha by animateFloatAsState(
        targetValue = when (state) {
            LyricLineState.ACTIVE -> 1.0f
            LyricLineState.PAST   -> 0.45f
            LyricLineState.FUTURE -> 0.30f
        },
        animationSpec = tween(durationMillis = 300, easing = EaseInOutCubic),
        label = "alpha"
    )

    val fontWeight = when (state) {
        LyricLineState.ACTIVE -> FontWeight.Bold
        else -> FontWeight.Medium
    }

    // Subtle left-indent on active line (Apple Music touch)
    val leftPadding by animateDpAsState(
        targetValue = if (state == LyricLineState.ACTIVE) 0.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "indent"
    )

    if (text.isNotBlank()) {
        Text(
            text = text,
            fontSize = fontSize.sp,
            fontWeight = fontWeight,
            color = accentColor,
            textAlign = TextAlign.Start,
            lineHeight = (fontSize * 1.35f).sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = leftPadding, bottom = 8.dp)
                .alpha(alpha)
        )
    } else {
        // Empty line = small spacer dot (like Apple Music)
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(alpha * 0.5f)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.extraSmall,
                color = accentColor.copy(alpha = 0.4f)
            ) {}
        }
    }
}

// ─── Plain Lyrics fallback ────────────────────────────────────────────────────

@Composable
fun PlainLyricsView(text: String) {
    val scrollState = rememberLazyListState()
    val lines = remember(text) { text.lines() }

    LazyColumn(
        state = scrollState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 60.dp)
    ) {
        items(lines) { line ->
            Text(
                text = line,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
    }
}

// ─── Loading / Not Found states ───────────────────────────────────────────────

@Composable
fun LyricsLoadingView() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        repeat(6) { i ->
            val lineAlpha = alpha * (1f - i * 0.12f).coerceAtLeast(0.1f)
            val width = listOf(0.9f, 0.75f, 0.85f, 0.65f, 0.8f, 0.5f)[i]
            Box(
                modifier = Modifier
                    .fillMaxWidth(width)
                    .height(20.dp)
                    .alpha(lineAlpha)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.small,
                    color = Color.White.copy(alpha = 0.2f)
                ) {}
            }
        }
    }
}

@Composable
fun LyricsNotFoundView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "♪",
            fontSize = 48.sp,
            color = Color.White.copy(alpha = 0.3f)
        )
        Text(
            text = "No lyrics found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.4f)
        )
        Text(
            text = "Try adding a .lrc file with the same\nname as your song",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.25f),
            textAlign = TextAlign.Center
        )
    }
}

// ─── Easing helpers ───────────────────────────────────────────────────────────

private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
private val EaseInOutSine  = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
