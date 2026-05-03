// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.anandj.picnic.post

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.anandj.bento.ExpandableText
import com.anandj.picnic.R
import com.anandj.picnic.data.db.entity.MediaEntity
import com.anandj.picnic.data.db.entity.PostEntity
import com.anandj.picnic.data.db.relation.PostWithMedia
import com.anandj.picnic.ui.theme.Black
import com.anandj.picnic.ui.theme.White
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PostScreen(
    viewModel: PostViewModel,
    onNavigateBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PostContract.Effect.NavigateBack -> onNavigateBack()
            }
        }
    }

    BackHandler { viewModel.sendAction(PostContract.Action.NavigateBack) }

    Box(modifier = Modifier.fillMaxSize().background(Black)) {
        val p = state.post
        if (p == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            PostContentColumn(
                post = p,
                onDismiss = { viewModel.sendAction(PostContract.Action.NavigateBack) },
                navSharedTransitionScope = sharedTransitionScope,
                navAnimatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun PostContentColumn(
    post: PostWithMedia,
    onDismiss: () -> Unit,
    navSharedTransitionScope: SharedTransitionScope,
    navAnimatedVisibilityScope: AnimatedVisibilityScope
) {
    val caption = post.post.caption?.trimStart()
    val context = LocalContext.current
    var inspectState by remember { mutableStateOf(InspectState()) }

    BackHandler(enabled = inspectState.isActive) {
        inspectState = InspectState()
    }

    val toolbarVisible = !inspectState.isActive
    val toolbarAlpha by animateFloatAsState(
        targetValue = if (toolbarVisible) 1f else 0f,
        label = "toolbarAlpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val media = post.media.firstOrNull() ?: return@IconButton
                        shareMedia(context, media)
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = "Share",
                            tint = White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Black),
                modifier = Modifier.graphicsLayer {
                    alpha = toolbarAlpha
                    translationY = (1f - toolbarAlpha) * -size.height
                }
            )
        },
        containerColor = Black,
        contentColor = White
    ) { innerPadding ->
        SharedTransitionLayout(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    MediaPager(
                        media = post.media,
                        onEnterInspectMode = {
                            inspectState = InspectState(media = it)
                        },
                        onInspectZoom = { delta ->
                            inspectState.scale.floatValue = (inspectState.scale.floatValue * delta).coerceIn(1f, 8f)
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        inspectMediaUri = inspectState.media?.uri,
                        navSharedTransitionScope = navSharedTransitionScope,
                        navAnimatedVisibilityScope = navAnimatedVisibilityScope
                    )
                    val date = remember(post.post.timestamp) {
                        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                            .format(Date(post.post.timestamp * 1000L))
                    }
                    val lat = post.post.latitude
                    val lng = post.post.longitude
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = White.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f)
                        )
                        if (lat != null && lng != null) {
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, "geo:$lat,$lng?q=$lat,$lng".toUri())
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_globe_location),
                                    contentDescription = "View location",
                                    tint = White
                                )
                            }
                        }
                    }
                    if (!caption.isNullOrBlank()) {
                        ExpandableText(text = caption, modifier = Modifier.padding(top = 12.dp))
                    }
                }

                InspectModeOverlay(
                    state = inspectState,
                    onDismiss = { inspectState = InspectState() },
                    sharedTransitionScope = this@SharedTransitionLayout
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, showSystemUi = true)
@Composable
private fun PostContentColumnPreview() {
    val media = List(3) { i ->
        MediaEntity(
            id = i.toLong(),
            postId = 0,
            uri = "",
            mediaType = "image",
            timestamp = 0
        )
    }
    val post = PostWithMedia(
        post = PostEntity(
            id = 1,
            timestamp = 1746921600L,
            caption = "Exploring the hidden trails of Patagonia. Three days in and every " +
                "turn reveals something more breathtaking than the last.",
            latitude = 39.087436,
            longitude = -120.055847
        ),
        media = media
    )
    SharedTransitionLayout {
        androidx.compose.animation.AnimatedVisibility(visible = true) {
            PostContentColumn(
                post = post,
                onDismiss = {},
                navSharedTransitionScope = this@SharedTransitionLayout,
                navAnimatedVisibilityScope = this@AnimatedVisibility
            )
        }
    }
}
