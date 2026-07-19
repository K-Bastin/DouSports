package com.dousports.app.ui.screens.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dousports.app.data.local.entity.ExerciseEntity
import com.dousports.app.data.repository.ExerciseRepository
import com.dousports.app.ui.theme.OrangeEnergy
import com.dousports.app.utils.secondaryMusclesList
import com.dousports.app.utils.stepsAsList
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CreateExerciseUiState(
    val name: String = "",
    val bodyPart: String = "",
    val equipment: String = "",
    val target: String = "",
    val muscleGroup: String = "",
    val secondaryMuscles: String = "",
    val steps: List<String> = listOf(""),
    val existingBodyParts: List<String> = emptyList(),
    val existingEquipment: List<String> = emptyList(),
    val nameError: Boolean = false,
    val bodyPartError: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isEditMode: Boolean = false,
    val editId: String = ""
)

@HiltViewModel
class CreateExerciseViewModel @Inject constructor(
    private val repository: ExerciseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateExerciseUiState())
    val uiState: StateFlow<CreateExerciseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val bodyParts = repository.getAllBodyParts()
            val equipment = repository.getAllEquipment()
            _uiState.update { it.copy(existingBodyParts = bodyParts, existingEquipment = equipment) }
        }
    }

    fun loadForEdit(exerciseId: String) {
        viewModelScope.launch {
            val ex = repository.getExerciseById(exerciseId) ?: return@launch
            _uiState.update {
                it.copy(
                    isEditMode = true,
                    editId = ex.id,
                    name = ex.name,
                    bodyPart = ex.bodyPart,
                    equipment = ex.equipment,
                    target = ex.target,
                    muscleGroup = ex.muscleGroup,
                    secondaryMuscles = ex.secondaryMusclesList().joinToString(", "),
                    steps = ex.stepsAsList().ifEmpty { listOf("") }
                )
            }
        }
    }

    fun updateName(v: String) = _uiState.update { it.copy(name = v, nameError = false) }
    fun updateBodyPart(v: String) = _uiState.update { it.copy(bodyPart = v, bodyPartError = false) }
    fun updateEquipment(v: String) = _uiState.update { it.copy(equipment = v) }
    fun updateTarget(v: String) = _uiState.update { it.copy(target = v) }
    fun updateMuscleGroup(v: String) = _uiState.update { it.copy(muscleGroup = v) }
    fun updateSecondaryMuscles(v: String) = _uiState.update { it.copy(secondaryMuscles = v) }

    fun updateStep(index: Int, value: String) {
        val steps = _uiState.value.steps.toMutableList()
        steps[index] = value
        _uiState.update { it.copy(steps = steps) }
    }

    fun addStep() = _uiState.update { it.copy(steps = it.steps + "") }

    fun removeStep(index: Int) {
        val steps = _uiState.value.steps.toMutableList()
        if (steps.size > 1) steps.removeAt(index)
        _uiState.update { it.copy(steps = steps) }
    }

    fun save() {
        val s = _uiState.value
        val nameError = s.name.isBlank()
        val bodyPartError = s.bodyPart.isBlank()
        if (nameError || bodyPartError) {
            _uiState.update { it.copy(nameError = nameError, bodyPartError = bodyPartError) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val gson = Gson()
            val secondaryList = s.secondaryMuscles
                .split(",").map { it.trim() }.filter { it.isNotBlank() }
            val stepsList = s.steps.filter { it.isNotBlank() }
            val exercise = ExerciseEntity(
                id = if (s.isEditMode) s.editId else "custom-${UUID.randomUUID()}",
                name = s.name.trim(),
                category = s.bodyPart.trim().lowercase(),
                bodyPart = s.bodyPart.trim(),
                equipment = s.equipment.trim().ifBlank { "body weight" },
                target = s.target.trim(),
                muscleGroup = s.muscleGroup.trim(),
                secondaryMuscles = gson.toJson(secondaryList),
                instructionSteps = gson.toJson(stepsList),
                imagePath = "",
                gifPath = "",
                mediaId = "",
                isCustom = true
            )
            if (s.isEditMode) repository.updateExercise(exercise)
            else repository.insertExercise(exercise)
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExerciseScreen(
    exerciseId: String?,
    onBack: () -> Unit,
    viewModel: CreateExerciseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(exerciseId) {
        if (exerciseId != null) viewModel.loadForEdit(exerciseId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isEditMode) "Modifier l'exercice" else "Créer un exercice")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
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
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Informations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("Nom de l'exercice *") },
                    isError = uiState.nameError,
                    supportingText = if (uiState.nameError) {
                        { Text("Le nom est requis") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeEnergy,
                        cursorColor = OrangeEnergy
                    )
                )
            }

            item {
                DropdownTextField(
                    value = uiState.bodyPart,
                    onValueChange = viewModel::updateBodyPart,
                    label = "Partie du corps *",
                    options = uiState.existingBodyParts,
                    isError = uiState.bodyPartError,
                    errorText = "La partie du corps est requise"
                )
            }

            item {
                DropdownTextField(
                    value = uiState.equipment,
                    onValueChange = viewModel::updateEquipment,
                    label = "Équipement",
                    options = uiState.existingEquipment
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.target,
                    onValueChange = viewModel::updateTarget,
                    label = { Text("Muscle cible") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeEnergy,
                        cursorColor = OrangeEnergy
                    )
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.muscleGroup,
                    onValueChange = viewModel::updateMuscleGroup,
                    label = { Text("Groupe musculaire") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeEnergy,
                        cursorColor = OrangeEnergy
                    )
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            item {
                Text(
                    "Muscles secondaires",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.secondaryMuscles,
                    onValueChange = viewModel::updateSecondaryMuscles,
                    label = { Text("Muscles secondaires") },
                    placeholder = { Text("ex: biceps, triceps, deltoïdes") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeEnergy,
                        cursorColor = OrangeEnergy
                    )
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            item {
                Text(
                    "Instructions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.steps.forEachIndexed { index, step ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(OrangeEnergy, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${index + 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedTextField(
                                value = step,
                                onValueChange = { viewModel.updateStep(index, it) },
                                placeholder = { Text("Étape ${index + 1}") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeEnergy,
                                    cursorColor = OrangeEnergy
                                )
                            )
                            if (uiState.steps.size > 1) {
                                IconButton(onClick = { viewModel.removeStep(index) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        "Supprimer",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else {
                                Spacer(Modifier.size(48.dp))
                            }
                        }
                    }

                    TextButton(
                        onClick = viewModel::addStep,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ajouter une étape")
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = viewModel::save,
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeEnergy),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (uiState.isEditMode) "Modifier" else "Créer l'exercice",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: List<String>,
    isError: Boolean = false,
    errorText: String = ""
) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = options.filter { it.contains(value, ignoreCase = true) }

    ExposedDropdownMenuBox(
        expanded = expanded && filtered.isNotEmpty(),
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            isError = isError,
            supportingText = if (isError) {
                { Text(errorText) }
            } else null,
            trailingIcon = {
                if (options.isNotEmpty()) {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded && filtered.isNotEmpty()
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangeEnergy,
                cursorColor = OrangeEnergy
            )
        )
        if (filtered.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filtered.take(5).forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
