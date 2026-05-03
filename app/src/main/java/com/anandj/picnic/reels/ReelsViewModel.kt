// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.reels

import com.anandj.bento.PaginatedViewModel
import com.anandj.picnic.data.db.AppDatabase
import com.anandj.picnic.data.db.entity.ReelEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.ceil

private const val PAGE_SIZE = 30

@HiltViewModel
class ReelsViewModel @Inject constructor(
    private val db: AppDatabase
) : PaginatedViewModel<ReelEntity, ReelsContract.Effect, ReelsContract.Action>() {

    init {
        loadInitial()
    }

    override suspend fun fetchPage(page: Int): Result<Pair<List<ReelEntity>, Int>> {
        return try {
            val offset = (page - 1) * PAGE_SIZE
            val reels = db.reelDao().getPage(PAGE_SIZE, offset)
            val total = db.reelDao().getCount()
            val totalPages = if (total == 0) 1 else ceil(total.toDouble() / PAGE_SIZE).toInt()
            Result.success(reels to totalPages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun onAction(action: ReelsContract.Action) {
        when (action) {
            ReelsContract.Action.Reload -> loadInitial()
            ReelsContract.Action.LoadMore -> loadNextPage()
            is ReelsContract.Action.OpenReel -> sendEffect(ReelsContract.Effect.NavigateToReel(action.reelId))
            ReelsContract.Action.OpenSettings -> sendEffect(ReelsContract.Effect.NavigateToSettings)
        }
    }
}
