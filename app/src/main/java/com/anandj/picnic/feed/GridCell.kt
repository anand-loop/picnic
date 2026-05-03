// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.anandj.picnic.feed

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anandj.picnic.data.db.relation.PostWithMedia
import com.anandj.picnic.ui.CountBadge
import com.anandj.picnic.ui.theme.White
import java.io.File

@Composable
internal fun GridCell(
    item: PostWithMedia,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val media = item.media.firstOrNull()
    val count = item.media.size
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
    ) {
        if (media != null) {
            GridCellImage(
                uri = media.uri,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
            when {
                count > 1 -> CountBadge(
                    count = count,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                )
                media.mediaType == "video" -> GridVideoOverlay()
            }
        }
    }
}

@Composable
private fun GridCellImage(
    uri: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val context = LocalContext.current
    val request = ImageRequest.Builder(context)
        .data(File(uri))
        .crossfade(true)
        .build()
    with(sharedTransitionScope) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .sharedElement(
                    state = rememberSharedContentState(key = "media-$uri"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
        )
    }
}

@Composable
private fun BoxScope.GridVideoOverlay() {
    Icon(
        imageVector = Icons.Filled.PlayArrow,
        contentDescription = null,
        tint = White,
        modifier = Modifier.size(24.dp).align(Alignment.TopEnd).padding(4.dp)
    )
}
