// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.stories

import android.net.Uri
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anandj.picnic.R
import com.anandj.picnic.ui.theme.Black
import com.anandj.picnic.ui.theme.White
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
private const val IMAGE_DURATION_MS = 5000L
private const val REWIND_THRESHOLD = 0.1f

@Composable
fun StoryPlayerScreen(
    viewModel: StoryPlayerViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                StoryPlayerContract.Effect.NavigateBack -> onNavigateBack()
            }
        }
    }

    BackHandler { viewModel.sendAction(StoryPlayerContract.Action.NavigateBack) }

    Box(modifier = Modifier.fillMaxSize().background(Black)) {
        val items = state.items
        if (items.isEmpty()) {
            CircularProgressIndicator(
                color = White,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        val currentIndex = state.currentIndex.coerceIn(0, items.lastIndex)
        val currentItem = items[currentIndex]
        val isVideo = currentItem.mediaType == "video" || isVideoUri(currentItem.uri)

        var currentProgress by remember(state.playToken) { mutableFloatStateOf(0f) }

        if (isVideo) {
            StoryVideoPlayer(
                uri = currentItem.uri,
                playToken = state.playToken,
                isPaused = state.isPaused,
                onProgressChange = { currentProgress = it },
                onEnded = { viewModel.sendAction(StoryPlayerContract.Action.Next) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            StoryImageView(
                uri = currentItem.uri,
                modifier = Modifier.fillMaxSize()
            )
            LaunchedEffect(state.playToken, state.isPaused) {
                if (!state.isPaused) {
                    val initialProgress = currentProgress
                    val initialFrameMs = withFrameMillis { it }
                    while (currentProgress < 1f) {
                        val nowMs = withFrameMillis { it }
                        val elapsedMs = nowMs - initialFrameMs
                        currentProgress = (initialProgress + elapsedMs.toFloat() / IMAGE_DURATION_MS).coerceAtMost(1f)
                    }
                    viewModel.sendAction(StoryPlayerContract.Action.Next)
                }
            }
        }

        val currentProgressRef = rememberUpdatedState(currentProgress)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentIndex) {
                    detectTapGestures(
                        onPress = {
                            viewModel.sendAction(StoryPlayerContract.Action.Pause)
                            try {
                                awaitRelease()
                            } finally {
                                viewModel.sendAction(StoryPlayerContract.Action.Resume)
                            }
                        },
                        onTap = { offset ->
                            val w = size.width.toFloat()
                            when {
                                offset.x < w / 3f -> {
                                    val action = if (currentProgressRef.value < REWIND_THRESHOLD) {
                                        StoryPlayerContract.Action.Previous
                                    } else {
                                        StoryPlayerContract.Action.Restart
                                    }
                                    viewModel.sendAction(action)
                                }
                                offset.x > w * 2f / 3f -> viewModel.sendAction(StoryPlayerContract.Action.Next)
                                else -> Unit
                            }
                        }
                    )
                }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            StoryProgressBar(
                itemCount = items.size,
                currentIndex = currentIndex,
                currentProgress = currentProgress
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = formatDate(currentItem.timestamp),
                    color = White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.sendAction(StoryPlayerContract.Action.NavigateBack) }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = White
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryImageView(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context).data(File(uri)).crossfade(true).build(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun StoryVideoPlayer(
    uri: String,
    playToken: Int,
    isPaused: Boolean,
    onProgressChange: (Float) -> Unit,
    onEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(uri))))
            prepare()
            playWhenReady = true
        }
    }

    val currentOnProgressChange by rememberUpdatedState(onProgressChange)
    val currentOnEnded by rememberUpdatedState(onEnded)

    LaunchedEffect(playToken, exoPlayer) {
        exoPlayer.seekTo(0)
        exoPlayer.playWhenReady = !isPaused
    }

    LaunchedEffect(isPaused, exoPlayer) {
        exoPlayer.playWhenReady = !isPaused
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) currentOnEnded()
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            withFrameMillis {
                val duration = exoPlayer.duration
                if (duration > 0) {
                    currentOnProgressChange((exoPlayer.currentPosition.toFloat() / duration).coerceIn(0f, 1f))
                }
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            LayoutInflater.from(ctx).inflate(R.layout.story_player_view, null) as PlayerView
        },
        update = { view -> view.player = exoPlayer },
        modifier = modifier
    )
}

private fun isVideoUri(uri: String): Boolean {
    val lower = uri.lowercase()
    return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".webm") || lower.endsWith(".mkv")
}

@Composable
private fun StoryProgressBar(
    itemCount: Int,
    currentIndex: Int,
    currentProgress: Float
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(itemCount) { index ->
            val segmentProgress = when {
                index < currentIndex -> 1f
                index == currentIndex -> currentProgress.coerceIn(0f, 1f)
                else -> 0f
            }
            LinearProgressIndicator(
                progress = { segmentProgress },
                color = White,
                trackColor = White.copy(alpha = 0.3f),
                strokeCap = StrokeCap.Butt,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
            )
        }
    }
}

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
private fun formatDate(epochSeconds: Long): String = remember(epochSeconds) {
    val zoneId = ZoneId.systemDefault()
    Instant.ofEpochSecond(epochSeconds).atZone(zoneId).toLocalDate().format(dateFormatter)
}
