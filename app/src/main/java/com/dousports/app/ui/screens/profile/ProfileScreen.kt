package com.dousports.app.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dousports.app.data.local.entity.BodyMeasurementEntity
import com.dousports.app.ui.screens.calendar.CalendarCard
import com.dousports.app.ui.screens.calendar.CalendarViewModel
import com.dousports.app.ui.screens.calendar.SessionCard
import com.dousports.app.ui.screens.calendar.monthName
import com.dousports.app.ui.theme.*
import com.dousports.app.ui.viewmodel.ThemeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    onNavigateToStats: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    calendarViewModel: CalendarViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val calState by calendarViewModel.uiState.collectAsState()
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()

    if (state.showAddDialog) {
        AddMeasurementDialog(
            heightInput = state.heightInput,
            weightInput = state.weightInput,
            onHeightChange = viewModel::onHeightChange,
            onWeightChange = viewModel::onWeightChange,
            onConfirm = viewModel::saveMeasurement,
            onDismiss = viewModel::dismissDialog
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openAddDialog,
                containerColor = OrangeEnergy
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter une mesure", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Mon Profil",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(onClick = { themeViewModel.toggleTheme() }) {
                            Icon(
                                if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkTheme) "Mode clair" else "Mode sombre",
                                tint = OrangeEnergy
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onNavigateToHistory,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeEnergy),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OrangeEnergy)
                        ) {
                            Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Historique")
                        }
                        OutlinedButton(
                            onClick = onNavigateToStats,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeEnergy),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OrangeEnergy)
                        ) {
                            Icon(Icons.Default.BarChart, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Stats")
                        }
                    }
                }
            }

            item { CurrentStatsCard(state) }

            if (state.weightHistory.size >= 2) {
                item { WeightChartCard(state.weightHistory) }
            }

            item {
                Text(
                    "Calendrier d'entraînement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                CalendarCard(
                    year = calState.year,
                    month = calState.month,
                    sessionsByDay = calState.sessionsByDay,
                    selectedDay = calState.selectedDay,
                    onPreviousMonth = calendarViewModel::previousMonth,
                    onNextMonth = calendarViewModel::nextMonth,
                    onDayClick = calendarViewModel::selectDay
                )
            }

            if (calState.selectedDay != null) {
                val sessions = calState.sessionsByDay[calState.selectedDay] ?: emptyList()
                if (sessions.isNotEmpty()) {
                    item {
                        Text(
                            "Séances du ${calState.selectedDay} ${monthName(calState.month)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    items(sessions) { session ->
                        SessionCard(
                            session = session,
                            isExpanded = calState.selectedSession?.id == session.id,
                            sets = if (calState.selectedSession?.id == session.id) calState.setsForSession else emptyList(),
                            isLoadingSets = calState.isLoadingSets,
                            onClick = { calendarViewModel.selectSession(session) }
                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Aucune séance ce jour", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (state.measurements.isNotEmpty()) {
                item {
                    Text(
                        "Historique",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                items(state.measurements) { m ->
                    MeasurementRow(m, onDelete = { viewModel.deleteMeasurement(m) })
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Aucune mesure enregistrée", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("Appuie sur + pour commencer", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentStatsCard(state: ProfileUiState) {
    val m = state.latest
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        if (m == null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucune donnée", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Card
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn("Taille", "%.0f cm".format(m.heightCm), OrangeEnergy)
            VerticalDivider()
            StatColumn("Poids", "%.1f kg".format(m.weightKg), GreenSuccess)
            state.bmi?.let {
                VerticalDivider()
                StatColumn("IMC", "%.1f".format(it), bmiColor(it))
            }
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun StatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun bmiColor(bmi: Float) = when {
    bmi < 18.5f -> Color(0xFF64B5F6)
    bmi < 25f   -> GreenSuccess
    bmi < 30f   -> YellowWarning
    else        -> RedError
}

@Composable
private fun WeightChartCard(history: List<Pair<Long, Float>>) {
    val dateFmt = remember { SimpleDateFormat("d MMM", Locale.FRENCH) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Évolution du poids",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "${history.size} mesures",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))

            val weights = history.map { it.second }
            val timestamps = history.map { it.first }
            val minW = weights.min()
            val maxW = weights.max()
            val range = (maxW - minW).coerceAtLeast(1f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val w = size.width
                val h = size.height
                val pts = weights.mapIndexed { i, wt ->
                    val x = if (weights.size > 1) i * w / (weights.size - 1) else w / 2f
                    val y = h - ((wt - minW) / range) * h * 0.85f - h * 0.05f
                    Offset(x, y)
                }

                // Horizontal grid lines
                val gridCount = 3
                for (i in 0..gridCount) {
                    val y = h * i / gridCount
                    drawLine(
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Fill under curve
                if (pts.size >= 2) {
                    val fillPath = Path().apply {
                        moveTo(pts.first().x, h)
                        pts.forEach { lineTo(it.x, it.y) }
                        lineTo(pts.last().x, h)
                        close()
                    }
                    drawPath(fillPath, color = OrangeEnergy.copy(alpha = 0.12f))

                    val linePath = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        pts.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        linePath,
                        color = OrangeEnergy,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                pts.forEach { pt ->
                    drawCircle(color = OrangeEnergy, radius = 5.dp.toPx(), center = pt)
                    drawCircle(color = NavyDark, radius = 3.dp.toPx(), center = pt)
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    dateFmt.format(Date(timestamps.first())),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "min %.1f  max %.1f kg".format(minW, maxW),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    dateFmt.format(Date(timestamps.last())),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MeasurementRow(m: BodyMeasurementEntity, onDelete: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.FRENCH) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    fmt.format(Date(m.recordedAt)),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "%.1f kg  •  %.0f cm".format(m.weightKg, m.heightCm),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = RedError)
            }
        }
    }
}

@Composable
private fun AddMeasurementDialog(
    heightInput: String,
    weightInput: String,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle mesure") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = heightInput,
                    onValueChange = onHeightChange,
                    label = { Text("Taille (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = onWeightChange,
                    label = { Text("Poids (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = heightInput.toFloatOrNull() != null && weightInput.toFloatOrNull() != null
            ) {
                Text("Enregistrer", color = OrangeEnergy)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
