// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.reels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anandj.picnic.data.db.entity.ReelEntity
import java.io.File

@Composable
internal fun ReelGridCell(
    item: ReelEntity,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
    ) {
        ReelCellImage(uri = item.uri)
    }
}

@Composable
private fun ReelCellImage(uri: String) {
    val context = LocalContext.current
    val request = ImageRequest.Builder(context)
        .data(File(uri))
        .crossfade(true)
        .build()
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}
