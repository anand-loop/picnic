// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.reels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anandj.picnic.R
import com.anandj.picnic.ui.PicnicTopAppBar

@Composable
private fun EmptyReelsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_reels),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = stringResource(R.string.reels_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.reels_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    viewModel: ReelsViewModel,
    scrollBehavior: TopAppBarScrollBehavior,
    columns: Int = 2
) {
    val state by viewModel.state.collectAsState()
    val gridState = rememberLazyGridState()

    val isLoadingMoreRef = rememberUpdatedState(state.isLoadingMore)
    val endReachedRef = rememberUpdatedState(state.endReached)
    val onLoadMore = rememberUpdatedState { viewModel.sendAction(ReelsContract.Action.LoadMore) }

    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            info.visibleItemsInfo.lastOrNull()?.index to info.totalItemsCount
        }.collect { (lastIndex, total) ->
            if (lastIndex != null && lastIndex >= total - 18 &&
                !isLoadingMoreRef.value && !endReachedRef.value
            ) {
                onLoadMore.value()
            }
        }
    }

    Scaffold(
        topBar = {
            PicnicTopAppBar(
                onSettingsClick = { viewModel.sendAction(ReelsContract.Action.OpenSettings) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.items.isEmpty() -> {
                    EmptyReelsState(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        state = gridState,
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.items, key = { it.id }) { reel ->
                            ReelGridCell(
                                item = reel,
                                onClick = { viewModel.sendAction(ReelsContract.Action.OpenReel(reel.id)) }
                            )
                        }
                        if (state.isLoadingMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
