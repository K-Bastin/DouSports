package com.dousports.app.ui.screens.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.data.local.entity.ExerciseEntity
import com.dousports.app.data.local.entity.RoutineEntity
import com.dousports.app.data.local.entity.RoutineExerciseEntity
import com.dousports.app.data.repository.ExerciseRepository
import com.dousports.app.data.repository.WorkoutRepository
import com.dousports.app.ui.theme.OrangeEnergy
import com.dousports.app.utils.toFrBodyPart
import com.dousports.app.utils.toFrEquipment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutineExerciseItem(
    val tempId: Long = System.nanoTime(),
    val exerciseId: String,
    val exerciseName: String,
    val sets: Int = 3,
    val reps: Int = 10,
    val weight: Float = 0f,
    val restSeconds: Int = 90
)

data class CreateRoutineState(
    val name: String = "",
    val description: String = "",
    val exercises: List<RoutineExerciseItem> = emptyList(),
    val exerciseSearchQuery: String = "",
    val exerciseSearchResults: List<ExerciseEntity> = emptyList(),
    val isPickerVisible: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class CreateRoutineViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreateRoutineState())
    val state: StateFlow<CreateRoutineState> = _state.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .flatMapLatest { q -> exerciseRepository.searchExercises(q) }
                .collect { results ->
                    _state.update { it.copy(exerciseSearchResults = results) }
                }
        }
    }

    fun loadRoutine(routineId: Long) {
        viewModelScope.launch {
            val routine = workoutRepository.getRoutineById(routineId) ?: return@launch
            val items = workoutRepository.getExercisesForRoutineSync(routineId).map { re ->
                RoutineExerciseItem(
                    tempId = re.id,
                    exerciseId = re.exerciseId,
                    exerciseName = re.exerciseName,
                    sets = re.targetSets,
                    reps = re.targetReps,
                    weight = re.targetWeight,
                    restSeconds = re.restSeconds
                )
            }
            _state.update {
                it.copy(
                    name = routine.name,
                    description = routine.description,
                    exercises = items
                )
            }
        }
    }

    fun onNameChange(name: String) = _state.update { it.copy(name = name) }
    fun onDescriptionChange(desc: String) = _state.update { it.copy(description = desc) }

    fun showPicker() = _state.update { it.copy(isPickerVisible = true) }
    fun hidePicker() = _state.update { it.copy(isPickerVisible = false) }

    fun onSearchChange(q: String) {
        searchQuery.value = q
        _state.update { it.copy(exerciseSearchQuery = q) }
    }

    fun addExercise(exercise: ExerciseEntity) {
        val item = RoutineExerciseItem(
            exerciseId = exercise.id,
            exerciseName = exercise.name
        )
        _state.update {
            it.copy(
                exercises = it.exercises + item,
                isPickerVisible = false,
                exerciseSearchQuery = ""
            )
        }
        searchQuery.value = ""
    }

    fun removeExercise(tempId: Long) =
        _state.update { it.copy(exercises = it.exercises.filter { e -> e.tempId != tempId }) }

    fun updateExercise(tempId: Long, sets: Int? = null, reps: Int? = null, weight: Float? = null) {
        _state.update {
            it.copy(exercises = it.exercises.map { e ->
                if (e.tempId == tempId) e.copy(
                    sets = sets ?: e.sets,
                    reps = reps ?: e.reps,
                    weight = weight ?: e.weight
                ) else e
            })
        }
    }

    fun save(routineId: Long?, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val s = _state.value
            val id = if (routineId != null) {
                workoutRepository.updateRoutine(
                    RoutineEntity(id = routineId, name = s.name, description = s.description)
                )
                routineId
            } else {
                workoutRepository.insertRoutine(
                    RoutineEntity(name = s.name, description = s.description)
                )
            }
            val items = s.exercises.mapIndexed { index, e ->
                RoutineExerciseEntity(
                    routineId = id,
                    exerciseId = e.exerciseId,
                    exerciseName = e.exerciseName,
                    orderIndex = index,
                    targetSets = e.sets,
                    targetReps = e.reps,
                    targetWeight = e.weight,
                    restSeconds = e.restSeconds
                )
            }
            workoutRepository.saveRoutineExercises(id, items)
            _state.update { it.copy(isSaving = false, isSaved = true) }
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoutineScreen(
    routineId: Long?,
    onBack: () -> Unit,
    onExercisePicker: (String) -> Unit,
    viewModel: CreateRoutineViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(routineId) {
        if (routineId != null) viewModel.loadRoutine(routineId)
    }

    if (state.isPickerVisible) {
        ExercisePickerSheet(
            query = state.exerciseSearchQuery,
            results = state.exerciseSearchResults,
            onQueryChange = viewModel::onSearchChange,
            onSelect = viewModel::addExercise,
            onDismiss = viewModel::hidePicker
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (routineId != null) "Modifier la routine" else "Nouvelle routine")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Retour") }
                },
                actions = {
                    Button(
                        onClick = { viewModel.save(routineId, onBack) },
                        enabled = state.name.isNotBlank() && !state.isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeEnergy),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = androidx.compose.ui.graphics.Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Enregistrer")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Nom de la routine *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeEnergy,
                        cursorColor = OrangeEnergy
                    )
                )
            }

            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("Description (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeEnergy,
                        cursorColor = OrangeEnergy
                    )
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Exercices (${state.exercises.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = viewModel::showPicker,
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ajouter")
                    }
                }
            }

            itemsIndexed(state.exercises, key = { _, e -> e.tempId }) { index, exercise ->
                RoutineExerciseCard(
                    index = index + 1,
                    item = exercise,
                    onRemove = { viewModel.removeExercise(exercise.tempId) },
                    onSetsChange = { viewModel.updateExercise(exercise.tempId, sets = it) },
                    onRepsChange = { viewModel.updateExercise(exercise.tempId, reps = it) },
                    onWeightChange = { viewModel.updateExercise(exercise.tempId, weight = it) }
                )
            }

            if (state.exercises.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.showPicker() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AddCircleOutline, null,
                                tint = OrangeEnergy, modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Ajouter des exercices",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutineExerciseCard(
    index: Int,
    item: RoutineExerciseItem,
    onRemove: () -> Unit,
    onSetsChange: (Int) -> Unit,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(OrangeEnergy),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$index", style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    item.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, "Supprimer", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallNumberField(
                    label = "Séries",
                    value = item.sets,
                    onValueChange = onSetsChange,
                    modifier = Modifier.weight(1f)
                )
                SmallNumberField(
                    label = "Reps",
                    value = item.reps,
                    onValueChange = onRepsChange,
                    modifier = Modifier.weight(1f)
                )
                SmallNumberField(
                    label = "Poids (kg)",
                    value = item.weight.toInt(),
                    onValueChange = { onWeightChange(it.toFloat()) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SmallNumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = if (value == 0) "" else value.toString(),
        onValueChange = { onValueChange(it.toIntOrNull() ?: 0) },
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OrangeEnergy,
            cursorColor = OrangeEnergy
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    query: String,
    results: List<ExerciseEntity>,
    onQueryChange: (String) -> Unit,
    onSelect: (ExerciseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choisir un exercice") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, "Retour") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Rechercher...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OrangeEnergy,
                    cursorColor = OrangeEnergy
                )
            )
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                items(results, key = { it.id }) { exercise ->
                    ListItem(
                        headlineContent = { Text(exercise.name) },
                        supportingContent = {
                            Text(
                                "${exercise.bodyPart.toFrBodyPart()} · ${exercise.equipment.toFrEquipment()}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        modifier = Modifier
                            .clickable { onSelect(exercise) }
                            .clip(RoundedCornerShape(8.dp))
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}
