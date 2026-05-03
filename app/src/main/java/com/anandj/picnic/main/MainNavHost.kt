// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.anandj.picnic.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anandj.picnic.data.DataManagementViewModel
import com.anandj.picnic.feed.FeedContract
import com.anandj.picnic.feed.FeedViewModel
import com.anandj.picnic.import.ImportScreen
import com.anandj.picnic.post.PostScreen
import com.anandj.picnic.post.PostViewModel
import com.anandj.picnic.reels.ReelDetailScreen
import com.anandj.picnic.reels.ReelDetailViewModel
import com.anandj.picnic.reels.ReelsContract
import com.anandj.picnic.reels.ReelsViewModel
import com.anandj.picnic.settings.SettingsScreen
import com.anandj.picnic.stories.StoriesContract
import com.anandj.picnic.stories.StoriesViewModel
import com.anandj.picnic.stories.StoryPlayerScreen
import com.anandj.picnic.stories.StoryPlayerViewModel

@Composable
fun MainNavHost(startDestination: String) {
    val navController = rememberNavController()
    val feedViewModel: FeedViewModel = hiltViewModel()
    val reelsViewModel: ReelsViewModel = hiltViewModel()
    val storiesViewModel: StoriesViewModel = hiltViewModel()
    val dataViewModel: DataManagementViewModel = hiltViewModel()
    val gridColumns by dataViewModel.gridColumns.collectAsState()

    LaunchedEffect(Unit) {
        feedViewModel.effects.collect { effect ->
            when (effect) {
                is FeedContract.Effect.NavigateToPost -> navController.navigate(Routes.post(effect.postId))
                FeedContract.Effect.NavigateToSettings -> navController.navigate(Routes.SETTINGS)
            }
        }
    }

    LaunchedEffect(Unit) {
        reelsViewModel.effects.collect { effect ->
            when (effect) {
                is ReelsContract.Effect.NavigateToReel -> navController.navigate(Routes.reel(effect.reelId))
                ReelsContract.Effect.NavigateToSettings -> navController.navigate(Routes.SETTINGS)
            }
        }
    }

    LaunchedEffect(Unit) {
        storiesViewModel.effects.collect { effect ->
            when (effect) {
                StoriesContract.Effect.NavigateToSettings -> navController.navigate(Routes.SETTINGS)
                is StoriesContract.Effect.NavigateToGroup -> navController.navigate(Routes.storyGroup(effect.dayKey))
            }
        }
    }

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(
                route = Routes.IMPORT,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) {
                val importViewModel: DataManagementViewModel = hiltViewModel()
                val importState by importViewModel.importState.collectAsState()
                val pickZip = rememberLauncherForActivityResult(GetContent()) { uri ->
                    uri?.let { importViewModel.importZip(it) }
                }
                ImportScreen(
                    importState = importState,
                    onImportClick = { pickZip.launch("application/zip") },
                    onGoToFeedClick = {
                        feedViewModel.sendAction(FeedContract.Action.Reload)
                        reelsViewModel.sendAction(ReelsContract.Action.Reload)
                        storiesViewModel.sendAction(StoriesContract.Action.Reload)
                        navController.navigate(Routes.FEED) {
                            popUpTo(Routes.IMPORT) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.FEED,
                popEnterTransition = { fadeIn() },
                exitTransition = { fadeOut() }
            ) {
                HomeScreen(
                    feedViewModel = feedViewModel,
                    reelsViewModel = reelsViewModel,
                    storiesViewModel = storiesViewModel,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    columns = gridColumns
                )
            }

            composable(
                route = Routes.SETTINGS,
                enterTransition = { slideInVertically { it } + fadeIn() },
                popExitTransition = { slideOutVertically { it } + fadeOut() }
            ) {
                val settingsViewModel: DataManagementViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOperationComplete = {
                        navController.navigate(Routes.IMPORT) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.POST,
                enterTransition = { fadeIn(tween(500)) },
                popExitTransition = { fadeOut(tween(500)) }
            ) {
                val mediaViewModel: PostViewModel = hiltViewModel()
                PostScreen(
                    viewModel = mediaViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }

            composable(
                route = Routes.REEL,
                enterTransition = { fadeIn(tween(300)) },
                popExitTransition = { fadeOut(tween(300)) }
            ) {
                val reelDetailViewModel: ReelDetailViewModel = hiltViewModel()
                ReelDetailScreen(
                    viewModel = reelDetailViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable
                )
            }

            composable(
                route = Routes.STORY_GROUP,
                enterTransition = { fadeIn(tween(300)) },
                popExitTransition = { fadeOut(tween(300)) }
            ) {
                val storyPlayerViewModel: StoryPlayerViewModel = hiltViewModel()
                StoryPlayerScreen(
                    viewModel = storyPlayerViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
