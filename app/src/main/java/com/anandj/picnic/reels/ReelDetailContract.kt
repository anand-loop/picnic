// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.reels

import com.anandj.picnic.data.db.entity.ReelEntity

object ReelDetailContract {
    data class State(val reel: ReelEntity? = null)

    sealed class Action {
        object NavigateBack : Action()
    }

    sealed class Effect {
        object NavigateBack : Effect()
    }
}
