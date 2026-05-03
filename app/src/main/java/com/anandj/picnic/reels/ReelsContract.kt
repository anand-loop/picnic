// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.reels

object ReelsContract {
    sealed class Action {
        object Reload : Action()
        object LoadMore : Action()
        data class OpenReel(val reelId: Long) : Action()
        object OpenSettings : Action()
    }

    sealed class Effect {
        data class NavigateToReel(val reelId: Long) : Effect()
        object NavigateToSettings : Effect()
    }
}
