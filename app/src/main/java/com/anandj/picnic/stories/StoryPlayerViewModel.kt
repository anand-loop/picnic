// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.stories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.anandj.bento.BaseViewModel
import com.anandj.picnic.data.db.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class StoryPlayerViewModel @Inject constructor(
    private val db: AppDatabase,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<StoryPlayerContract.State, StoryPlayerContract.Effect, StoryPlayerContract.Action>(
    StoryPlayerContract.State()
) {

    private val dayKey: String = checkNotNull(savedStateHandle.get<String>("dayKey"))

    init {
        viewModelScope.launch {
            val items = db.storyDao().getByDay(dayKey)
            updateState { it.copy(items = items, dayKey = dayKey) }
        }
    }

    override suspend fun onAction(action: StoryPlayerContract.Action) {
        when (action) {
            StoryPlayerContract.Action.Next -> {
                val s = currentState
                if (s.items.isEmpty()) return
                updateState {
                    it.copy(
                        currentIndex = (s.currentIndex + 1) % s.items.size,
                        playToken = s.playToken + 1
                    )
                }
            }
            StoryPlayerContract.Action.Previous -> {
                val s = currentState
                if (s.items.isEmpty()) return
                val prev = if (s.currentIndex == 0) s.items.lastIndex else s.currentIndex - 1
                updateState { it.copy(currentIndex = prev, playToken = s.playToken + 1) }
            }
            StoryPlayerContract.Action.Restart -> {
                val s = currentState
                if (s.items.isEmpty()) return
                updateState { it.copy(playToken = s.playToken + 1) }
            }
            StoryPlayerContract.Action.Pause -> updateState { it.copy(isPaused = true) }
            StoryPlayerContract.Action.Resume -> updateState { it.copy(isPaused = false) }
            StoryPlayerContract.Action.NavigateBack -> sendEffect(StoryPlayerContract.Effect.NavigateBack)
        }
    }
}
