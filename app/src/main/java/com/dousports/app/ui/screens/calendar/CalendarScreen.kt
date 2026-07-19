package com.dousports.app.ui.screens.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dousports.app.data.local.entity.WorkoutSessionEntity
import com.dousports.app.data.local.entity.WorkoutSetEntity
import com.dousports.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private val DAY_HEADERS = listOf("L", "M", "M", "J", "V", "S", "D")

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Calendrier",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        item {
            CalendarCard(
                year = state.year,
                month = state.month,
                sessionsByDay = state.sessionsByDay,
                selectedDay = state.selectedDay,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onDayClick = viewModel::selectDay
            )
        }

        if (state.selectedDay != null) {
            val sessions = state.sessionsByDay[state.selectedDay] ?: emptyList()

            if (sessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aucune séance ce jour", color = TextSecondary)
                    }
                }
            } else {
                item {
                    Text(
                        "Séances du ${state.selectedDay} ${monthName(state.month)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                items(sessions) { session ->
                    SessionCard(
                        session = session,
                        isExpanded = state.selectedSession?.id == session.id,
                        sets = if (state.selectedSession?.id == session.id) state.setsForSession else emptyList(),
                        isLoadingSets = state.isLoadingSets,
                        onClick = { viewModel.selectSession(session) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarCard(
    year: Int,
    month: Int,
    sessionsByDay: Map<Int, List<WorkoutSessionEntity>>,
    selectedDay: Int?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month navigation header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mois précédent", tint = TextPrimary)
                }
                Text(
                    "${monthName(month)} $year",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Mois suivant", tint = TextPrimary)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Day-of-week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                DAY_HEADERS.forEach { h ->
                    Text(
                        h,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Calendar grid
            val cal = Calendar.getInstance().apply {
                set(year, month - 1, 1)
                set(Calendar.HOUR_OF_DAY, 0)
            }
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            // Monday = 0 offset (Calendar returns Sunday=1)
            val firstDow = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            val totalCells = firstDow + daysInMonth
            val rows = (totalCells + 6) / 7
            val today = Calendar.getInstance()
            val isCurrentMonth = today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) + 1 == month

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val day = cellIndex - firstDow + 1
                            val isCurrentDay = isCurrentMonth && day == today.get(Calendar.DAY_OF_MONTH)
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (day in 1..daysInMonth) {
                                    DayCell(
                                        day = day,
                                        hasSession = sessionsByDay.containsKey(day),
                                        sessionCount = sessionsByDay[day]?.size ?: 0,
                                        isSelected = selectedDay == day,
                                        isToday = isCurrentDay,
                                        onClick = { onDayClick(day) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Legend
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(OrangeEnergy))
                    Text("Séance", fontSize = 11.sp, color = TextSecondary)
                }
                val total = sessionsByDay.values.sumOf { it.size }
                if (total > 0) {
                    Text("$total séance${if (total > 1) "s" else ""} ce mois", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    hasSession: Boolean,
    sessionCount: Int,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val bg = when {
        isSelected -> OrangeEnergy
        isToday    -> OrangeEnergy.copy(alpha = 0.2f)
        else       -> Color.Transparent
    }
    val textColor = when {
        isSelected -> Color.White
        isToday    -> OrangeEnergy
        else       -> TextPrimary
    }

    Column(
        modifier = Modifier
            .padding(2.dp)
            .clip(CircleShape)
            .background(bg)
            .then(
                if (isToday && !isSelected)
                    Modifier.border(1.dp, OrangeEnergy, CircleShape)
                else Modifier
            )
            .clickable(enabled = hasSession || true, onClick = onClick)
            .size(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            day.toString(),
            fontSize = 13.sp,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            lineHeight = 13.sp
        )
        if (hasSession) {
            Spacer(Modifier.height(1.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(sessionCount.coerceAtMost(3)) {
                    Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else OrangeEnergy)
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: WorkoutSessionEntity,
    isExpanded: Boolean,
    sets: List<WorkoutSetEntity>,
    isLoadingSets: Boolean,
    onClick: () -> Unit
) {
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.FRENCH) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        session.routineName.ifBlank { "Séance libre" },
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            timeFmt.format(Date(session.startedAt)),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        if (session.durationSeconds > 0) {
                            Text("•", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                durationLabel(session.durationSeconds),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(color = CardDarker)
                    if (isLoadingSets) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = OrangeEnergy, modifier = Modifier.size(24.dp))
                        }
                    } else if (sets.isEmpty()) {
                        Text(
                            "Aucune série enregistrée",
                            modifier = Modifier.padding(16.dp),
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    } else {
                        SetsTable(sets)
                    }
                }
            }
        }
    }
}

@Composable
private fun SetsTable(sets: List<WorkoutSetEntity>) {
    val byExercise = sets.groupBy { it.exerciseName }
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        byExercise.forEach { (exerciseName, exerciseSets) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    exerciseName,
                    fontWeight = FontWeight.SemiBold,
                    color = OrangeEnergy,
                    fontSize = 13.sp
                )
                // Header
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Série", Modifier.width(44.dp), fontSize = 11.sp, color = TextSecondary)
                    Text("Reps", Modifier.width(44.dp), fontSize = 11.sp, color = TextSecondary)
                    Text("Poids", fontSize = 11.sp, color = TextSecondary)
                }
                exerciseSets.forEach { s ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${s.setNumber}",
                            modifier = Modifier.width(44.dp),
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            "${s.reps}",
                            modifier = Modifier.width(44.dp),
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            if (s.weight > 0) "%.1f kg".format(s.weight) else "—",
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

private fun monthName(month: Int): String = when (month) {
    1 -> "Janvier"; 2 -> "Février"; 3 -> "Mars"; 4 -> "Avril"
    5 -> "Mai"; 6 -> "Juin"; 7 -> "Juillet"; 8 -> "Août"
    9 -> "Septembre"; 10 -> "Octobre"; 11 -> "Novembre"; 12 -> "Décembre"
    else -> ""
}

private fun durationLabel(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}min${if (s > 0) " ${s}s" else ""}" else "${s}s"
}
