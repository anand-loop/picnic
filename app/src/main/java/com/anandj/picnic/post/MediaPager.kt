// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.anandj.picnic.post

import android.net.Uri
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.anandj.bento.Carousel
import com.anandj.picnic.data.db.entity.MediaEntity
import com.anandj.picnic.ui.theme.White
import java.io.File

// ─── Public entry point ──────────────────────────────────────────────────────

@Composable
internal fun MediaPager(
    media: List<MediaEntity>,
    modifier: Modifier = Modifier,
    pagerMaxHeight: Dp? = null,
    onEnterInspectMode: ((MediaEntity) -> Unit)? = null,
    onInspectZoom: ((Float) -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    inspectMediaUri: String? = null,
    navSharedTransitionScope: SharedTransitionScope? = null,
    navAnimatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    Carousel(
        items = media,
        modifier = modifier,
        maxHeight = pagerMaxHeight,
        indicatorActiveColor = White,
        indicatorInactiveColor = White.copy(alpha = 0.4f)
    ) { _, item, isActive ->
        MediaPageItem(
            item = item,
            isActive = isActive,
            sizeModifier = Modifier.fillMaxWidth(),
            onEnterInspectMode = onEnterInspectMode,
            onInspectZoom = onInspectZoom,
            sharedTransitionScope = sharedTransitionScope,
            inspectMediaUri = inspectMediaUri,
            navSharedTransitionScope = navSharedTransitionScope,
            navAnimatedVisibilityScope = navAnimatedVisibilityScope
        )
    }
}

// ─── Per-page dispatch ────────────────────────────────────────────────────────

@Composable
private fun MediaPageItem(
    item: MediaEntity,
    isActive: Boolean,
    sizeModifier: Modifier,
    onEnterInspectMode: ((MediaEntity) -> Unit)?,
    onInspectZoom: ((Float) -> Unit)?,
    sharedTransitionScope: SharedTransitionScope?,
    inspectMediaUri: String?,
    navSharedTransitionScope: SharedTransitionScope?,
    navAnimatedVisibilityScope: AnimatedVisibilityScope?
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        when {
            LocalInspectionMode.current -> {
                val ratio = if (item.mediaType == "video") 16f / 9f else 1f
                Box(modifier = sizeModifier.aspectRatio(ratio).background(Color(0xFF444444)))
            }
            item.mediaType == "video" -> {
                MediaVideoPlayer(
                    uri = item.uri,
                    isActive = isActive,
                    modifier = sizeModifier.aspectRatio(16f / 9f)
                )
            }
            else -> {
                MediaImagePage(
                    item = item,
                    sizeModifier = sizeModifier,
                    onEnterInspectMode = onEnterInspectMode,
                    onInspectZoom = onInspectZoom,
                    sharedTransitionScope = sharedTransitionScope,
                    inspectMediaUri = inspectMediaUri,
                    navSharedTransitionScope = navSharedTransitionScope,
                    navAnimatedVisibilityScope = navAnimatedVisibilityScope
                )
            }
        }
    }
}

// ─── Image page ───────────────────────────────────────────────────────────────

@Composable
private fun MediaImagePage(
    item: MediaEntity,
    sizeModifier: Modifier,
    onEnterInspectMode: ((MediaEntity) -> Unit)?,
    onInspectZoom: ((Float) -> Unit)?,
    sharedTransitionScope: SharedTransitionScope?,
    inspectMediaUri: String?,
    navSharedTransitionScope: SharedTransitionScope?,
    navAnimatedVisibilityScope: AnimatedVisibilityScope?
) {
    val file = remember(item.uri) { File(item.uri) }
    val gestureModifier = inspectGestureModifier(item, onEnterInspectMode, onInspectZoom)
    val sharedElementModifier = sharedElementModifier(item, sharedTransitionScope, inspectMediaUri)
    val navSharedElementModifier = navSharedElementModifier(item, navSharedTransitionScope, navAnimatedVisibilityScope)

    AsyncImage(
        model = file,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
        modifier = sizeModifier
            .aspectRatio(1f)
            .then(navSharedElementModifier)
            .then(sharedElementModifier)
            .then(gestureModifier)
    )
}

// ─── Gesture modifier ─────────────────────────────────────────────────────────

/**
 * Returns a [Modifier] that handles double-tap (enter inspect) and
 * pinch-open (enter + forward ongoing zoom). Uses [PointerEventPass.Initial]
 * while waiting for a second finger so single-finger pager swipes are not consumed.
 */
@Composable
private fun inspectGestureModifier(
    item: MediaEntity,
    onEnterInspectMode: ((MediaEntity) -> Unit)?,
    onInspectZoom: ((Float) -> Unit)?
): Modifier {
    if (onEnterInspectMode == null && onInspectZoom == null) return Modifier

    val currentOnEnterInspect by rememberUpdatedState(onEnterInspectMode)
    val currentOnInspectZoom by rememberUpdatedState(onInspectZoom)

    return Modifier
        .pointerInput(item) {
            detectTapGestures(onDoubleTap = { currentOnEnterInspect?.invoke(item) })
        }
        .pointerInput(item) {
            awaitEachGesture {
                // Phase 1 — peek with Initial pass; don't consume single-finger swipes.
                awaitFirstDown(requireUnconsumed = false)
                var twoFingers = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val count = event.changes.count { it.pressed }
                    if (count >= 2) {
                        twoFingers = true
                        break
                    }
                    if (count == 0) break
                }
                if (!twoFingers) return@awaitEachGesture

                // Phase 2 — two fingers confirmed; consume all events and track distance.
                var prevDist = 0f
                var enteredInspect = false
                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.size < 2) break
                    event.changes.forEach { it.consume() }

                    val dist = (pressed[0].position - pressed[1].position).getDistance()
                    if (prevDist == 0f) {
                        prevDist = dist
                        continue
                    }

                    val zoomDelta = dist / prevDist
                    prevDist = dist

                    if (!enteredInspect) {
                        if (zoomDelta > 1f) {
                            enteredInspect = true
                            currentOnEnterInspect?.invoke(item)
                        }
                    } else {
                        currentOnInspectZoom?.invoke(zoomDelta)
                    }
                }
            }
        }
}

// ─── Shared element modifier ──────────────────────────────────────────────────

@Composable
private fun sharedElementModifier(
    item: MediaEntity,
    sharedTransitionScope: SharedTransitionScope?,
    inspectMediaUri: String?
): Modifier {
    if (sharedTransitionScope == null) return Modifier
    return with(sharedTransitionScope) {
        Modifier.sharedElementWithCallerManagedVisibility(
            sharedContentState = rememberSharedContentState(key = "inspect-${item.uri}"),
            visible = inspectMediaUri != item.uri
        )
    }
}

@Composable
private fun navSharedElementModifier(
    item: MediaEntity,
    navSharedTransitionScope: SharedTransitionScope?,
    navAnimatedVisibilityScope: AnimatedVisibilityScope?
): Modifier {
    if (navSharedTransitionScope == null || navAnimatedVisibilityScope == null) return Modifier
    return with(navSharedTransitionScope) {
        Modifier.sharedElement(
            state = rememberSharedContentState(key = "media-${item.uri}"),
            animatedVisibilityScope = navAnimatedVisibilityScope
        )
    }
}

// ─── Video player ─────────────────────────────────────────────────────────────

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun MediaVideoPlayer(uri: String, isActive: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(uri))))
            prepare()
        }
    }
    player.playWhenReady = isActive

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    // Gray shutter shown until the first video frame is rendered (default is black).
    val shutterColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                setShutterBackgroundColor(shutterColor)
            }
        },
        modifier = modifier
    )
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MediaPagerPreview() {
    val media = List(3) { i ->
        MediaEntity(
            id = i.toLong(),
            postId = 0,
            uri = "https://picsum.photos/seed/$i/600/600",
            mediaType = "image",
            timestamp = 0
        )
    }
    MediaPager(media = media)
}
