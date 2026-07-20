package com.dousports.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.data.preferences.ThemePreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePreferenceManager: ThemePreferenceManager
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = themePreferenceManager.isDarkTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )

    fun toggleTheme() {
        viewModelScope.launch {
            themePreferenceManager.setDarkTheme(!isDarkTheme.value)
        }
    }

    fun setDarkTheme(dark: Boolean) {
        viewModelScope.launch {
            themePreferenceManager.setDarkTheme(dark)
        }
    }
}
