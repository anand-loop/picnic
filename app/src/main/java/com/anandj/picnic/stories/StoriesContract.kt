// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.stories

object StoriesContract {
    sealed class Action {
        object Reload : Action()
        object LoadMore : Action()
        object OpenSettings : Action()
        data class OpenGroup(val dayKey: String) : Action()
    }

    sealed class Effect {
        object NavigateToSettings : Effect()
        data class NavigateToGroup(val dayKey: String) : Effect()
    }
}
