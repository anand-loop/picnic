// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.import

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anandj.picnic.R
import com.anandj.picnic.data.repository.ImportState
import com.anandj.picnic.ui.theme.Black
import com.anandj.picnic.ui.theme.BrandTitleStyle

@Composable
fun ImportScreen(importState: ImportState, onImportClick: () -> Unit, onGoToFeedClick: () -> Unit) {
    val isLoading = importState is ImportState.InProgress

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = BrandTitleStyle.copy(fontSize = 32.sp)
            )

            if (isLoading) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator()
            }

            val statusText = when (importState) {
                is ImportState.InProgress -> importState.message
                is ImportState.Done -> pluralStringResource(
                    R.plurals.import_posts_imported,
                    importState.posts,
                    importState.posts
                )
                is ImportState.Error -> importState.cause
                else -> null
            }
            if (statusText != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = statusText,
                    color = Black,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        val (buttonLabel, buttonAction) = when (importState) {
            is ImportState.Done -> stringResource(R.string.import_go_to_feed) to onGoToFeedClick
            else -> stringResource(R.string.import_action) to onImportClick
        }
        Button(
            onClick = buttonAction,
            enabled = !isLoading,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Text(buttonLabel)
        }
    }
}
