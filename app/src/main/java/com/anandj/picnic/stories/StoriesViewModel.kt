// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.stories

import com.anandj.bento.PaginatedViewModel
import com.anandj.picnic.data.db.AppDatabase
import com.anandj.picnic.data.db.relation.StoryGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.ceil

private const val PAGE_SIZE = 30

@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val db: AppDatabase
) : PaginatedViewModel<StoryGroup, StoriesContract.Effect, StoriesContract.Action>() {

    init {
        loadInitial()
    }

    override suspend fun fetchPage(page: Int): Result<Pair<List<StoryGroup>, Int>> {
        return try {
            val offset = (page - 1) * PAGE_SIZE
            val groups = db.storyDao().getGroupedPage(PAGE_SIZE, offset)
            val total = db.storyDao().getGroupCount()
            val totalPages = if (total == 0) 1 else ceil(total.toDouble() / PAGE_SIZE).toInt()
            Result.success(groups to totalPages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun onAction(action: StoriesContract.Action) {
        when (action) {
            StoriesContract.Action.Reload -> loadInitial()
            StoriesContract.Action.LoadMore -> loadNextPage()
            StoriesContract.Action.OpenSettings -> sendEffect(StoriesContract.Effect.NavigateToSettings)
            is StoriesContract.Action.OpenGroup -> sendEffect(StoriesContract.Effect.NavigateToGroup(action.dayKey))
        }
    }
}
