// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.anandj.picnic.main

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anandj.picnic.feed.FeedScreen
import com.anandj.picnic.feed.FeedViewModel
import com.anandj.picnic.feed.FloatingBottomNavBar
import com.anandj.picnic.feed.NavTab
import com.anandj.picnic.reels.ReelsScreen
import com.anandj.picnic.reels.ReelsViewModel
import com.anandj.picnic.stories.StoriesScreen
import com.anandj.picnic.stories.StoriesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    feedViewModel: FeedViewModel,
    reelsViewModel: ReelsViewModel,
    storiesViewModel: StoriesViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    columns: Int
) {
    var selectedTab by rememberSaveable { mutableStateOf(NavTab.POSTS) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Box(modifier = Modifier.fillMaxSize()) {
        when (selectedTab) {
            NavTab.POSTS -> FeedScreen(
                viewModel = feedViewModel,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                scrollBehavior = scrollBehavior,
                columns = columns
            )
            NavTab.REELS -> ReelsScreen(
                viewModel = reelsViewModel,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                scrollBehavior = scrollBehavior,
                columns = columns
            )
            NavTab.STORIES -> StoriesScreen(
                viewModel = storiesViewModel,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                scrollBehavior = scrollBehavior,
                columns = columns
            )
        }

        FloatingBottomNavBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}
