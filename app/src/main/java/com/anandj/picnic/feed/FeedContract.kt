// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.feed

object FeedContract {
    sealed class Action {
        object Reload : Action()
        object Refresh : Action()
        object LoadMore : Action()
        data class OpenPost(val postId: Long) : Action()
        object OpenSettings : Action()
    }

    sealed class Effect {
        data class NavigateToPost(val postId: Long) : Effect()
        object NavigateToSettings : Effect()
    }
}
