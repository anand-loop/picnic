// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.anandj.picnic.feed

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.anandj.bento.PaginatedList
import com.anandj.bento.PaginatedListLayout
import com.anandj.picnic.ui.PicnicTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    scrollBehavior: TopAppBarScrollBehavior,
    columns: Int = 2
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            PicnicTopAppBar(
                onSettingsClick = { viewModel.sendAction(FeedContract.Action.OpenSettings) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        PaginatedList(
            state = state,
            onLoadInitial = { viewModel.sendAction(FeedContract.Action.Reload) },
            onLoadMore = { viewModel.sendAction(FeedContract.Action.LoadMore) },
            itemKey = { it.post.id },
            itemContent = { post ->
                GridCell(
                    item = post,
                    onClick = { viewModel.sendAction(FeedContract.Action.OpenPost(post.post.id)) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            },
            layout = PaginatedListLayout.Grid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            ),
            modifier = Modifier
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        )
    }
}
