// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.stories

import com.anandj.picnic.data.db.entity.StoryEntity

object StoryPlayerContract {
    data class State(
        val items: List<StoryEntity> = emptyList(),
        val currentIndex: Int = 0,
        val playToken: Int = 0,
        val isPaused: Boolean = false,
        val dayKey: String = ""
    )

    sealed class Action {
        object Next : Action()
        object Previous : Action()
        object Restart : Action()
        object Pause : Action()
        object Resume : Action()
        object NavigateBack : Action()
    }

    sealed class Effect {
        object NavigateBack : Effect()
    }
}
