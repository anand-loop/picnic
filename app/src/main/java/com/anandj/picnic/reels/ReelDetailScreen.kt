// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.anandj.picnic.reels

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anandj.picnic.R
import com.anandj.picnic.post.shareFile
import com.anandj.picnic.ui.theme.Black
import com.anandj.picnic.ui.theme.White
import java.io.File

@Composable
fun ReelDetailScreen(
    viewModel: ReelDetailViewModel,
    onNavigateBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ReelDetailContract.Effect.NavigateBack -> onNavigateBack()
            }
        }
    }

    BackHandler { viewModel.sendAction(ReelDetailContract.Action.NavigateBack) }

    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(Black)) {
        val reel = state.reel
        if (reel == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            // Thumbnail acts as the shared-element target — animates from the grid cell.
            // The video player overlays it once prepared.
            with(sharedTransitionScope) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(reel.uri))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedElement(
                            state = rememberSharedContentState(key = "reel-${reel.uri}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                )
            }
            ReelVideoPlayer(
                uri = reel.uri,
                initialPositionMs = viewModel.playbackPositionMs,
                onPositionChanged = { viewModel.playbackPositionMs = it },
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.sendAction(ReelDetailContract.Action.NavigateBack) }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = White
                )
            }
            IconButton(
                onClick = {
                    val uri = reel?.uri ?: return@IconButton
                    shareFile(context = context, path = uri, mimeType = "video/*")
                },
                enabled = reel != null
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = stringResource(R.string.action_share),
                    tint = White
                )
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun ReelVideoPlayer(
    uri: String,
    initialPositionMs: Long,
    onPositionChanged: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(uri))))
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            seekTo(initialPositionMs)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            onPositionChanged(exoPlayer.currentPosition)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { this.player = exoPlayer } },
        modifier = modifier
    )
}
