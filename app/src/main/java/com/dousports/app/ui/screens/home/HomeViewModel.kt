package com.dousports.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.data.local.entity.RoutineEntity
import com.dousports.app.data.local.entity.WorkoutSessionEntity
import com.dousports.app.data.preferences.WeeklyGoalPreferenceManager
import com.dousports.app.data.repository.WorkoutRepository
import com.dousports.app.utils.startOfWeekMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val recentSessions: List<WorkoutSessionEntity> = emptyList(),
    val weeklyCount: Int = 0,
    val weeklyVolume: Float = 0f,
    val routines: List<RoutineEntity> = emptyList(),
    val streak: Int = 0,
    val weeklyGoal: Int = 3,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val weeklyGoalPrefs: WeeklyGoalPreferenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                workoutRepository.getRecentSessions(5),
                workoutRepository.getAllRoutines(),
                weeklyGoalPrefs.weeklyGoal
            ) { sessions, routines, goal ->
                Triple(sessions, routines, goal)
            }.collect { (sessions, routines, goal) ->
                val since = startOfWeekMillis()
                val weeklyCount = workoutRepository.countSessionsSince(since)
                val weeklyVolume = workoutRepository.totalVolumeSince(since) ?: 0f
                val allSessions = workoutRepository.getAllSessions().first()
                val streak = computeStreak(allSessions)
                _uiState.value = HomeUiState(
                    recentSessions = sessions,
                    weeklyCount = weeklyCount,
                    weeklyVolume = weeklyVolume,
                    routines = routines,
                    streak = streak,
                    weeklyGoal = goal,
                    isLoading = false
                )
            }
        }
    }

    fun setWeeklyGoal(goal: Int) {
        viewModelScope.launch { weeklyGoalPrefs.setWeeklyGoal(goal) }
    }

    private fun computeStreak(sessions: List<WorkoutSessionEntity>): Int {
        val completedDays = sessions
            .filter { it.finishedAt != null }
            .map { session ->
                val cal = Calendar.getInstance().apply { timeInMillis = session.startedAt }
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            .toSortedSet(compareByDescending { it })

        if (completedDays.isEmpty()) return 0

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayMs = 24 * 60 * 60 * 1000L

        var streak = 0
        var current = if (completedDays.contains(today)) today else today - dayMs
        while (completedDays.contains(current)) {
            streak++
            current -= dayMs
        }
        return streak
    }
}
