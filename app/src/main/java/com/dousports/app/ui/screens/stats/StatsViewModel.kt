package com.dousports.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.data.local.entity.WorkoutSessionEntity
import com.dousports.app.data.repository.ExerciseRepository
import com.dousports.app.data.repository.WorkoutRepository
import com.dousports.app.utils.startOfMonthMillis
import com.dousports.app.utils.startOfWeekMillis
import java.util.concurrent.TimeUnit
import com.dousports.app.utils.toFrBodyPart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonalRecord(
    val exerciseId: String,
    val exerciseName: String,
    val maxWeight: Float
)

data class ExerciseStats(
    val exerciseName: String,
    val totalSets: Int,
    val totalReps: Int,
    val totalVolume: Float
)

data class MuscleGroupStats(
    val name: String,
    val totalSets: Int,
    val totalVolume: Float,
    val percentage: Float,
    val colorIndex: Int = 0
)

data class StatsUiState(
    val weeklyCount: Int = 0,
    val monthlyCount: Int = 0,
    val weeklyVolume: Float = 0f,
    val monthlyVolume: Float = 0f,
    val totalSessions: Int = 0,
    val personalRecords: List<PersonalRecord> = emptyList(),
    val recentSessions: List<WorkoutSessionEntity> = emptyList(),
    val topExercises: List<ExerciseStats> = emptyList(),
    val muscleGroups: List<MuscleGroupStats> = emptyList(),
    val totalSetsAllTime: Int = 0,
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

                val allSets = workoutRepository.getAllSets()
                val totalSetsAllTime = allSets.size

                val topExercises = allSets
                    .groupBy { it.exerciseName }
                    .map { (name, sets) ->
                        ExerciseStats(
                            exerciseName = name,
                            totalSets = sets.size,
                            totalReps = sets.sumOf { it.reps },
                            totalVolume = sets.sumOf { (it.reps * it.weight).toDouble() }.toFloat()
                        )
                    }
                    .sortedByDescending { it.totalSets }
                    .take(8)

                val exerciseMap = exerciseRepository
                    .getExercisesByIds(allSets.map { it.exerciseId }.distinct())
                    .associateBy { it.id }

                val since28Days = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(28)
                val recentSets = workoutRepository.getAllSetsSince(since28Days)
                val recentExerciseMap = exerciseRepository
                    .getExercisesByIds(recentSets.map { it.exerciseId }.distinct())
                    .associateBy { it.id }

                val muscleGroups = recentSets
                    .groupBy { recentExerciseMap[it.exerciseId]?.bodyPart ?: "other" }
                    .map { (bodyPart, sets) ->
                        bodyPart to Pair(sets.size, sets.sumOf { (it.reps * it.weight).toDouble() }.toFloat())
                    }
                    .sortedByDescending { it.second.second }
                    .let { list ->
                        val totalVolume = list.sumOf { it.second.second.toDouble() }.toFloat().coerceAtLeast(1f)
                        list.mapIndexed { index, (bodyPart, pair) ->
                            MuscleGroupStats(
                                name = bodyPart.toFrBodyPart(),
                                totalSets = pair.first,
                                totalVolume = pair.second,
                                percentage = pair.second / totalVolume,
                                colorIndex = index
                            )
                        }
                    }

                _uiState.update {
                    it.copy(
                        weeklyCount = weeklyCount,
                        monthlyCount = monthlyCount,
                        weeklyVolume = weeklyVolume,
                        monthlyVolume = monthlyVolume,
                        totalSessions = sessions.count { s -> s.finishedAt != null },
                        personalRecords = records,
                        recentSessions = sessions.take(20),
                        topExercises = topExercises,
                        muscleGroups = muscleGroups,
                        totalSetsAllTime = totalSetsAllTime,
                        isLoading = false
                    )
                }
            }
        }
    }
}
