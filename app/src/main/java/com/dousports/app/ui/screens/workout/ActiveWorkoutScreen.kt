package com.dousports.app.ui.screens.workout

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dousports.app.ui.theme.GreenSuccess
import com.dousports.app.ui.theme.OrangeEnergy
import com.dousports.app.utils.toDurationString

@Composable
fun ActiveWorkoutScreen(
    routineId: Long,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(routineId) {
        if (uiState.isLoading) viewModel.loadRoutine(routineId)
    }

    LaunchedEffect(uiState.prBeatenExerciseName) {
        val name = uiState.prBeatenExerciseName ?: return@LaunchedEffect
        snackbarHostState.showSnackbar("Nouveau record sur $name !")
        viewModel.clearPrBeaten()
    }

    LaunchedEffect(uiState.restTimerFinished) {
        if (uiState.restTimerFinished) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(VibratorManager::class.java))?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }
            vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            viewModel.clearRestTimerFinished()
        }
    }

    BackHandler { showCancelDialog = true }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Annuler la séance ?") },
            text = { Text("Votre progression sera perdue.") },
            confirmButton = {
                TextButton(onClick = { viewModel.cancelWorkout(onCancel) }) {
                    Text("Annuler la séance", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Continuer") }
            }
        )
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Terminer la séance ?") },
            text = { Text("Super séance ! Êtes-vous sûr de vouloir terminer ?") },
            confirmButton = {
                TextButton(onClick = { viewModel.finishWorkout(onFinish) }) {
                    Text("Terminer", color = GreenSuccess)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("Continuer") }
            }
        )
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = OrangeEnergy)
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { scaffoldPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(scaffoldPadding)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showCancelDialog = true }) {
                Icon(Icons.Default.Close, "Annuler")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    uiState.routineName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    uiState.elapsedSeconds.toDurationString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = OrangeEnergy
                )
            }
            Button(
                onClick = { showFinishDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Terminer")
            }
        }

        HorizontalDivider()

        // Rest timer
        val restRemaining = uiState.restTimerRemaining
        if (restRemaining != null) {
            RestTimerBar(
                remaining = restRemaining,
                total = uiState.restTimerTotalSeconds,
                onSkip = viewModel::skipRestTimer
            )
        }

        // Exercise tabs
        if (uiState.exercises.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.exercises) { index, ex ->
                    val isSelected = uiState.currentExerciseIndex == index
                    val isComplete = ex.loggedSets.size >= ex.routineExercise.targetSets
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCurrentExercise(index) },
                        label = {
                            Text(
                                ex.routineExercise.exerciseName.take(20),
                                maxLines = 1
                            )
                        },
                        leadingIcon = if (isComplete) {
                            { Icon(Icons.Default.CheckCircle, null, tint = GreenSuccess, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeEnergy,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Current exercise
        if (uiState.exercises.isNotEmpty() && uiState.currentExerciseIndex < uiState.exercises.size) {
            val currentEx = uiState.exercises[uiState.currentExerciseIndex]
            ExerciseWorkoutPanel(
                exerciseState = currentEx,
                exerciseIndex = uiState.currentExerciseIndex,
                onLogSet = { reps, weight ->
                    viewModel.logSet(uiState.currentExerciseIndex, reps, weight)
                },
                onRemoveLastSet = {
                    viewModel.removeLastSet(uiState.currentExerciseIndex)
                }
            )
        }
    }
    } // end Scaffold
}

@Composable
private fun RestTimerBar(
    remaining: Int,
    total: Int,
    onSkip: () -> Unit
) {
    val progress = if (total > 0) remaining.toFloat() / total.toFloat() else 0f
    val minutes = remaining / 60
    val seconds = remaining % 60

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Repos : %d:%02d".format(minutes, seconds),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OrangeEnergy
                )
                TextButton(onClick = onSkip) {
                    Text("Ignorer", style = MaterialTheme.typography.bodySmall)
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = OrangeEnergy,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun ExerciseWorkoutPanel(
    exerciseState: ExerciseWorkoutState,
    exerciseIndex: Int,
    onLogSet: (Int, Float) -> Unit,
    onRemoveLastSet: () -> Unit
) {
    var repsInput by remember(exerciseIndex) {
        mutableStateOf(exerciseState.routineExercise.targetReps.toString())
    }
    var weightInput by remember(exerciseIndex) {
        mutableStateOf(
            exerciseState.previousSets.firstOrNull()?.weight?.let {
                if (it > 0) it.toInt().toString() else null
            } ?: if (exerciseState.routineExercise.targetWeight > 0)
                exerciseState.routineExercise.targetWeight.toInt().toString()
            else ""
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                exerciseState.routineExercise.exerciseName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${exerciseState.routineExercise.targetSets} séries × ${exerciseState.routineExercise.targetReps} reps",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val pr = exerciseState.personalRecord
            if (pr != null && pr > 0f) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = OrangeEnergy,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Record : %.1f kg".format(pr),
                        style = MaterialTheme.typography.bodySmall,
                        color = OrangeEnergy
                    )
                }
            }
        }

        // Logged sets
        if (exerciseState.loggedSets.isNotEmpty()) {
            item {
                Text(
                    "Séries effectuées",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            itemsIndexed(exerciseState.loggedSets) { index, set ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenSuccess.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(GreenSuccess),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${set.setNumber}", color = Color.White,
                                style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "${set.reps} reps",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        if (set.weight > 0) {
                            Text(
                                "%.1f kg".format(set.weight),
                                style = MaterialTheme.typography.bodyLarge,
                                color = OrangeEnergy,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            item {
                TextButton(
                    onClick = onRemoveLastSet,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Undo, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Annuler la dernière série")
                }
            }
        }

        // Previous performance
        if (exerciseState.previousSets.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Précédente séance",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        val grouped = exerciseState.previousSets
                            .groupBy { it.setNumber }
                            .entries.take(5)
                        grouped.forEach { (setNum, sets) ->
                            val s = sets.first()
                            Text(
                                "Série $setNum: ${s.reps} reps${if (s.weight > 0) " · %.1f kg".format(s.weight) else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Input for next set
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Série ${exerciseState.loggedSets.size + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WorkoutNumberField(
                            label = "Répétitions",
                            value = repsInput,
                            onValueChange = { repsInput = it },
                            modifier = Modifier.weight(1f)
                        )
                        WorkoutNumberField(
                            label = "Poids (kg)",
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val reps = repsInput.toIntOrNull() ?: 0
                            val weight = weightInput.toFloatOrNull() ?: 0f
                            if (reps > 0) {
                                onLogSet(reps, weight)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeEnergy),
                        shape = RoundedCornerShape(10.dp),
                        enabled = repsInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Valider la série", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        textStyle = LocalTextStyle.current.copy(
            textAlign = TextAlign.Center,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OrangeEnergy,
            cursorColor = OrangeEnergy
        )
    )
}
