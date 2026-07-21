package com.dousports.app.ui.screens.routines

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.data.local.entity.RoutineEntity
import com.dousports.app.data.local.entity.RoutineExerciseEntity
import com.dousports.app.data.repository.WorkoutRepository
import com.dousports.app.utils.RoutineShareDto
import com.dousports.app.utils.RoutineShareManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutinesUiState(
    val routines: List<RoutineEntity> = emptyList(),
    val isLoading: Boolean = true,
    val shareCode: String? = null,
    val shareQr: Bitmap? = null,
    val shareRoutineName: String? = null,
    val importError: String? = null,
    val importPreview: RoutineShareDto? = null
)

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val shareManager: RoutineShareManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutinesUiState())
    val uiState: StateFlow<RoutinesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllRoutines().collect { routines ->
                _uiState.update { it.copy(routines = routines, isLoading = false) }
            }
        }
    }

    fun deleteRoutine(routine: RoutineEntity) {
        viewModelScope.launch { repository.deleteRoutine(routine) }
    }

    fun duplicateRoutine(routineId: Long) {
        viewModelScope.launch { repository.duplicateRoutine(routineId) }
    }

    fun shareRoutine(routineId: Long) {
        viewModelScope.launch {
            val routine = repository.getRoutineById(routineId) ?: return@launch
            val exercises = repository.getExercisesForRoutineSync(routineId)
            val code = shareManager.encode(routine, exercises)
            val qr = shareManager.generateQrBitmap(code)
            _uiState.update { it.copy(shareCode = code, shareQr = qr, shareRoutineName = routine.name) }
        }
    }

    fun clearShare() {
        _uiState.update { it.copy(shareCode = null, shareQr = null, shareRoutineName = null) }
    }

    fun previewImport(code: String) {
        val dto = shareManager.decode(code)
        if (dto == null) {
            _uiState.update { it.copy(importError = "Code invalide ou corrompu.", importPreview = null) }
        } else {
            _uiState.update { it.copy(importPreview = dto, importError = null) }
        }
    }

    fun confirmImport() {
        val dto = _uiState.value.importPreview ?: return
        viewModelScope.launch {
            val routine = RoutineEntity(name = dto.name, description = dto.description)
            val routineId = repository.insertRoutine(routine)
            val exercises = dto.exercises.mapIndexed { idx, e ->
                RoutineExerciseEntity(
                    routineId = routineId,
                    exerciseId = e.exerciseId,
                    exerciseName = e.exerciseName,
                    orderIndex = e.orderIndex.takeIf { it > 0 } ?: idx,
                    targetSets = e.targetSets.coerceAtLeast(1),
                    targetReps = e.targetReps.coerceAtLeast(1),
                    targetWeight = e.targetWeight,
                    restSeconds = e.restSeconds.coerceAtLeast(0)
                )
            }
            repository.saveRoutineExercises(routineId, exercises)
            _uiState.update { it.copy(importPreview = null, importError = null) }
        }
    }

    fun clearImport() {
        _uiState.update { it.copy(importPreview = null, importError = null) }
    }
}
