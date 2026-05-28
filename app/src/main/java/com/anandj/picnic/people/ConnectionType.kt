// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.people

import androidx.annotation.StringRes
import com.anandj.picnic.R

/**
 * The two relationship directions stored in the shared `connections` table.
 *
 * Each toggle option maps to its [dbValue] (the `ConnectionEntity.type`
 * discriminator the DAO filters on) and the [labelRes] shown on the segmented
 * button. Keeping the string literal here — instead of scattered through the
 * ViewModel and queries — is the single source of truth for the discriminator.
 */
enum class ConnectionType(val dbValue: String, @StringRes val labelRes: Int) {
    FOLLOWERS("follower", R.string.people_followers),
    FOLLOWING("following", R.string.people_following),
}
