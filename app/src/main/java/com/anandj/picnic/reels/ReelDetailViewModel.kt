// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.reels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.anandj.bento.BaseViewModel
import com.anandj.picnic.data.db.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class ReelDetailViewModel @Inject constructor(
    private val db: AppDatabase,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<ReelDetailContract.State, ReelDetailContract.Effect, ReelDetailContract.Action>(
    ReelDetailContract.State()
) {

    private val reelId: Long = checkNotNull(savedStateHandle.get<String>("reelId")).toLong()

    /** Last known playback position in milliseconds. Survives configuration changes. */
    var playbackPositionMs: Long = 0L

    init {
        viewModelScope.launch {
            val reel = db.reelDao().getById(reelId)
            updateState { ReelDetailContract.State(reel = reel) }
        }
    }

    override suspend fun onAction(action: ReelDetailContract.Action) {
        when (action) {
            ReelDetailContract.Action.NavigateBack -> sendEffect(ReelDetailContract.Effect.NavigateBack)
        }
    }
}
