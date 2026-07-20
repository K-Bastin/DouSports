package com.dousports.app.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.data.local.entity.BodyMeasurementEntity
import com.dousports.app.data.local.entity.ProgressPhotoEntity
import com.dousports.app.data.repository.ProfileRepository
import com.dousports.app.data.repository.ProgressPhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ProfileUiState(
    val measurements: List<BodyMeasurementEntity> = emptyList(),
    val photos: List<ProgressPhotoEntity> = emptyList(),
    val heightInput: String = "",
    val weightInput: String = "",
    val showAddDialog: Boolean = false,
    val selectedPhoto: ProgressPhotoEntity? = null
) {
    val latest: BodyMeasurementEntity? get() = measurements.firstOrNull()
    val bmi: Float? get() {
        val m = latest ?: return null
        val hm = m.heightCm / 100f
        return if (hm > 0) m.weightKg / (hm * hm) else null
    }
    val weightHistory: List<Pair<Long, Float>> get() =
        measurements.asReversed().map { it.recordedAt to it.weightKg }
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val photoRepository: ProgressPhotoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllMeasurements(),
                photoRepository.getAllPhotos()
            ) { measurements, photos ->
                measurements to photos
            }.collect { (measurements, photos) ->
                _uiState.update { it.copy(measurements = measurements, photos = photos) }
            }
        }
    }

    fun openAddDialog() = _uiState.update { s ->
        s.copy(
            showAddDialog = true,
            heightInput = s.latest?.heightCm?.let { "%.0f".format(it) } ?: "",
            weightInput = ""
        )
    }

    fun dismissDialog() = _uiState.update { it.copy(showAddDialog = false) }

    fun onHeightChange(v: String) = _uiState.update { it.copy(heightInput = v) }
    fun onWeightChange(v: String) = _uiState.update { it.copy(weightInput = v) }

    fun saveMeasurement() {
        val state = _uiState.value
        val h = state.heightInput.toFloatOrNull() ?: return
        val w = state.weightInput.toFloatOrNull() ?: return
        viewModelScope.launch {
            repository.insertMeasurement(BodyMeasurementEntity(heightCm = h, weightKg = w))
            _uiState.update { it.copy(showAddDialog = false) }
        }
    }

    fun deleteMeasurement(m: BodyMeasurementEntity) {
        viewModelScope.launch { repository.deleteMeasurement(m) }
    }

    fun createPhotoFile(): File = photoRepository.createPhotoFile()

    fun savePhotoFromUri(uri: Uri) {
        viewModelScope.launch { photoRepository.savePhotoFromUri(uri) }
    }

    fun savePhotoFromFile(file: File) {
        viewModelScope.launch { photoRepository.savePhotoFromFile(file) }
    }

    fun selectPhoto(photo: ProgressPhotoEntity?) = _uiState.update { it.copy(selectedPhoto = photo) }

    fun deleteSelectedPhoto() {
        val photo = _uiState.value.selectedPhoto ?: return
        viewModelScope.launch {
            photoRepository.deletePhoto(photo)
            _uiState.update { it.copy(selectedPhoto = null) }
        }
    }
}
