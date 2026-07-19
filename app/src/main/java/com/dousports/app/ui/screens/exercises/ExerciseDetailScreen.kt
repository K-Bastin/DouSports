package com.dousports.app.ui.screens.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.dousports.app.utils.secondaryMusclesList
import com.dousports.app.utils.stepsAsList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val repository: ExerciseRepository
) : ViewModel() {
    private val _exercise = MutableStateFlow<ExerciseEntity?>(null)
    val exercise = _exercise.asStateFlow()

    fun load(id: String) {
        viewModelScope.launch {
            _exercise.value = repository.getExerciseById(id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onBack: () -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val exercise by viewModel.exercise.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(exerciseId) { viewModel.load(exerciseId) }

    val gifLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(GifDecoder.Factory()) }
            .build()
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
                    // GIF animation
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

                item {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Tags row
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InfoChip(label = ex.bodyPart, primary = true)
                            if (ex.equipment.isNotBlank()) InfoChip(label = ex.equipment)
                            if (ex.muscleGroup.isNotBlank()) InfoChip(label = ex.muscleGroup)
                        }

                        Spacer(Modifier.height(16.dp))

                        // Secondary muscles
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

                        // Instructions
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

                val steps = exercise?.stepsAsList() ?: emptyList()
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
