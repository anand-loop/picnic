// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.data

import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anandj.picnic.data.repository.ImportState
import com.anandj.picnic.data.repository.InstagramRepository
import com.anandj.picnic.ui.theme.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val repository: InstagramRepository,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val importState: StateFlow<ImportState> = repository.importState

    private val _dataCleared = MutableStateFlow(false)
    val dataCleared: StateFlow<Boolean> = _dataCleared.asStateFlow()

    val themePreference: StateFlow<ThemePreference> = dataStore.data
        .map { prefs -> ThemePreference.fromString(prefs[THEME_KEY]) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemePreference.SYSTEM)

    val gridColumns: StateFlow<Int> = dataStore.data
        .map { prefs -> (prefs[GRID_COLUMNS_KEY] ?: 3).coerceIn(1, 3) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 3)

    suspend fun hasData(): Boolean = repository.hasData()

    fun importZip(uri: Uri) {
        viewModelScope.launch { repository.importFromZip(uri) }
    }

    fun reimportZip(uri: Uri) = importZip(uri)

    fun clearData() {
        viewModelScope.launch {
            repository.clearData()
            _dataCleared.value = true
        }
    }

    fun setThemePreference(pref: ThemePreference) {
        viewModelScope.launch {
            dataStore.edit { it[THEME_KEY] = pref.name }
        }
    }

    fun setGridColumns(columns: Int) {
        viewModelScope.launch {
            dataStore.edit { it[GRID_COLUMNS_KEY] = columns.coerceIn(1, 3) }
        }
    }

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_preference")
        private val GRID_COLUMNS_KEY = intPreferencesKey("grid_columns")
    }
}
