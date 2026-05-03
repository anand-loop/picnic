// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.post

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.anandj.picnic.data.db.entity.MediaEntity
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val DISMISS_THRESHOLD = 0.6f

class InspectState(
    val media: MediaEntity? = null,
    val scale: MutableFloatState = mutableFloatStateOf(1f),
    val offsetX: MutableFloatState = mutableFloatStateOf(0f),
    val offsetY: MutableFloatState = mutableFloatStateOf(0f)
) {
    val isActive: Boolean get() = media != null

    fun reset() {
        scale.floatValue = 1f
        offsetX.floatValue = 0f
        offsetY.floatValue = 0f
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun InspectModeOverlay(
    state: InspectState,
    onDismiss: () -> Unit,
    sharedTransitionScope: SharedTransitionScope
) {
    val coroutineScope = rememberCoroutineScope()
    var snapJob = remember<Job?> { null }

    val media = state.media
    val scale = state.scale
    val offsetX = state.offsetX
    val offsetY = state.offsetY

    AnimatedVisibility(
        visible = media != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .pointerInput(Unit) {
                    // Consume ALL pointer events to prevent leaking to parent
                    // composables (e.g. ModalNavigationDrawer interpreting
                    // pinch moves as edge-swipe to open the debug drawer).
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        var pointersUp = false
                        var wasDrag = false
                        while (!pointersUp) {
                            val event = awaitPointerEvent()
                            event.changes.forEach {
                                if (it.positionChanged()) wasDrag = true
                                it.consume()
                            }
                            pointersUp = event.changes.none { it.pressed }
                        }
                        if (!wasDrag) onDismiss()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (media != null) {
                val file = remember(media.uri) { File(media.uri) }
                with(sharedTransitionScope) {
                    AsyncImage(
                        model = file,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .sharedElement(
                                state = rememberSharedContentState(key = "inspect-${media.uri}"),
                                animatedVisibilityScope = this@AnimatedVisibility
                            )
                            .graphicsLayer(
                                scaleX = scale.floatValue,
                                scaleY = scale.floatValue,
                                translationX = offsetX.floatValue,
                                translationY = offsetY.floatValue
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(onDoubleTap = { onDismiss() })
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    // Cancel any running snap animation when the user touches again.
                                    snapJob?.cancel()

                                    scale.floatValue = (scale.floatValue * zoom).coerceIn(0.25f, 8f)
                                    val maxX =
                                        (size.width * (scale.floatValue - 1f)).coerceAtLeast(0f) / 2f
                                    val maxY =
                                        (size.height * (scale.floatValue - 1f)).coerceAtLeast(0f) / 2f
                                    offsetX.floatValue =
                                        (offsetX.floatValue + pan.x * scale.floatValue).coerceIn(-maxX, maxX)
                                    offsetY.floatValue =
                                        (offsetY.floatValue + pan.y * scale.floatValue).coerceIn(-maxY, maxY)
                                }
                            }
                            .pointerInput(Unit) {
                                // Watches for full gesture release to trigger snap-back or dismiss.
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    do {
                                        val event = awaitPointerEvent()
                                    } while (event.changes.any { it.pressed })

                                    val currentScale = scale.floatValue
                                    when {
                                        currentScale < DISMISS_THRESHOLD -> onDismiss()
                                        currentScale < 1f -> {
                                            snapJob = coroutineScope.launch {
                                                Animatable(currentScale).animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMedium
                                                    )
                                                ) { scale.floatValue = value }
                                            }
                                        }
                                    }
                                }
                            }
                    )
                }
            }
        }
    }
}
