package com.dousports.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.BuildConfig
import com.dousports.app.utils.UpdateChecker
import com.dousports.app.utils.UpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateCheckViewModel @Inject constructor(
    private val updateChecker: UpdateChecker
) : ViewModel() {

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo

    init {
        viewModelScope.launch {
            _updateInfo.value = updateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
        }
    }

    fun dismiss() {
        _updateInfo.value = null
    }
}
