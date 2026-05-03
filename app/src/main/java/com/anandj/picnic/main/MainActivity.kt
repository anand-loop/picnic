// Copyright (C) 2026 Anand Jesudason
// SPDX-License-Identifier: Apache-2.0

package com.anandj.picnic.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.anandj.picnic.data.DataManagementViewModel
import com.anandj.picnic.ui.theme.PicnicTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val dataViewModel: DataManagementViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        var startDestination by mutableStateOf<String?>(null)
        setContent {
            val themePreference by dataViewModel.themePreference.collectAsState()
            PicnicTheme(themePreference = themePreference) {
                if (startDestination != null) {
                    MainNavHost(startDestination = startDestination!!)
                }
            }
        }
        lifecycleScope.launch {
            startDestination = if (dataViewModel.hasData()) Routes.FEED else Routes.IMPORT
        }
    }
}
