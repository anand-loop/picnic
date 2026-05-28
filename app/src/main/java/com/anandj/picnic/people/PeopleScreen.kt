// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.people

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.anandj.bento.PaginatedList
import com.anandj.bento.PaginatedListLayout
import com.anandj.picnic.R
import com.anandj.picnic.data.db.entity.ConnectionEntity
import com.anandj.picnic.ui.PicnicTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    viewModel: PeopleViewModel,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val state by viewModel.state.collectAsState()
    // The toggle is pure UI state; the VM holds the matching `type` and only
    // repaginates when SelectType reports a change. Both default to FOLLOWERS.
    var selectedType by rememberSaveable { mutableStateOf(ConnectionType.FOLLOWERS) }

    Scaffold(
        topBar = {
            PicnicTopAppBar(
                onSettingsClick = { viewModel.sendAction(PeopleContract.Action.OpenSettings) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                ConnectionType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = {
                            if (selectedType != type) {
                                selectedType = type
                                viewModel.sendAction(PeopleContract.Action.SelectType(type))
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, ConnectionType.entries.size),
                        label = { Text(stringResource(type.labelRes)) }
                    )
                }
            }

            PaginatedList(
                state = state,
                onLoadInitial = { viewModel.sendAction(PeopleContract.Action.Reload) },
                onLoadMore = { viewModel.sendAction(PeopleContract.Action.LoadMore) },
                itemKey = { it.id },
                itemContent = { connection -> ConnectionRow(connection) },
                layout = PaginatedListLayout.List(itemSpacing = 0.dp),
                emptyContent = { EmptyPeopleState(selectedType) },
                bottomPadding = 80.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
            )
        }
    }
}

@Composable
private fun ConnectionRow(item: ConnectionEntity) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.href.isNullOrBlank()) {
                val href = item.href ?: return@clickable
                // href is the profile URL; open it externally like PostScreen's location link.
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, href.toUri())) }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "@${item.username}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        val subtitle = relativeTime(item.timestamp)
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyPeopleState(type: ConnectionType) {
    val message = when (type) {
        ConnectionType.FOLLOWERS -> R.string.people_empty_followers
        ConnectionType.FOLLOWING -> R.string.people_empty_following
    }
    Text(
        text = stringResource(message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(32.dp)
    )
}

@Composable
private fun relativeTime(epochSeconds: Long): String = remember(epochSeconds) {
    if (epochSeconds <= 0L) {
        ""
    } else {
        // ConnectionEntity.timestamp is Unix seconds; getRelativeTimeSpanString wants millis.
        DateUtils.getRelativeTimeSpanString(
            epochSeconds * 1000L,
            System.currentTimeMillis(),
            DateUtils.DAY_IN_MILLIS
        ).toString()
    }
}
