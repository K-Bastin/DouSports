package com.dousports.app.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.data.local.entity.RoutineExerciseEntity
import com.dousports.app.data.local.entity.WorkoutSessionEntity
import com.dousports.app.data.local.entity.WorkoutSetEntity
import com.dousports.app.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoggedSet(
    val setNumber: Int,
    val reps: Int,
    val weight: Float
)

data class ExerciseWorkoutState(
    val routineExercise: RoutineExerciseEntity,
    val loggedSets: List<LoggedSet> = emptyList(),
    val previousSets: List<WorkoutSetEntity> = emptyList(),
    val personalRecord: Float? = null
)

data class ActiveWorkoutUiState(
    val routineId: Long = 0,
    val routineName: String = "",
    val exercises: List<ExerciseWorkoutState> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val elapsedSeconds: Long = 0L,
    val sessionId: Long? = null,
    val sessionStartedAt: Long = 0L,
    val isLoading: Boolean = true,
    val isFinished: Boolean = false,
    val prBeatenExerciseName: String? = null,
    val restTimerTotalSeconds: Int = 90,
    val restTimerRemaining: Int? = null,
    val restTimerFinished: Boolean = false
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var restTimerJob: Job? = null

    fun loadRoutine(routineId: Long) {
        viewModelScope.launch {
            val routine = repository.getRoutineById(routineId) ?: return@launch
            val routineExercises = repository.getExercisesForRoutineSync(routineId)

            val startedAt = System.currentTimeMillis()
            val sessionId = repository.insertSession(
                WorkoutSessionEntity(
                    routineId = routineId,
                    routineName = routine.name,
                    startedAt = startedAt
                )
            )

            val exerciseStates = routineExercises.map { re ->
                val prev = repository.getRecentSetsForExercise(re.exerciseId, 10)
                val pr = repository.maxWeightForExercise(re.exerciseId)
                ExerciseWorkoutState(routineExercise = re, previousSets = prev, personalRecord = pr)
            }

            _uiState.update {
                it.copy(
                    routineId = routineId,
                    routineName = routine.name,
                    exercises = exerciseStates,
                    sessionId = sessionId,
                    sessionStartedAt = startedAt,
                    isLoading = false
                )
            }

            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    fun setCurrentExercise(index: Int) =
        _uiState.update { it.copy(currentExerciseIndex = index) }

    fun logSet(exerciseIndex: Int, reps: Int, weight: Float) {
        val current = _uiState.value.exercises[exerciseIndex]
        val setNumber = current.loggedSets.size + 1
        val newSet = LoggedSet(setNumber, reps, weight)
        val prBeaten = weight > 0f && (current.personalRecord == null || weight > current.personalRecord)

        _uiState.update {
            val updated = it.exercises.toMutableList()
            val newPr = if (prBeaten) weight else current.personalRecord
            updated[exerciseIndex] = current.copy(loggedSets = current.loggedSets + newSet, personalRecord = newPr)
            it.copy(
                exercises = updated,
                prBeatenExerciseName = if (prBeaten) current.routineExercise.exerciseName else it.prBeatenExerciseName
            )
        }

        // Persist to database
        viewModelScope.launch {
            val state = _uiState.value
            val sessionId = state.sessionId ?: return@launch
            repository.insertSet(
                WorkoutSetEntity(
                    sessionId = sessionId,
                    exerciseId = current.routineExercise.exerciseId,
                    exerciseName = current.routineExercise.exerciseName,
                    setNumber = setNumber,
                    reps = reps,
                    weight = weight
                )
            )
        }

        // Start rest timer
        startRestTimer(current.routineExercise.restSeconds.takeIf { it > 0 } ?: _uiState.value.restTimerTotalSeconds)
    }

    private fun startRestTimer(durationSeconds: Int) {
        restTimerJob?.cancel()
        _uiState.update { it.copy(restTimerTotalSeconds = durationSeconds, restTimerRemaining = durationSeconds, restTimerFinished = false) }
        restTimerJob = viewModelScope.launch {
            var remaining = durationSeconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.update { it.copy(restTimerRemaining = remaining) }
            }
            _uiState.update { it.copy(restTimerRemaining = null, restTimerFinished = true) }
        }
    }

    fun skipRestTimer() {
        restTimerJob?.cancel()
        _uiState.update { it.copy(restTimerRemaining = null, restTimerFinished = false) }
    }

    fun clearRestTimerFinished() {
        _uiState.update { it.copy(restTimerFinished = false) }
    }

    fun clearPrBeaten() {
        _uiState.update { it.copy(prBeatenExerciseName = null) }
    }

    fun removeLastSet(exerciseIndex: Int) {
        val current = _uiState.value.exercises[exerciseIndex]
        if (current.loggedSets.isEmpty()) return
        _uiState.update {
            val updated = it.exercises.toMutableList()
            updated[exerciseIndex] = current.copy(loggedSets = current.loggedSets.dropLast(1))
            it.copy(exercises = updated)
        }
    }

    fun finishWorkout(onDone: () -> Unit) {
        timerJob?.cancel()
        viewModelScope.launch {
            val state = _uiState.value
            val sessionId = state.sessionId ?: return@launch
            val duration = state.elapsedSeconds

            repository.updateSession(
                WorkoutSessionEntity(
                    id = sessionId,
                    routineId = state.routineId,
                    routineName = state.routineName,
                    startedAt = state.sessionStartedAt,
                    finishedAt = System.currentTimeMillis(),
                    durationSeconds = duration
                )
            )

            // Update routine's lastPerformedAt
            repository.getRoutineById(state.routineId)?.let { routine ->
                repository.updateRoutine(
                    routine.copy(lastPerformedAt = System.currentTimeMillis())
                )
            }

            _uiState.update { it.copy(isFinished = true) }
            onDone()
        }
    }

    fun cancelWorkout(onDone: () -> Unit) {
        timerJob?.cancel()
        viewModelScope.launch {
            val sessionId = _uiState.value.sessionId
            if (sessionId != null) {
                val session = repository.getSessionById(sessionId)
                session?.let { repository.deleteSession(it) }
            }
            onDone()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        restTimerJob?.cancel()
    }
}
