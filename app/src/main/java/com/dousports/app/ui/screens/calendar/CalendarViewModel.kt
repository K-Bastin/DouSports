package com.dousports.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.data.local.entity.WorkoutSessionEntity
import com.dousports.app.data.local.entity.WorkoutSetEntity
import com.dousports.app.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class CalendarUiState(
    val year: Int = 0,
    val month: Int = 0,
    val sessionsByDay: Map<Int, List<WorkoutSessionEntity>> = emptyMap(),
    val selectedDay: Int? = null,
    val selectedSession: WorkoutSessionEntity? = null,
    val setsForSession: List<WorkoutSetEntity> = emptyList(),
    val isLoadingSets: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        val now = Calendar.getInstance()
        loadMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
    }

    fun loadMonth(year: Int, month: Int) {
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val endMs = cal.timeInMillis

        viewModelScope.launch {
            val sessions = repository.getSessionsInRange(startMs, endMs)
            val grouped = sessions.groupBy { s ->
                Calendar.getInstance().also { c -> c.timeInMillis = s.startedAt }
                    .get(Calendar.DAY_OF_MONTH)
            }
            _uiState.update {
                it.copy(
                    year = year,
                    month = month,
                    sessionsByDay = grouped,
                    selectedDay = null,
                    selectedSession = null,
                    setsForSession = emptyList()
                )
            }
        }
    }

    fun previousMonth() {
        val s = _uiState.value
        val cal = Calendar.getInstance().apply { set(s.year, s.month - 1, 1) }
        cal.add(Calendar.MONTH, -1)
        loadMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun nextMonth() {
        val s = _uiState.value
        val cal = Calendar.getInstance().apply { set(s.year, s.month - 1, 1) }
        cal.add(Calendar.MONTH, 1)
        loadMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun selectDay(day: Int) {
        val sessions = _uiState.value.sessionsByDay[day] ?: emptyList()
        _uiState.update {
            it.copy(
                selectedDay = if (it.selectedDay == day) null else day,
                selectedSession = null,
                setsForSession = emptyList()
            )
        }
        if (sessions.size == 1) selectSession(sessions.first())
    }

    fun selectSession(session: WorkoutSessionEntity) {
        if (_uiState.value.selectedSession?.id == session.id) {
            _uiState.update { it.copy(selectedSession = null, setsForSession = emptyList()) }
            return
        }
        _uiState.update { it.copy(selectedSession = session, isLoadingSets = true) }
        viewModelScope.launch {
            val sets = repository.getSetsForSessionSync(session.id)
            _uiState.update { it.copy(setsForSession = sets, isLoadingSets = false) }
        }
    }
}
