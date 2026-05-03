// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.bento

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A horizontal pager with optional page indicators.
 *
 * @param items Items to page through.
 * @param maxHeight Optional maximum height for each page.
 * @param showIndicators Whether to display page indicator dots. Defaults to true when there are multiple items.
 * @param indicatorActiveColor Color of the active page dot.
 * @param indicatorInactiveColor Color of inactive page dots.
 * @param indicatorSpacing Space between the pager and the indicator row.
 * @param content Composable rendered for each page, receiving the index, item, and whether the page is currently visible.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> Carousel(
    items: List<T>,
    modifier: Modifier = Modifier,
    maxHeight: Dp? = null,
    showIndicators: Boolean = items.size > 1,
    indicatorActiveColor: Color = LocalContentColor.current,
    indicatorInactiveColor: Color = indicatorActiveColor.copy(alpha = 0.4f),
    indicatorSpacing: Dp = 14.dp,
    content: @Composable (index: Int, item: T, isActive: Boolean) -> Unit,
) {
    val pageCount = items.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                val item = items.getOrNull(page) ?: return@HorizontalPager
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(if (maxHeight != null) Modifier.heightIn(max = maxHeight) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    content(page, item, page == pagerState.currentPage)
                }
            }
        }

        if (showIndicators) {
            PageIndicators(
                pageCount = pageCount,
                currentPage = pagerState.currentPage,
                activeColor = indicatorActiveColor,
                inactiveColor = indicatorInactiveColor,
                modifier = Modifier.padding(top = indicatorSpacing),
            )
        }
    }
}

@Composable
private fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val active = index == currentPage
            Box(
                modifier =
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (active) 8.dp else 6.dp)
                        .background(
                            color = if (active) activeColor else inactiveColor,
                            shape = CircleShape,
                        ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CarouselPreview() {
    Carousel(
        items = listOf(0xFF1E88E5, 0xFF43A047, 0xFFE53935, 0xFFFB8C00),
        modifier = Modifier.fillMaxWidth(),
        maxHeight = 160.dp,
    ) { _, color, _ ->
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color(color)),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CarouselSingleItemPreview() {
    Carousel(
        items = listOf(0xFF8E24AA),
        modifier = Modifier.fillMaxWidth(),
        maxHeight = 120.dp,
    ) { _, color, _ ->
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(color)),
        )
    }
}
