// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.bento

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A pull-to-refresh paginated container that handles initial loading, pagination, and error states.
 * Supports both list and grid layouts via the [layout] parameter.
 *
 * @param state Current pagination state.
 * @param onLoadInitial Called to retry after an initial load error.
 * @param onLoadMore Called when the user scrolls near the end.
 * @param onRefresh Called on pull-to-refresh. When null, the pull-to-refresh gesture is disabled.
 * @param itemKey Stable key factory for items.
 * @param itemContent Composable rendered for each item.
 * @param layout Layout strategy — use [PaginatedListLayout.List] or [PaginatedListLayout.Grid].
 * @param bottomPadding Extra padding at the bottom.
 * @param footerContent Optional composable shown after all items have been loaded.
 * @param emptyContent Composable shown when the list is empty and not loading. If null, nothing is displayed.
 * @param loadingMoreContent Composable shown while fetching the next page. Uses a default loading indicator when null.
 * @param errorContent Composable shown on initial load error. Receives a retry callback. Uses a default error view when null.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> PaginatedList(
    state: PaginatedState<T>,
    onLoadInitial: () -> Unit,
    onLoadMore: () -> Unit,
    itemKey: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    onRefresh: (() -> Unit)? = null,
    layout: PaginatedListLayout = PaginatedListLayout.List(),
    bottomPadding: Dp = 0.dp,
    footerContent: (@Composable () -> Unit)? = null,
    loadingMoreContent: (@Composable () -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null,
    errorContent: (@Composable (onRetry: () -> Unit) -> Unit)? = null,
) {
    val content: @Composable BoxScope.() -> Unit = {
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            state.error != null && state.items.isEmpty() -> {
                Box(modifier = Modifier.align(Alignment.Center)) {
                    if (errorContent != null) {
                        errorContent(onLoadInitial)
                    } else {
                        ErrorView(
                            message = state.error,
                            onRetry = onLoadInitial,
                        )
                    }
                }
            }

            state.items.isEmpty() && emptyContent != null -> {
                Box(modifier = Modifier.align(Alignment.Center)) {
                    emptyContent()
                }
            }

            else -> {
                when (layout) {
                    is PaginatedListLayout.List ->
                        PaginatedLazyColumn(
                            state = state,
                            itemKey = itemKey,
                            itemContent = itemContent,
                            onLoadMore = onLoadMore,
                            bottomPadding = bottomPadding,
                            itemSpacing = layout.itemSpacing,
                            footerContent = footerContent,
                            loadingMoreContent = loadingMoreContent,
                        )

                    is PaginatedListLayout.Grid ->
                        PaginatedLazyGrid(
                            state = state,
                            itemKey = itemKey,
                            itemContent = itemContent,
                            onLoadMore = onLoadMore,
                            columns = layout.columns,
                            contentPadding = layout.contentPadding,
                            bottomPadding = bottomPadding,
                            footerContent = footerContent,
                            loadingMoreContent = loadingMoreContent,
                        )
                }
            }
        }
    }

    if (onRefresh != null) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier.fillMaxSize(),
            content = content,
        )
    } else {
        Box(modifier = modifier.fillMaxSize(), content = content)
    }
}

@Composable
private fun <T : Any> PaginatedLazyColumn(
    state: PaginatedState<T>,
    itemKey: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
    onLoadMore: () -> Unit,
    bottomPadding: Dp,
    itemSpacing: Dp,
    footerContent: (@Composable () -> Unit)?,
    loadingMoreContent: (@Composable () -> Unit)?,
) {
    val listState = rememberLazyListState()
    val onLoadMoreRef = rememberUpdatedState(onLoadMore)

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            info.visibleItemsInfo.lastOrNull()?.index to info.totalItemsCount
        }.collect { (lastIndex, totalItems) ->
            if (lastIndex != null && lastIndex >= totalItems - 3 &&
                !state.isLoadingMore && !state.endReached
            ) {
                onLoadMoreRef.value()
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        items(state.items, key = { itemKey(it) }) { item ->
            itemContent(item)
        }

        when {
            state.isLoadingMore ->
                item(key = "loading_more") {
                    if (loadingMoreContent != null) loadingMoreContent() else LoadingMoreIndicator()
                }

            state.paginationError != null ->
                item(key = "pagination_error") {
                    PaginationErrorView(
                        message = state.paginationError,
                        onRetry = onLoadMore,
                    )
                }

            footerContent != null && state.endReached && state.items.isNotEmpty() ->
                item(key = "end_reached") { footerContent() }
        }
    }
}

@Composable
private fun <T : Any> PaginatedLazyGrid(
    state: PaginatedState<T>,
    itemKey: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
    onLoadMore: () -> Unit,
    columns: GridCells,
    contentPadding: PaddingValues,
    bottomPadding: Dp,
    footerContent: (@Composable () -> Unit)?,
    loadingMoreContent: (@Composable () -> Unit)?,
) {
    val gridState = rememberLazyGridState()
    val onLoadMoreRef = rememberUpdatedState(onLoadMore)
    val layoutDirection = LocalLayoutDirection.current

    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            info.visibleItemsInfo.lastOrNull()?.index to info.totalItemsCount
        }.collect { (lastIndex, total) ->
            if (lastIndex != null && lastIndex >= total - 18 &&
                !state.isLoadingMore && !state.endReached
            ) {
                onLoadMoreRef.value()
            }
        }
    }

    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        contentPadding =
            PaddingValues(
                start = contentPadding.calculateLeftPadding(layoutDirection),
                top = contentPadding.calculateTopPadding(),
                end = contentPadding.calculateRightPadding(layoutDirection),
                bottom = contentPadding.calculateBottomPadding() + bottomPadding,
            ),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.items, key = { itemKey(it) }) { item ->
            itemContent(item)
        }

        when {
            state.isLoadingMore ->
                item(key = "loading_more", span = { GridItemSpan(maxLineSpan) }) {
                    if (loadingMoreContent != null) loadingMoreContent() else LoadingMoreIndicator()
                }

            state.paginationError != null ->
                item(key = "pagination_error", span = { GridItemSpan(maxLineSpan) }) {
                    PaginationErrorView(
                        message = state.paginationError,
                        onRetry = onLoadMore,
                    )
                }

            footerContent != null && state.endReached && state.items.isNotEmpty() ->
                item(key = "end_reached", span = { GridItemSpan(maxLineSpan) }) {
                    footerContent()
                }
        }
    }
}

@Composable
private fun LoadingMoreIndicator() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PaginationErrorView(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        Button(onClick = onRetry) { Text(stringResource(R.string.bento_retry)) }
    }
}

@Composable
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.bento_error_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.bento_retry))
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun PaginatedListLoadedPreview() {
    MaterialTheme {
        PaginatedList(
            state =
                PaginatedState(
                    items = (1..5).map { "News item #$it" },
                    endReached = true,
                ),
            onLoadInitial = {},
            onLoadMore = {},
            onRefresh = {},
            itemKey = { it },
            itemContent = { item ->
                Text(
                    text = item,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            footerContent = { Text("All 5 articles loaded", style = MaterialTheme.typography.bodySmall) },
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun PaginatedListErrorPreview() {
    MaterialTheme {
        PaginatedList<String>(
            state = PaginatedState(error = "Unable to load items. Check your connection."),
            onLoadInitial = {},
            onLoadMore = {},
            onRefresh = {},
            itemKey = { it },
            itemContent = { Text(it) },
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun PaginatedGridPreview() {
    MaterialTheme {
        PaginatedList(
            state =
                PaginatedState(
                    items = (1..9).toList(),
                    endReached = true,
                ),
            onLoadInitial = {},
            onLoadMore = {},
            onRefresh = {},
            itemKey = { it },
            itemContent = { item ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .background(Color(0xFFBBDEFB)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "$item", color = Color(0xFF0D47A1))
                }
            },
            layout =
                PaginatedListLayout.Grid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                ),
            footerContent = { Text("All 9 items loaded", style = MaterialTheme.typography.bodySmall) },
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 320)
@Composable
private fun ErrorViewPreview() {
    MaterialTheme {
        ErrorView(
            message = "Unable to load items. Check your connection.",
            onRetry = {},
        )
    }
}
