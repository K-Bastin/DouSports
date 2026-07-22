package com.dousports.app.ui.screens.schedule

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.data.local.entity.RoutineEntity
import com.dousports.app.data.local.entity.WeeklyScheduleEntity
import com.dousports.app.data.repository.WeeklyScheduleRepository
import com.dousports.app.data.repository.WorkoutRepository
import com.dousports.app.ui.theme.OrangeEnergy
import androidx.glance.appwidget.updateAll
import com.dousports.app.widget.TodayRoutineWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private val DAY_NAMES = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")

data class WeeklyScheduleUiState(
    val schedule: Map<Int, WeeklyScheduleEntity> = emptyMap(), // dayOfWeek -> entry
    val routines: List<RoutineEntity> = emptyList(),
    val pickerDay: Int? = null  // day currently being assigned
)

@HiltViewModel
class WeeklyScheduleViewModel @Inject constructor(
    private val scheduleRepository: WeeklyScheduleRepository,
    private val workoutRepository: WorkoutRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(WeeklyScheduleUiState())
    val state: StateFlow<WeeklyScheduleUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                scheduleRepository.getSchedule(),
                workoutRepository.getAllRoutines()
            ) { schedule, routines ->
                Pair(schedule.associateBy { it.dayOfWeek }, routines)
            }.collect { (scheduleMap, routines) ->
                _state.update { it.copy(schedule = scheduleMap, routines = routines) }
            }
        }
    }

    fun openPicker(day: Int) = _state.update { it.copy(pickerDay = day) }
    fun closePicker() = _state.update { it.copy(pickerDay = null) }

    fun assignRoutine(dayOfWeek: Int, routine: RoutineEntity) {
        viewModelScope.launch {
            scheduleRepository.setDay(dayOfWeek, routine.id, routine.name)
            _state.update { it.copy(pickerDay = null) }
            TodayRoutineWidget().updateAll(context)
        }
    }

    fun clearDay(dayOfWeek: Int) {
        viewModelScope.launch {
            scheduleRepository.clearDay(dayOfWeek)
            TodayRoutineWidget().updateAll(context)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleScreen(
    onBack: () -> Unit,
    onStartRoutine: (Long, Boolean) -> Unit = { _, _ -> },
    viewModel: WeeklyScheduleViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Routine picker dialog
    state.pickerDay?.let { day ->
        RoutinePickerDialog(
            dayName = DAY_NAMES[day - 1],
            routines = state.routines,
            onSelect = { routine -> viewModel.assignRoutine(day, routine) },
            onDismiss = viewModel::closePicker
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planning hebdomadaire") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "Planifie ta semaine d'entraînement",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items((1..7).toList(), key = { it }) { day ->
                val entry = state.schedule[day]
                DayCard(
                    dayName = DAY_NAMES[day - 1],
                    entry = entry,
                    onAssign = { viewModel.openPicker(day) },
                    onClear = { viewModel.clearDay(day) },
                    onStart = entry?.let {
                        val isTimed = state.routines.find { r -> r.id == it.routineId }?.isTimed ?: false
                        { onStartRoutine(it.routineId, isTimed) }
                    }
                )
            }
        }
    }
}

@Composable
private fun DayCard(
    dayName: String,
    entry: WeeklyScheduleEntity?,
    onAssign: () -> Unit,
    onClear: () -> Unit,
    onStart: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry != null)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (entry == null)
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day label
            Box(
                modifier = Modifier
                    .width(90.dp)
            ) {
                Text(
                    dayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (entry != null) OrangeEnergy else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Content
            if (entry != null) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.routineName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (onStart != null) {
                    IconButton(
                        onClick = onStart,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Démarrer",
                            tint = OrangeEnergy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Supprimer",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    "Repos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onAssign) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Assigner", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun RoutinePickerDialog(
    dayName: String,
    routines: List<RoutineEntity>,
    onSelect: (RoutineEntity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir une routine — $dayName") },
        text = {
            if (routines.isEmpty()) {
                Text("Aucune routine disponible. Crée d'abord une routine.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(routines, key = { it.id }) { routine ->
                        ListItem(
                            headlineContent = { Text(routine.name) },
                            supportingContent = routine.description
                                .takeIf { it.isNotBlank() }
                                ?.let { { Text(it, maxLines = 1) } },
                            modifier = Modifier
                                .clickable { onSelect(routine) }
                                .fillMaxWidth()
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
