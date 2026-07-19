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

private data class YM(val year: Int, val month: Int)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _ym = MutableStateFlow(
        Calendar.getInstance().let { cal ->
            YM(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }
    )

    private val _uiState = MutableStateFlow(
        Calendar.getInstance().let { cal ->
            CalendarUiState(year = cal.get(Calendar.YEAR), month = cal.get(Calendar.MONTH) + 1)
        }
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        // Reactively rebuilds sessionsByDay whenever DB changes OR displayed month changes
        viewModelScope.launch {
            combine(_ym, repository.getAllSessions()) { ym, allSessions ->
                allSessions
                    .filter { s ->
                        val cal = Calendar.getInstance().also { c -> c.timeInMillis = s.startedAt }
                        cal.get(Calendar.YEAR) == ym.year && cal.get(Calendar.MONTH) + 1 == ym.month
                    }
                    .groupBy { s ->
                        Calendar.getInstance().also { c -> c.timeInMillis = s.startedAt }
                            .get(Calendar.DAY_OF_MONTH)
                    }
            }.collect { grouped ->
                _uiState.update { it.copy(sessionsByDay = grouped) }
            }
        }
    }

    fun loadMonth(year: Int, month: Int) {
        _ym.value = YM(year, month)
        _uiState.update {
            it.copy(
                year = year,
                month = month,
                selectedDay = null,
                selectedSession = null,
                setsForSession = emptyList()
            )
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
