package com.dousports.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.data.local.entity.WorkoutSessionEntity
import com.dousports.app.data.repository.ExerciseRepository
import com.dousports.app.data.repository.WorkoutRepository
import com.dousports.app.utils.startOfMonthMillis
import com.dousports.app.utils.startOfWeekMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonalRecord(
    val exerciseId: String,
    val exerciseName: String,
    val maxWeight: Float
)

data class StatsUiState(
    val weeklyCount: Int = 0,
    val monthlyCount: Int = 0,
    val weeklyVolume: Float = 0f,
    val monthlyVolume: Float = 0f,
    val totalSessions: Int = 0,
    val personalRecords: List<PersonalRecord> = emptyList(),
    val recentSessions: List<WorkoutSessionEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            workoutRepository.getAllSessions().collect { sessions ->
                val weekStart = startOfWeekMillis()
                val monthStart = startOfMonthMillis()

                val weeklyCount = workoutRepository.countSessionsSince(weekStart)
                val monthlyCount = workoutRepository.countSessionsSince(monthStart)
                val weeklyVolume = workoutRepository.totalVolumeSince(weekStart) ?: 0f
                val monthlyVolume = workoutRepository.totalVolumeSince(monthStart) ?: 0f

                val trackedIds = workoutRepository.getAllTrackedExerciseIds()
                val records = trackedIds.mapNotNull { id ->
                    val maxWeight = workoutRepository.maxWeightForExercise(id) ?: return@mapNotNull null
                    val exercise = exerciseRepository.getExerciseById(id)
                    val name = exercise?.name ?: id
                    PersonalRecord(id, name, maxWeight)
                }.sortedByDescending { it.maxWeight }.take(10)

                _uiState.update {
                    it.copy(
                        weeklyCount = weeklyCount,
                        monthlyCount = monthlyCount,
                        weeklyVolume = weeklyVolume,
                        monthlyVolume = monthlyVolume,
                        totalSessions = sessions.count { s -> s.finishedAt != null },
                        personalRecords = records,
                        recentSessions = sessions.take(20),
                        isLoading = false
                    )
                }
            }
        }
    }
}
