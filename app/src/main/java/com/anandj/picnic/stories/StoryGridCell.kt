// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.anandj.picnic.stories

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anandj.picnic.data.db.relation.StoryGroup
import com.anandj.picnic.ui.CountBadge
import java.io.File

@Composable
internal fun StoryGridCell(
    item: StoryGroup,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
    ) {
        StoryCellImage(
            uri = item.coverUri,
            dayKey = item.dayKey,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
        if (item.itemCount > 1) {
            CountBadge(
                count = item.itemCount,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            )
        }
    }
}

@Composable
private fun StoryCellImage(
    uri: String,
    dayKey: String,
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
                    state = rememberSharedContentState(key = "story-group-$dayKey"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
        )
    }
}

