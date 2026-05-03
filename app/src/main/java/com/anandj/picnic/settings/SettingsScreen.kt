// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anandj.picnic.data.DataManagementViewModel
import com.anandj.picnic.data.repository.ImportState
import com.anandj.picnic.ui.theme.ThemePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: DataManagementViewModel, onNavigateBack: () -> Unit, onOperationComplete: () -> Unit) {
    val importState by viewModel.importState.collectAsState()
    val dataCleared by viewModel.dataCleared.collectAsState()
    val themePreference by viewModel.themePreference.collectAsState()
    val gridColumns by viewModel.gridColumns.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(dataCleared) {
        if (dataCleared) onOperationComplete()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ThemeSettingsItem(
                    current = themePreference,
                    onSelect = { viewModel.setThemePreference(it) }
                )
                HorizontalDivider()
                GridColumnsSettingsItem(
                    current = gridColumns,
                    onSelect = { viewModel.setGridColumns(it) }
                )
                HorizontalDivider()
                SettingsItem(
                    title = "Clear data",
                    subtitle = "Remove all imported data",
                    enabled = importState !is ImportState.InProgress,
                    onClick = { showClearConfirm = true }
                )
                HorizontalDivider()

                if (importState is ImportState.InProgress) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text(
                                text = (importState as ImportState.InProgress).message,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }

                if (importState is ImportState.Error) {
                    Text(
                        text = "Error: ${(importState as ImportState.Error).cause}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear data") },
            text = { Text("This will remove all imported data. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    viewModel.clearData()
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSettingsItem(current: ThemePreference, onSelect: (ThemePreference) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text("Theme", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemePreference.entries.forEachIndexed { index, pref ->
                SegmentedButton(
                    selected = current == pref,
                    onClick = { onSelect(pref) },
                    shape = SegmentedButtonDefaults.itemShape(index, ThemePreference.entries.size),
                    label = { Text(pref.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GridColumnsSettingsItem(current: Int, onSelect: (Int) -> Unit) {
    val options = listOf(1, 2, 3)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text("Grid columns", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, columns ->
                SegmentedButton(
                    selected = current == columns,
                    onClick = { onSelect(columns) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    label = { Text("$columns") }
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(title: String, subtitle: String, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.let {
                if (enabled) it else it.copy(alpha = 0.38f)
            }
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
