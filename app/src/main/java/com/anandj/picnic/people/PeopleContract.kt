// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.people

object PeopleContract {
    sealed class Action {
        object Reload : Action()
        object LoadMore : Action()
        object OpenSettings : Action()
        data class SelectType(val type: ConnectionType) : Action()
    }

    sealed class Effect {
        object NavigateToSettings : Effect()
    }
}
