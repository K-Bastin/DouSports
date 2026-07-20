package com.dousports.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.BuildConfig
import com.dousports.app.utils.UpdateChecker
import com.dousports.app.utils.UpdateInfo
import com.dousports.app.utils.UpdateInstaller
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateCheckViewModel @Inject constructor(
    private val updateChecker: UpdateChecker,
    private val installer: UpdateInstaller
) : ViewModel() {

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo

    private val _needInstallPermission = MutableStateFlow(false)
    val needInstallPermission: StateFlow<Boolean> = _needInstallPermission

    init {
        viewModelScope.launch {
            _updateInfo.value = updateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
        }
        // Retry any install that was waiting for the permission
        installer.retryPendingInstall()
    }

    fun download() {
        val info = _updateInfo.value ?: return
        if (!installer.canInstallPackages()) {
            _needInstallPermission.value = true
            dismiss()
            return
        }
        installer.download(info.downloadUrl, info.latestVersion)
        dismiss()
    }

    fun openInstallPermissionSettings() {
        installer.openInstallPermissionSettings()
        _needInstallPermission.value = false
    }

    fun dismissPermission() {
        _needInstallPermission.value = false
    }

    fun dismiss() {
        _updateInfo.value = null
    }
}
