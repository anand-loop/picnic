// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.post

import com.anandj.picnic.data.db.relation.PostWithMedia

object PostContract {
    data class State(val post: PostWithMedia? = null)

    sealed class Action {
        object NavigateBack : Action()
    }

    sealed class Effect {
        object NavigateBack : Effect()
    }
}
