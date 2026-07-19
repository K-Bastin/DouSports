package com.dousports.app.ui.screens.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.data.local.entity.RoutineEntity
import com.dousports.app.data.local.entity.RoutineExerciseEntity
import com.dousports.app.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutinesUiState(
    val routines: List<RoutineEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    private val repository: WorkoutRepository
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
}
