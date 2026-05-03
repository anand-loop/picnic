// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.bento

/**
 * UI state for a paginated list of [T].
 *
 * @property items The accumulated list of loaded items.
 * @property isLoading True during the initial load.
 * @property isLoadingMore True while fetching the next page.
 * @property isRefreshing True during a pull-to-refresh.
 * @property error Full-screen error message shown when [items] is empty.
 * @property paginationError Inline error shown when a subsequent page fails.
 * @property currentPage The last successfully loaded page number.
 * @property totalPages Total number of pages reported by the API.
 * @property endReached True when [currentPage] has reached [totalPages].
 */
data class PaginatedState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val paginationError: String? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 1,
    val endReached: Boolean = false,
)
