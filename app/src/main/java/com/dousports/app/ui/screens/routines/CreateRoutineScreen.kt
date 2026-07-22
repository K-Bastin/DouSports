package com.dousports.app.ui.screens.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dousports.app.data.local.entity.ExerciseEntity
import com.dousports.app.data.local.entity.RoutineEntity
import com.dousports.app.data.local.entity.RoutineExerciseEntity
import com.dousports.app.data.repository.ExerciseRepository
import com.dousports.app.data.repository.WorkoutRepository
import com.dousports.app.ui.theme.OrangeEnergy
import com.dousports.app.utils.imageUrl
import com.dousports.app.utils.toFrBodyPart
import com.dousports.app.utils.toFrEquipment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class RoutineExerciseItem(
    val tempId: Long = System.nanoTime(),
    val exerciseId: String,
    val exerciseName: String,
    val sets: Int = 3,
    val reps: Int = 10,
    val weight: Float = 0f,
    val restSeconds: Int = 90,
    val durationSeconds: Int = 45
)

val ROUTINE_COLORS = listOf(
    0xFFFF6200.toInt(),
    0xFF2196F3.toInt(),
    0xFF4CAF50.toInt(),
    0xFF9C27B0.toInt(),
    0xFFF44336.toInt(),
    0xFF009688.toInt(),
    0xFFE91E63.toInt(),
    0xFF3F51B5.toInt(),
    0xFFFF9800.toInt(),
    0xFF00BCD4.toInt()
)

data class CreateRoutineState(
    val name: String = "",
    val description: String = "",
    val color: Int = 0,
    val isTimed: Boolean = false,
    val exercises: List<RoutineExerciseItem> = emptyList(),
    val exerciseSearchQuery: String = "",
    val exerciseSearchResults: List<ExerciseEntity> = emptyList(),
    val pickerBodyParts: List<String> = emptyList(),
    val pickerEquipmentList: List<String> = emptyList(),
    val pickerSelectedBodyPart: String? = null,
    val pickerSelectedEquipment: String? = null,
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
    private val pickerBodyPart = MutableStateFlow<String?>(null)
    private val pickerEquipment = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            val bodyParts = exerciseRepository.getAllBodyParts()
            val equipment = exerciseRepository.getAllEquipment()
            _state.update { it.copy(pickerBodyParts = bodyParts, pickerEquipmentList = equipment) }
        }

        viewModelScope.launch {
            combine(
                searchQuery.debounce(300),
                pickerBodyPart,
                pickerEquipment
            ) { q, bp, eq -> Triple(q, bp, eq) }
                .flatMapLatest { (q, bp, eq) -> exerciseRepository.filterExercises(q, bp, eq) }
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
                    restSeconds = re.restSeconds,
                    durationSeconds = re.durationSeconds
                )
            }
            _state.update {
                it.copy(
                    name = routine.name,
                    description = routine.description,
                    color = routine.color,
                    isTimed = routine.isTimed,
                    exercises = items
                )
            }
        }
    }

    fun onNameChange(name: String) = _state.update { it.copy(name = name) }
    fun onDescriptionChange(desc: String) = _state.update { it.copy(description = desc) }
    fun onColorChange(color: Int) = _state.update { it.copy(color = color) }
    fun onIsTimedChange(isTimed: Boolean) = _state.update { it.copy(isTimed = isTimed) }

    fun showPicker() = _state.update { it.copy(isPickerVisible = true) }
    fun hidePicker() = _state.update { it.copy(isPickerVisible = false) }

    fun onSearchChange(q: String) {
        searchQuery.value = q
        _state.update { it.copy(exerciseSearchQuery = q) }
    }

    fun onPickerBodyPartSelected(bodyPart: String?) {
        pickerBodyPart.value = bodyPart
        _state.update { it.copy(pickerSelectedBodyPart = bodyPart) }
    }

    fun onPickerEquipmentSelected(equipment: String?) {
        pickerEquipment.value = equipment
        _state.update { it.copy(pickerSelectedEquipment = equipment) }
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
                exerciseSearchQuery = "",
                pickerSelectedBodyPart = null,
                pickerSelectedEquipment = null
            )
        }
        searchQuery.value = ""
        pickerBodyPart.value = null
        pickerEquipment.value = null
    }

    fun removeExercise(tempId: Long) =
        _state.update { it.copy(exercises = it.exercises.filter { e -> e.tempId != tempId }) }

    fun moveExercise(from: Int, to: Int) {
        if (from == to) return
        _state.update {
            val list = it.exercises.toMutableList()
            val item = list.removeAt(from)
            list.add(to, item)
            it.copy(exercises = list)
        }
    }

    fun updateExercise(tempId: Long, sets: Int? = null, reps: Int? = null, weight: Float? = null, durationSeconds: Int? = null) {
        _state.update {
            it.copy(exercises = it.exercises.map { e ->
                if (e.tempId == tempId) e.copy(
                    sets = sets ?: e.sets,
                    reps = reps ?: e.reps,
                    weight = weight ?: e.weight,
                    durationSeconds = durationSeconds ?: e.durationSeconds
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
                    RoutineEntity(id = routineId, name = s.name, description = s.description, color = s.color, isTimed = s.isTimed)
                )
                routineId
            } else {
                workoutRepository.insertRoutine(
                    RoutineEntity(name = s.name, description = s.description, color = s.color, isTimed = s.isTimed)
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
                    restSeconds = e.restSeconds,
                    durationSeconds = e.durationSeconds
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
            bodyParts = state.pickerBodyParts,
            equipmentList = state.pickerEquipmentList,
            selectedBodyPart = state.pickerSelectedBodyPart,
            selectedEquipment = state.pickerSelectedEquipment,
            onQueryChange = viewModel::onSearchChange,
            onBodyPartSelected = viewModel::onPickerBodyPartSelected,
            onEquipmentSelected = viewModel::onPickerEquipmentSelected,
            onSelect = viewModel::addExercise,
            onDismiss = viewModel::hidePicker
        )
        return
    }

    // Drag & drop state
    val lazyListState = rememberLazyListState()
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    // Tracks current drag position without stale-closure risk
    val currentDragIndex = remember { androidx.compose.runtime.mutableIntStateOf(-1) }

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
            state = lazyListState,
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
                RoutineColorPicker(
                    selectedColor = state.color,
                    onColorSelected = viewModel::onColorChange
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Routine chronométrée",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Chaque exercice dure un temps fixé",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.isTimed,
                            onCheckedChange = viewModel::onIsTimedChange,
                            colors = SwitchDefaults.colors(checkedThumbColor = OrangeEnergy, checkedTrackColor = OrangeEnergy.copy(alpha = 0.4f))
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Exercices (${state.exercises.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (state.isTimed && state.exercises.isNotEmpty()) {
                            val totalSec = state.exercises.sumOf { it.durationSeconds } +
                                state.exercises.dropLast(1).sumOf { it.restSeconds }
                            val m = totalSec / 60
                            val s = totalSec % 60
                            val label = if (m > 0) "~${m}min ${s}s" else "~${s}s"
                            Text(
                                "Durée estimée : $label",
                                style = MaterialTheme.typography.bodySmall,
                                color = OrangeEnergy
                            )
                        }
                    }
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
                val isDragging = draggedIndex == index
                RoutineExerciseCard(
                    index = index + 1,
                    item = exercise,
                    isTimed = state.isTimed,
                    isDragging = isDragging,
                    dragOffset = if (isDragging) dragOffset else 0f,
                    onRemove = { viewModel.removeExercise(exercise.tempId) },
                    onSetsChange = { viewModel.updateExercise(exercise.tempId, sets = it) },
                    onRepsChange = { viewModel.updateExercise(exercise.tempId, reps = it) },
                    onWeightChange = { viewModel.updateExercise(exercise.tempId, weight = it) },
                    onDurationChange = { viewModel.updateExercise(exercise.tempId, durationSeconds = it) },
                    onDragStart = {
                        draggedIndex = index
                        currentDragIndex.intValue = index
                        dragOffset = 0f
                    },
                    onDrag = { delta ->
                        dragOffset += delta
                        val fromIdx = currentDragIndex.intValue
                        val draggingTempId = if (fromIdx >= 0) state.exercises.getOrNull(fromIdx)?.tempId else null
                        if (fromIdx >= 0 && draggingTempId != null) {
                            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                            val draggingItem = visibleItems.firstOrNull { it.key == draggingTempId }
                            if (draggingItem != null) {
                                val centerY = draggingItem.offset + draggingItem.size / 2 + dragOffset
                                val targetItem = visibleItems.firstOrNull { item ->
                                    item.key != draggingTempId &&
                                    centerY >= item.offset && centerY <= item.offset + item.size &&
                                    state.exercises.any { it.tempId == item.key }
                                }
                                if (targetItem != null) {
                                    val targetIdx = state.exercises.indexOfFirst { it.tempId == targetItem.key }
                                    if (targetIdx >= 0 && targetIdx != fromIdx) {
                                        viewModel.moveExercise(fromIdx, targetIdx)
                                        draggedIndex = targetIdx
                                        currentDragIndex.intValue = targetIdx
                                        dragOffset = 0f
                                    }
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        draggedIndex = null
                        currentDragIndex.intValue = -1
                        dragOffset = 0f
                    }
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
private fun RoutineColorPicker(selectedColor: Int, onColorSelected: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Couleur de la routine",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(ROUTINE_COLORS) { colorInt ->
                val isSelected = selectedColor == colorInt
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(colorInt))
                        .then(
                            if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                            else Modifier
                        )
                        .clickable { onColorSelected(colorInt) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        if (selectedColor != 0) {
            TextButton(
                onClick = { onColorSelected(0) },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Réinitialiser", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RoutineExerciseCard(
    index: Int,
    item: RoutineExerciseItem,
    isTimed: Boolean = false,
    onRemove: () -> Unit,
    onSetsChange: (Int) -> Unit,
    onRepsChange: (Int) -> Unit,
    onWeightChange: (Float) -> Unit,
    onDurationChange: (Int) -> Unit = {},
    isDragging: Boolean = false,
    dragOffset: Float = 0f,
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDrag by rememberUpdatedState(onDrag)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = dragOffset
                shadowElevation = if (isDragging) 12.dp.toPx() else 0f
                alpha = if (isDragging) 0.95f else 1f
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Drag handle
                Icon(
                    Icons.Default.DragHandle,
                    contentDescription = "Réorganiser",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(24.dp)
                        .pointerInput(item.tempId) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { latestOnDragStart() },
                                onDragEnd = { latestOnDragEnd() },
                                onDragCancel = { latestOnDragEnd() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    latestOnDrag(dragAmount.y)
                                }
                            )
                        }
                )
                Spacer(Modifier.width(8.dp))
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
            if (isTimed) {
                SmallNumberField(
                    label = "Durée (s)",
                    value = item.durationSeconds,
                    onValueChange = onDurationChange,
                    modifier = Modifier.fillMaxWidth(0.45f)
                )
            } else {
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
    bodyParts: List<String>,
    equipmentList: List<String>,
    selectedBodyPart: String?,
    selectedEquipment: String?,
    onQueryChange: (String) -> Unit,
    onBodyPartSelected: (String?) -> Unit,
    onEquipmentSelected: (String?) -> Unit,
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
                placeholder = { Text("Rechercher un exercice...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
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

            if (bodyParts.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedBodyPart == null,
                            onClick = { onBodyPartSelected(null) },
                            label = { Text("Tous") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangeEnergy,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(bodyParts) { part ->
                        FilterChip(
                            selected = selectedBodyPart == part,
                            onClick = { onBodyPartSelected(if (selectedBodyPart == part) null else part) },
                            label = { Text(part.toFrBodyPart()) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangeEnergy,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (equipmentList.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedEquipment == null,
                            onClick = { onEquipmentSelected(null) },
                            label = { Text("Tout équip.") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }
                    items(equipmentList) { equip ->
                        FilterChip(
                            selected = selectedEquipment == equip,
                            onClick = { onEquipmentSelected(if (selectedEquipment == equip) null else equip) },
                            label = { Text(equip.toFrEquipment()) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }
                }
            }

            Text(
                "${results.size} exercices",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results, key = { it.id }) { exercise ->
                    PickerExerciseCard(exercise = exercise, onClick = { onSelect(exercise) })
                }
            }
        }
    }
}

@Composable
private fun PickerExerciseCard(exercise: ExerciseEntity, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            if (exercise.isCustom) {
                if (exercise.gifPath.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(exercise.gifPath))
                            .crossfade(true)
                            .build(),
                        contentDescription = exercise.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(OrangeEnergy.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter, null,
                            modifier = Modifier.size(40.dp),
                            tint = OrangeEnergy.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(exercise.imageUrl())
                        .crossfade(true)
                        .build(),
                    contentDescription = exercise.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                )
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    exercise.bodyPart.toFrBodyPart(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
