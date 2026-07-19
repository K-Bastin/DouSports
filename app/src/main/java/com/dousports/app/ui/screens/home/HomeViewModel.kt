package com.dousports.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.data.local.entity.RoutineEntity
import com.dousports.app.data.local.entity.WorkoutSessionEntity
import com.dousports.app.data.repository.WorkoutRepository
import com.dousports.app.utils.startOfWeekMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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

    val uiState: StateFlow<HomeUiState> = combine(
        workoutRepository.getRecentSessions(5),
        workoutRepository.getAllRoutines()
    ) { sessions, routines ->
        val since = startOfWeekMillis()
        val weeklyCount = workoutRepository.countSessionsSince(since)
        val weeklyVolume = workoutRepository.totalVolumeSince(since) ?: 0f
        HomeUiState(
            recentSessions = sessions,
            weeklyCount = weeklyCount,
            weeklyVolume = weeklyVolume,
            routines = routines,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}
