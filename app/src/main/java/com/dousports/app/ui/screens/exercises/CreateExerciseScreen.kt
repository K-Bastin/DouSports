package com.dousports.app.ui.screens.exercises

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.dousports.app.data.local.entity.ExerciseEntity
import com.dousports.app.data.repository.ExerciseRepository
import com.dousports.app.ui.theme.OrangeEnergy
import com.dousports.app.utils.secondaryMusclesList
import com.dousports.app.utils.stepsAsList
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class CreateExerciseUiState(
    val name: String = "",
    val bodyPart: String = "",
    val equipment: String = "",
    val selectedTarget: String = "",
    val selectedMuscleGroup: String = "",
    val selectedSecondaryMuscles: List<String> = emptyList(),
    val gifPath: String = "",
    val steps: List<String> = listOf(""),
    val existingBodyParts: List<String> = emptyList(),
    val existingEquipment: List<String> = emptyList(),
    val allMuscleOptions: List<String> = emptyList(),
    val nameError: Boolean = false,
    val bodyPartError: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isEditMode: Boolean = false,
    val editId: String = ""
)

@HiltViewModel
class CreateExerciseViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateExerciseUiState())
    val uiState: StateFlow<CreateExerciseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val bodyParts = repository.getAllBodyParts()
            val equipment = repository.getAllEquipment()
            val targets = repository.getAllTargets()
            val muscleGroups = repository.getAllMuscleGroups()
            val allMuscles = (targets + muscleGroups).distinct().sorted()
            _uiState.update {
                it.copy(
                    existingBodyParts = bodyParts,
                    existingEquipment = equipment,
                    allMuscleOptions = allMuscles
                )
            }
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
                    selectedTarget = ex.target,
                    selectedMuscleGroup = ex.muscleGroup,
                    selectedSecondaryMuscles = ex.secondaryMusclesList(),
                    gifPath = ex.gifPath,
                    steps = ex.stepsAsList().ifEmpty { listOf("") }
                )
            }
        }
    }

    fun updateName(v: String) = _uiState.update { it.copy(name = v, nameError = false) }
    fun updateBodyPart(v: String) = _uiState.update { it.copy(bodyPart = v, bodyPartError = false) }
    fun updateEquipment(v: String) = _uiState.update { it.copy(equipment = v) }

    fun selectTarget(value: String) = _uiState.update {
        it.copy(selectedTarget = if (it.selectedTarget == value) "" else value)
    }

    fun selectMuscleGroup(value: String) = _uiState.update {
        it.copy(selectedMuscleGroup = if (it.selectedMuscleGroup == value) "" else value)
    }

    fun toggleSecondaryMuscle(muscle: String) = _uiState.update {
        val updated = if (muscle in it.selectedSecondaryMuscles)
            it.selectedSecondaryMuscles - muscle
        else
            it.selectedSecondaryMuscles + muscle
        it.copy(selectedSecondaryMuscles = updated)
    }

    fun onGifSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldPath = _uiState.value.gifPath
            if (oldPath.isNotBlank()) File(oldPath).delete()
            val destDir = File(context.filesDir, "custom_exercises").also { it.mkdirs() }
            val destFile = File(destDir, "${UUID.randomUUID()}.gif")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                _uiState.update { it.copy(gifPath = destFile.absolutePath) }
            } catch (_: Exception) { }
        }
    }

    fun clearGif() {
        val oldPath = _uiState.value.gifPath
        if (oldPath.isNotBlank()) viewModelScope.launch(Dispatchers.IO) { File(oldPath).delete() }
        _uiState.update { it.copy(gifPath = "") }
    }

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
            val stepsList = s.steps.filter { it.isNotBlank() }
            val exercise = ExerciseEntity(
                id = if (s.isEditMode) s.editId else "custom-${UUID.randomUUID()}",
                name = s.name.trim(),
                category = s.bodyPart.trim().lowercase(),
                bodyPart = s.bodyPart.trim(),
                equipment = s.equipment.trim().ifBlank { "body weight" },
                target = s.selectedTarget,
                muscleGroup = s.selectedMuscleGroup,
                secondaryMuscles = gson.toJson(s.selectedSecondaryMuscles),
                instructionSteps = gson.toJson(stepsList),
                imagePath = s.gifPath,
                gifPath = s.gifPath,
                mediaId = "",
                isCustom = true
            )
            if (s.isEditMode) repository.updateExercise(exercise)
            else repository.insertExercise(exercise)
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateExerciseScreen(
    exerciseId: String?,
    onBack: () -> Unit,
    viewModel: CreateExerciseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(exerciseId) {
        if (exerciseId != null) viewModel.loadForEdit(exerciseId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    val gifLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(GifDecoder.Factory()) }
            .build()
    }

    val gifPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) viewModel.onGifSelected(uri) }

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

            // --- Animation ---
            item {
                Text(
                    "Animation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { gifPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.gifPath.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(uiState.gifPath))
                                    .crossfade(true)
                                    .build(),
                                imageLoader = gifLoader,
                                contentDescription = "Animation",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Modifier",
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .padding(6.dp),
                                tint = Color.White
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Ajouter une animation (optionnel)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (uiState.gifPath.isNotBlank()) {
                        TextButton(onClick = viewModel::clearGif) {
                            Text("Retirer l'animation", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            // --- Informations ---
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

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            // --- Muscles ---
            item {
                Text(
                    "Muscles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                ChipSelectSection(
                    title = "Muscle cible",
                    options = uiState.allMuscleOptions,
                    selected = if (uiState.selectedTarget.isNotBlank())
                        listOf(uiState.selectedTarget) else emptyList(),
                    onToggle = viewModel::selectTarget
                )
            }

            item {
                ChipSelectSection(
                    title = "Groupe musculaire",
                    options = uiState.allMuscleOptions,
                    selected = if (uiState.selectedMuscleGroup.isNotBlank())
                        listOf(uiState.selectedMuscleGroup) else emptyList(),
                    onToggle = viewModel::selectMuscleGroup
                )
            }

            item {
                ChipSelectSection(
                    title = "Muscles secondaires (multi-sélection)",
                    options = uiState.allMuscleOptions,
                    selected = uiState.selectedSecondaryMuscles,
                    onToggle = viewModel::toggleSecondaryMuscle
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            // --- Instructions ---
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

            // --- Bouton enregistrer ---
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipSelectSection(
    title: String,
    options: List<String>,
    selected: List<String>,
    onToggle: (String) -> Unit
) {
    if (options.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                val isSelected = option in selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggle(option) },
                    label = {
                        Text(
                            option.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrangeEnergy,
                        selectedLabelColor = Color.White
                    )
                )
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
