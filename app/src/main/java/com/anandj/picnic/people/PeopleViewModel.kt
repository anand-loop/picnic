// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.people

import com.anandj.bento.PaginatedViewModel
import com.anandj.picnic.data.db.AppDatabase
import com.anandj.picnic.data.db.entity.ConnectionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.ceil

private const val PAGE_SIZE = 30

@HiltViewModel
class PeopleViewModel @Inject constructor(
    private val db: AppDatabase
) : PaginatedViewModel<ConnectionEntity, PeopleContract.Effect, PeopleContract.Action>() {

    // Which relationship direction the paginated query targets. fetchPage reads
    // this at execution time, so switching it (SelectType) + loadInitial() is
    // enough to repaginate the other side from page 1.
    private var type: ConnectionType = ConnectionType.FOLLOWERS

    init {
        loadInitial()
    }

    override suspend fun fetchPage(page: Int): Result<Pair<List<ConnectionEntity>, Int>> {
        return try {
            val offset = (page - 1) * PAGE_SIZE
            val items = db.connectionDao().getPage(type.dbValue, PAGE_SIZE, offset)
            val total = db.connectionDao().getCount(type.dbValue)
            val totalPages = if (total == 0) 1 else ceil(total.toDouble() / PAGE_SIZE).toInt()
            Result.success(items to totalPages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun onAction(action: PeopleContract.Action) {
        when (action) {
            PeopleContract.Action.Reload -> loadInitial()
            PeopleContract.Action.LoadMore -> loadNextPage()
            PeopleContract.Action.OpenSettings -> sendEffect(PeopleContract.Effect.NavigateToSettings)
            is PeopleContract.Action.SelectType -> {
                if (action.type != type) {
                    type = action.type
                    loadInitial()
                }
            }
        }
    }
}
