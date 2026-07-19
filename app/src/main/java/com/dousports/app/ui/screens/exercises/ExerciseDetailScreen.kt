package com.dousports.app.ui.screens.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.dousports.app.utils.gifUrl
import java.io.File
import com.dousports.app.utils.secondaryMusclesList
import com.dousports.app.utils.stepsAsList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseDetailUiState(
    val exercise: ExerciseEntity? = null,
    val isDeleted: Boolean = false
)

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val repository: ExerciseRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            repository.getExerciseByIdFlow(id).collect { ex ->
                _uiState.update { it.copy(exercise = ex) }
            }
        }
    }

    fun deleteExercise() {
        val ex = _uiState.value.exercise ?: return
        viewModelScope.launch {
            repository.deleteExercise(ex)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onBack: () -> Unit,
    onEditExercise: (String) -> Unit = {},
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val exercise = uiState.exercise
    val context = LocalContext.current

    LaunchedEffect(exerciseId) { viewModel.load(exerciseId) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onBack()
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val gifLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(GifDecoder.Factory()) }
            .build()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer l'exercice") },
            text = {
                Text(
                    "Voulez-vous supprimer \"${exercise?.name}\" ? Cette action est irréversible."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteExercise()
                }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    if (exercise?.isCustom == true) {
                        IconButton(onClick = { onEditExercise(exercise.id) }) {
                            Icon(Icons.Default.Edit, "Modifier")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                "Supprimer",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        exercise?.let { ex ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    if (ex.isCustom) {
                        if (ex.gifPath.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(File(ex.gifPath))
                                        .crossfade(true)
                                        .build(),
                                    imageLoader = gifLoader,
                                    contentDescription = ex.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .background(OrangeEnergy.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        modifier = Modifier.size(72.dp),
                                        tint = OrangeEnergy.copy(alpha = 0.4f)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Exercice personnalisé",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OrangeEnergy.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(ex.gifUrl())
                                    .crossfade(true)
                                    .build(),
                                imageLoader = gifLoader,
                                contentDescription = ex.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InfoChip(label = ex.bodyPart, primary = true)
                            if (ex.equipment.isNotBlank()) InfoChip(label = ex.equipment)
                            if (ex.muscleGroup.isNotBlank()) InfoChip(label = ex.muscleGroup)
                            if (ex.isCustom) InfoChip(label = "Perso")
                        }

                        Spacer(Modifier.height(16.dp))

                        val secondary = ex.secondaryMusclesList()
                        if (secondary.isNotEmpty()) {
                            Text(
                                "Muscles secondaires",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                secondary.take(4).forEach { muscle ->
                                    InfoChip(label = muscle)
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        val steps = ex.stepsAsList()
                        if (steps.isNotEmpty()) {
                            Text(
                                "Instructions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }

                val steps = exercise.stepsAsList()
                itemsIndexed(steps) { index, step ->
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(OrangeEnergy),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            step,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = OrangeEnergy)
        }
    }
}

@Composable
private fun InfoChip(label: String, primary: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (primary) OrangeEnergy.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            label.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
            color = if (primary) OrangeEnergy else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
