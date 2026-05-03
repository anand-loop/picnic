// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.post

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.anandj.bento.BaseViewModel
import com.anandj.picnic.data.db.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class PostViewModel @Inject constructor(
    private val db: AppDatabase,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<PostContract.State, PostContract.Effect, PostContract.Action>(PostContract.State()) {

    private val postId: Long = checkNotNull(savedStateHandle.get<String>("postId")).toLong()

    init {
        viewModelScope.launch {
            val post = db.postDao().getById(postId)
            updateState { PostContract.State(post = post) }
        }
    }

    override suspend fun onAction(action: PostContract.Action) {
        when (action) {
            PostContract.Action.NavigateBack -> sendEffect(PostContract.Effect.NavigateBack)
        }
    }
}
