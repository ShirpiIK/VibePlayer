package com.vibeplayer.app.ui.lyrics

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibeplayer.app.lyrics.LyricsState
import kotlinx.coroutines.launch

@Composable
fun LyricsView(
    lyricsState: LyricsState,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    accentColor: Color = Color.White
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentIndex) {
        if (lyricsState is LyricsState.Synced && currentIndex >= 0 && currentIndex < lyricsState.lines.size) {
            coroutineScope.launch {
                listState.animateScrollToItem(
                    index = currentIndex,
                    scrollOffset = -300
                )
            }
        }
    }

    Box(modifier = modifier) {
        when (lyricsState) {
            is LyricsState.Loading -> {
                CircularProgressIndicator(
                    color = accentColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is LyricsState.NotFound -> {
                Text(
                    text = "Lyrics not found",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is LyricsState.Plain -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text(
                            text = lyricsState.text,
                            color = Color.White,
                            fontSize = 18.sp,
                            lineHeight = 28.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            is LyricsState.Synced -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 120.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    itemsIndexed(lyricsState.lines) { index, line ->
                        val isCurrent = index == currentIndex
                        
                        val alpha by animateFloatAsState(
                            targetValue = if (isCurrent) 1f else 0.4f,
                            animationSpec = tween(300),
                            label = "alpha"
                        )
                        
                        val scale by animateFloatAsState(
                            targetValue = if (isCurrent) 1.1f else 1f,
                            animationSpec = tween(300, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)),
                            label = "scale"
                        )

                        Text(
                            text = line.text,
                            color = if (isCurrent) accentColor else Color.White.copy(alpha = alpha),
                            fontSize = 24.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale
                                )
                        )
                    }
                }
            }
        }
    }
}
