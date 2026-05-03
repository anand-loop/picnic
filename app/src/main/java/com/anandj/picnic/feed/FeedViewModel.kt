// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.feed

import com.anandj.bento.PaginatedViewModel
import com.anandj.picnic.data.db.AppDatabase
import com.anandj.picnic.data.db.relation.PostWithMedia
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.ceil

private const val PAGE_SIZE = 30

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val db: AppDatabase
) : PaginatedViewModel<PostWithMedia, FeedContract.Effect, FeedContract.Action>() {

    init {
        loadInitial()
    }

    override suspend fun fetchPage(page: Int): Result<Pair<List<PostWithMedia>, Int>> {
        return try {
            val offset = (page - 1) * PAGE_SIZE
            val posts = db.postDao().getPage(PAGE_SIZE, offset)
            val total = db.postDao().getCount()
            val totalPages = if (total == 0) 1 else ceil(total.toDouble() / PAGE_SIZE).toInt()
            Result.success(posts to totalPages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun onAction(action: FeedContract.Action) {
        when (action) {
            FeedContract.Action.Reload -> loadInitial()
            FeedContract.Action.Refresh -> refresh()
            FeedContract.Action.LoadMore -> loadNextPage()
            is FeedContract.Action.OpenPost -> sendEffect(FeedContract.Effect.NavigateToPost(action.postId))
            FeedContract.Action.OpenSettings -> sendEffect(FeedContract.Effect.NavigateToSettings)
        }
    }
}
