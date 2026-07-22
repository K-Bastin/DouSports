package com.dousports.app.ui.screens.workout

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dousports.app.ui.theme.GreenSuccess
import com.dousports.app.ui.theme.OrangeEnergy

@Composable
fun TimedWorkoutScreen(
    routineId: Long,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    viewModel: TimedWorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(routineId) {
        if (uiState.isLoading) viewModel.loadRoutine(routineId)
    }

    LaunchedEffect(uiState.shouldVibrate) {
        if (uiState.shouldVibrate) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(VibratorManager::class.java))?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }
            vibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
            viewModel.clearVibrate()
        }
    }

    LaunchedEffect(uiState.phase) {
        if (uiState.phase == TimedPhase.FINISHED) onFinish()
    }

    BackHandler {
        if (uiState.phase == TimedPhase.FINISHED) {
            onFinish()
        } else {
            viewModel.pause()
            showCancelDialog = true
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = {
                showCancelDialog = false
                if (!uiState.isPaused) viewModel.startOrResume()
            },
            title = { Text("Annuler la séance ?") },
            text = { Text("Votre progression sera perdue.") },
            confirmButton = {
                TextButton(onClick = { viewModel.cancelWorkout(onCancel) }) {
                    Text("Annuler la séance", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    viewModel.startOrResume()
                }) { Text("Continuer") }
            }
        )
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = OrangeEnergy)
        }
        return
    }

    val arcColor by animateColorAsState(
        targetValue = when (uiState.phase) {
            TimedPhase.EXERCISE -> OrangeEnergy
            TimedPhase.REST -> GreenSuccess
            TimedPhase.READY -> MaterialTheme.colorScheme.primary
            TimedPhase.FINISHED -> GreenSuccess
        },
        animationSpec = tween(400),
        label = "arc_color"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    val currentExercise = uiState.exercises.getOrNull(uiState.currentExerciseIndex)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // Header row: routine name + elapsed
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.routineName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = uiState.totalElapsedSeconds.toDurationLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${uiState.currentExerciseIndex + 1} / ${uiState.exercises.size}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(32.dp))

            // Phase label
            val phaseLabel = when (uiState.phase) {
                TimedPhase.READY -> "PRÊT"
                TimedPhase.EXERCISE -> "EXERCICE"
                TimedPhase.REST -> "REPOS"
                TimedPhase.FINISHED -> "TERMINÉ"
            }
            Text(
                text = phaseLabel,
                style = MaterialTheme.typography.labelLarge,
                color = arcColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(24.dp))

            // Arc countdown circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                val sweepFraction = if (uiState.phaseDuration > 0) {
                    uiState.phaseRemaining.toFloat() / uiState.phaseDuration.toFloat()
                } else 1f
                val sweepAngle = sweepFraction * 360f

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 20.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Track
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    // Progress
                    if (sweepAngle > 0f) {
                        drawArc(
                            color = arcColor,
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Time remaining in center
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.phaseRemaining.toTimeString(),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Exercise name — during REST, currentExerciseIndex already points to next exercise
            if (uiState.phase == TimedPhase.REST) {
                Text(
                    text = "Repos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (currentExercise != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Prochain : ${currentExercise.exerciseName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (currentExercise != null) {
                Text(
                    text = currentExercise.exerciseName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }

            Spacer(Modifier.weight(1f))

            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel button
                OutlinedButton(
                    onClick = {
                        viewModel.pause()
                        showCancelDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Annuler", modifier = Modifier.size(20.dp))
                }

                // Play / Pause
                Button(
                    onClick = {
                        if (uiState.isPaused || uiState.phase == TimedPhase.READY) {
                            viewModel.startOrResume()
                        } else {
                            viewModel.pause()
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = arcColor),
                    modifier = Modifier.size(72.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    val icon = if (uiState.isPaused || uiState.phase == TimedPhase.READY) {
                        Icons.Default.PlayArrow
                    } else {
                        Icons.Default.Pause
                    }
                    Icon(icon, contentDescription = "Lecture/Pause", modifier = Modifier.size(36.dp))
                }

                // Skip
                OutlinedButton(
                    onClick = { viewModel.skipPhase() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(52.dp),
                    enabled = uiState.phase != TimedPhase.FINISHED
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Passer", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun Int.toTimeString(): String {
    val m = this / 60
    val s = this % 60
    return if (m > 0) "%d:%02d".format(m, s) else "%d".format(s)
}

private fun Long.toDurationLabel(): String {
    val m = this / 60
    val s = this % 60
    return "%d:%02d".format(m, s)
}
