// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.bento

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layout strategy for [PaginatedList].
 */
sealed interface PaginatedListLayout {
    /**
     * Vertical list layout backed by [LazyColumn].
     *
     * @property itemSpacing Vertical space between items.
     */
    data class List(
        val itemSpacing: Dp = 8.dp,
    ) : PaginatedListLayout

    /**
     * Grid layout backed by [LazyVerticalGrid].
     *
     * @property columns Grid column configuration.
     * @property contentPadding Padding around the grid content.
     */
    data class Grid(
        val columns: GridCells = GridCells.Fixed(3),
        val contentPadding: PaddingValues = PaddingValues(0.dp),
    ) : PaginatedListLayout
}
