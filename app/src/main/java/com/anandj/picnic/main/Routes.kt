// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.main

object Routes {
    const val IMPORT = "import"
    const val FEED = "feed"
    const val SETTINGS = "settings"
    const val POST = "media/{postId}"
    fun post(postId: Long) = "media/$postId"
    const val REEL = "reel/{reelId}"
    fun reel(reelId: Long) = "reel/$reelId"
    const val STORY_GROUP = "story_group/{dayKey}"
    fun storyGroup(dayKey: String) = "story_group/$dayKey"
}
