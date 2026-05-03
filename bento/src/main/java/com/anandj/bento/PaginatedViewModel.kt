// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.bento

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * [BaseViewModel] with built-in pagination, refresh, and error-handling logic.
 *
 * Subclasses implement [fetchPage] and call [loadInitial], [loadNextPage], and [refresh]
 * to drive [PaginatedState] automatically.
 *
 * @param T The type of item in the paginated list.
 * @param E Effect type forwarded to [BaseViewModel].
 * @param A Action type forwarded to [BaseViewModel].
 */
abstract class PaginatedViewModel<T, E, A> : BaseViewModel<PaginatedState<T>, E, A>(PaginatedState()) {
    /**
     * Fetches a single page of data. Returns a [Result] containing the list of items
     * and the total page count, or a failure if the request errors.
     */
    protected abstract suspend fun fetchPage(page: Int): Result<Pair<List<T>, Int>>

    /** Resets state and loads page 1. No-ops if a load is already in progress. */
    protected fun loadInitial() {
        if (currentState.isLoading) return
        updateState {
            it.copy(
                isLoading = true,
                error = null,
                paginationError = null,
                items = emptyList(),
                currentPage = 0,
                endReached = false,
            )
        }
        loadPage(1)
    }

    /** Reloads page 1 without clearing the existing item list. No-ops if already refreshing or loading. */
    protected fun refresh() {
        val current = currentState
        if (current.isRefreshing || current.isLoading) return
        updateState { it.copy(isRefreshing = true, paginationError = null, error = null) }
        viewModelScope.launch {
            fetchPage(1)
                .onSuccess { (items, totalPages) ->
                    updateState {
                        it.copy(
                            items = items,
                            isRefreshing = false,
                            currentPage = 1,
                            totalPages = totalPages,
                            endReached = 1 >= totalPages,
                            paginationError = null,
                            error = null,
                        )
                    }
                }
                .onFailure { updateState { it.copy(isRefreshing = false) } }
        }
    }

    /** Appends the next page to [PaginatedState.items]. No-ops if loading, refreshing, or end is reached. */
    protected fun loadNextPage() {
        val current = currentState
        if (current.isLoadingMore || current.endReached || current.isLoading) return
        updateState { it.copy(isLoadingMore = true, error = null, paginationError = null) }
        loadPage(current.currentPage + 1)
    }

    private fun loadPage(page: Int) {
        viewModelScope.launch {
            fetchPage(page)
                .onSuccess { (items, totalPages) ->
                    updateState { current ->
                        current.copy(
                            items = current.items + items,
                            isLoading = false,
                            isLoadingMore = false,
                            currentPage = page,
                            totalPages = totalPages,
                            endReached = page >= totalPages,
                            error = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    val isPagination = currentState.items.isNotEmpty()
                    updateState {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error =
                                if (isPagination) {
                                    null
                                } else {
                                    throwable.message ?: "An unexpected error occurred"
                                },
                            paginationError =
                                if (isPagination) {
                                    throwable.message ?: "Failed to load more"
                                } else {
                                    null
                                },
                        )
                    }
                }
        }
    }
}
