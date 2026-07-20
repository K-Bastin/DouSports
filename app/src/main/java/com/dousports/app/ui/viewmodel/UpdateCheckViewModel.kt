package com.dousports.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.BuildConfig
import com.dousports.app.utils.UpdateChecker
import com.dousports.app.utils.UpdateInfo
import com.dousports.app.utils.UpdateInstaller
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DownloadState { IDLE, DOWNLOADING, DOWNLOADED, ERROR }

@HiltViewModel
class UpdateCheckViewModel @Inject constructor(
    private val updateChecker: UpdateChecker,
    private val installer: UpdateInstaller
) : ViewModel() {

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog

    private val _downloadState = MutableStateFlow(DownloadState.IDLE)
    val downloadState: StateFlow<DownloadState> = _downloadState

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    private val _needInstallPermission = MutableStateFlow(false)
    val needInstallPermission: StateFlow<Boolean> = _needInstallPermission

    private var activeDownloadId = -1L

    init {
        viewModelScope.launch {
            val info = updateChecker.checkForUpdate(BuildConfig.VERSION_NAME)
            _updateInfo.value = info
            if (info != null) _showUpdateDialog.value = true
        }
        installer.retryPendingInstall()
    }

    fun startDownload() {
        val info = _updateInfo.value ?: return
        if (_downloadState.value == DownloadState.DOWNLOADING) return
        if (!installer.canInstallPackages()) {
            _needInstallPermission.value = true
            return
        }
        _downloadState.value = DownloadState.DOWNLOADING
        _downloadProgress.value = 0f
        activeDownloadId = installer.download(info.downloadUrl, info.latestVersion)

        viewModelScope.launch {
            while (_downloadState.value == DownloadState.DOWNLOADING) {
                delay(500)
                when (val p = installer.getDownloadProgress(activeDownloadId)) {
                    null -> { /* not yet started, keep polling */ }
                    -1f  -> _downloadState.value = DownloadState.ERROR
                    1f   -> {
                        _downloadProgress.value = 1f
                        _downloadState.value = DownloadState.DOWNLOADED
                    }
                    else -> _downloadProgress.value = p
                }
            }
        }
    }

    fun install() {
        val id = if (activeDownloadId != -1L) activeDownloadId else installer.getPendingId()
        installer.installOnCompletion(id)
    }

    fun retryDownload() {
        _downloadState.value = DownloadState.IDLE
        _downloadProgress.value = 0f
        startDownload()
    }

    fun openUninstall() = installer.openUninstall()

    fun openInstallPermissionSettings() {
        installer.openInstallPermissionSettings()
        _needInstallPermission.value = false
    }

    fun dismissPermission() {
        _needInstallPermission.value = false
    }

    fun dismissDialog() {
        _showUpdateDialog.value = false
    }

    fun dismiss() {
        _updateInfo.value = null
        _showUpdateDialog.value = false
        _downloadState.value = DownloadState.IDLE
        _downloadProgress.value = 0f
    }
}
