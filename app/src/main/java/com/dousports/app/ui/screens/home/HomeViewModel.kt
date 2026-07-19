package com.dousports.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.data.local.entity.RoutineEntity
import com.dousports.app.data.local.entity.WorkoutSessionEntity
import com.dousports.app.data.repository.WorkoutRepository
import com.dousports.app.utils.startOfWeekMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentSessions: List<WorkoutSessionEntity> = emptyList(),
    val weeklyCount: Int = 0,
    val weeklyVolume: Float = 0f,
    val routines: List<RoutineEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                workoutRepository.getRecentSessions(5),
                workoutRepository.getAllRoutines()
            ) { sessions, routines ->
                Pair(sessions, routines)
            }.collect { (sessions, routines) ->
                val since = startOfWeekMillis()
                val weeklyCount = workoutRepository.countSessionsSince(since)
                val weeklyVolume = workoutRepository.totalVolumeSince(since) ?: 0f

                _uiState.update {
                    it.copy(
                        recentSessions = sessions,
                        weeklyCount = weeklyCount,
                        weeklyVolume = weeklyVolume,
                        routines = routines,
                        isLoading = false
                    )
                }
            }
        }
    }
}
