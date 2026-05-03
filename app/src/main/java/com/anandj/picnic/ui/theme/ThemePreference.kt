// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.ui.theme

enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM;

    companion object {
        fun fromString(value: String?): ThemePreference = when (value) {
            "LIGHT" -> LIGHT
            "DARK" -> DARK
            else -> SYSTEM
        }
    }
}
