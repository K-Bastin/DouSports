package com.dousports.app.ui.screens.workout

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.data.local.entity.RoutineExerciseEntity
import com.dousports.app.data.local.entity.WorkoutSessionEntity
import com.dousports.app.data.preferences.SavedTimerState
import com.dousports.app.data.preferences.TimerStateStore
import com.dousports.app.data.repository.WorkoutRepository
import com.dousports.app.services.WorkoutForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TimedPhase { READY, EXERCISE, REST, FINISHED }

data class TimedWorkoutUiState(
    val routineId: Long = 0,
    val routineName: String = "",
    val routineColor: Int = 0,
    val exercises: List<RoutineExerciseEntity> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val phase: TimedPhase = TimedPhase.READY,
    val phaseRemaining: Int = 0,
    val phaseDuration: Int = 0,
    val totalElapsedSeconds: Long = 0L,
    val isPaused: Boolean = false,
    val isLoading: Boolean = true,
    val sessionId: Long? = null,
    val sessionStartedAt: Long = 0L,
    val shouldVibrate: Boolean = false
)

@HiltViewModel
class TimedWorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val timerStateStore: TimerStateStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimedWorkoutUiState())
    val uiState: StateFlow<TimedWorkoutUiState> = _uiState.asStateFlow()

    private var phaseJob: Job? = null
    private var globalTimerJob: Job? = null

    fun loadRoutine(routineId: Long) {
        viewModelScope.launch {
            val routine = repository.getRoutineById(routineId) ?: return@launch
            val exercises = repository.getExercisesForRoutineSync(routineId)
            val existing = repository.getUnfinishedSessionForRoutine(routineId)
            val startedAt: Long
            val sessionId: Long
            if (existing != null) {
                startedAt = existing.startedAt
                sessionId = existing.id
            } else {
                startedAt = System.currentTimeMillis()
                sessionId = repository.insertSession(
                    WorkoutSessionEntity(
                        routineId = routineId,
                        routineName = routine.name,
                        startedAt = startedAt,
                        routineColor = routine.color
                    )
                )
            }

            val saved = if (existing != null) timerStateStore.load(sessionId) else null
            val firstDuration = exercises.firstOrNull()?.durationSeconds ?: 45

            if (saved != null) {
                val restoredPhase = TimedPhase.entries.getOrElse(saved.phaseOrdinal) { TimedPhase.READY }
                val restoredIndex = saved.exerciseIndex.coerceIn(0, (exercises.size - 1).coerceAtLeast(0))
                val restoredRemaining = saved.phaseRemainingSeconds.coerceAtLeast(1)
                val phaseDuration = when (restoredPhase) {
                    TimedPhase.EXERCISE -> exercises.getOrNull(restoredIndex)?.durationSeconds ?: 45
                    TimedPhase.REST -> exercises.getOrNull((restoredIndex - 1).coerceAtLeast(0))?.restSeconds?.takeIf { it > 0 } ?: 30
                    else -> firstDuration
                }
                _uiState.update {
                    it.copy(
                        routineId = routineId,
                        routineName = routine.name,
                        routineColor = routine.color,
                        exercises = exercises,
                        currentExerciseIndex = restoredIndex,
                        phase = restoredPhase,
                        phaseRemaining = restoredRemaining,
                        phaseDuration = phaseDuration,
                        sessionId = sessionId,
                        sessionStartedAt = startedAt,
                        isLoading = false,
                        isPaused = true
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        routineId = routineId,
                        routineName = routine.name,
                        routineColor = routine.color,
                        exercises = exercises,
                        phase = TimedPhase.READY,
                        phaseRemaining = firstDuration,
                        phaseDuration = firstDuration,
                        sessionId = sessionId,
                        sessionStartedAt = startedAt,
                        isLoading = false
                    )
                }
            }

            ContextCompat.startForegroundService(context, WorkoutForegroundService.startIntent(context))
        }
    }

    fun startOrResume() {
        val state = _uiState.value
        if (state.phase == TimedPhase.FINISHED) return

        if (state.isPaused) {
            _uiState.update { it.copy(isPaused = false) }
            startPhaseCountdown()
            if (globalTimerJob?.isActive != true) startGlobalTimer()
            return
        }

        if (state.phase == TimedPhase.READY) {
            _uiState.update { it.copy(isPaused = false) }
            startPhaseCountdown()
            startGlobalTimer()
        }
    }

    fun pause() {
        phaseJob?.cancel()
        globalTimerJob?.cancel()
        _uiState.update { it.copy(isPaused = true) }
        viewModelScope.launch { persistTimerState(_uiState.value) }
    }

    fun skipPhase() {
        phaseJob?.cancel()
        advancePhaseState()
        val state = _uiState.value
        if (state.phase != TimedPhase.FINISHED) {
            viewModelScope.launch { persistTimerState(state) }
            if (!state.isPaused) startPhaseCountdown()
        }
    }

    fun clearVibrate() {
        _uiState.update { it.copy(shouldVibrate = false) }
    }

    // Single continuous loop — no self-cancellation.
    private fun startPhaseCountdown() {
        phaseJob?.cancel()
        phaseJob = viewModelScope.launch {
            var tickCount = 0
            while (true) {
                delay(1000)
                val state = _uiState.value
                if (state.isPaused || state.phase == TimedPhase.FINISHED) break
                val remaining = state.phaseRemaining - 1
                if (remaining <= 0) {
                    _uiState.update { it.copy(phaseRemaining = 0, shouldVibrate = true) }
                    advancePhaseState()
                    val newState = _uiState.value
                    if (newState.phase == TimedPhase.FINISHED) break
                    persistTimerState(newState)
                    tickCount = 0
                } else {
                    _uiState.update { it.copy(phaseRemaining = remaining) }
                    tickCount++
                    if (tickCount % 5 == 0) persistTimerState(_uiState.value)
                }
            }
        }
    }

    // Pure state transition — never launches coroutines or cancels jobs.
    private fun advancePhaseState() {
        val state = _uiState.value
        when (state.phase) {
            TimedPhase.READY, TimedPhase.REST -> {
                val duration = state.exercises.getOrNull(state.currentExerciseIndex)?.durationSeconds ?: 45
                _uiState.update {
                    it.copy(phase = TimedPhase.EXERCISE, phaseRemaining = duration, phaseDuration = duration)
                }
            }
            TimedPhase.EXERCISE -> {
                val nextIndex = state.currentExerciseIndex + 1
                if (nextIndex >= state.exercises.size) {
                    finishWorkout()
                } else {
                    val restDuration = state.exercises[state.currentExerciseIndex].restSeconds.takeIf { it > 0 } ?: 30
                    _uiState.update {
                        it.copy(
                            phase = TimedPhase.REST,
                            currentExerciseIndex = nextIndex,
                            phaseRemaining = restDuration,
                            phaseDuration = restDuration
                        )
                    }
                }
            }
            TimedPhase.FINISHED -> {}
        }
    }

    private fun startGlobalTimer() {
        globalTimerJob?.cancel()
        globalTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_uiState.value.isPaused) {
                    _uiState.update { it.copy(totalElapsedSeconds = it.totalElapsedSeconds + 1) }
                }
            }
        }
    }

    private fun finishWorkout() {
        globalTimerJob?.cancel()
        context.startService(WorkoutForegroundService.stopIntent(context))
        val state = _uiState.value
        val sessionId = state.sessionId ?: return
        viewModelScope.launch {
            timerStateStore.clear()
            repository.updateSession(
                WorkoutSessionEntity(
                    id = sessionId,
                    routineId = state.routineId,
                    routineName = state.routineName,
                    startedAt = state.sessionStartedAt,
                    finishedAt = System.currentTimeMillis(),
                    durationSeconds = state.totalElapsedSeconds,
                    routineColor = state.routineColor
                )
            )
            repository.getRoutineById(state.routineId)?.let { routine ->
                repository.updateRoutine(routine.copy(lastPerformedAt = System.currentTimeMillis()))
            }
        }
        _uiState.update { it.copy(phase = TimedPhase.FINISHED, shouldVibrate = true) }
    }

    fun cancelWorkout(onDone: () -> Unit) {
        phaseJob?.cancel()
        globalTimerJob?.cancel()
        context.startService(WorkoutForegroundService.stopIntent(context))
        viewModelScope.launch {
            timerStateStore.clear()
            val sessionId = _uiState.value.sessionId
            if (sessionId != null) {
                repository.getSessionById(sessionId)?.let { repository.deleteSession(it) }
            }
            onDone()
        }
    }

    private suspend fun persistTimerState(state: TimedWorkoutUiState) {
        val sessionId = state.sessionId ?: return
        timerStateStore.save(
            SavedTimerState(
                sessionId = sessionId,
                exerciseIndex = state.currentExerciseIndex,
                phaseOrdinal = state.phase.ordinal,
                phaseRemainingSeconds = state.phaseRemaining
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        phaseJob?.cancel()
        globalTimerJob?.cancel()
        context.startService(WorkoutForegroundService.stopIntent(context))
    }
}
