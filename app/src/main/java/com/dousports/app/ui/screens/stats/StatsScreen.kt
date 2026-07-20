package com.dousports.app.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dousports.app.data.local.entity.WorkoutSessionEntity
import com.dousports.app.ui.theme.GreenSuccess
import com.dousports.app.ui.theme.NavyDark
import com.dousports.app.ui.theme.OrangeEnergy
import com.dousports.app.ui.theme.TextPrimary
import com.dousports.app.ui.theme.TextSecondary
import com.dousports.app.ui.theme.CardDark
import com.dousports.app.utils.toDurationString
import com.dousports.app.utils.toFormattedDate
import com.dousports.app.utils.toFormattedTime

private val Gold = Color(0xFFD29922)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: (() -> Unit)? = null,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = { Text("Statistiques", color = MaterialTheme.colorScheme.onBackground) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Retour", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        if (onBack == null) {
        item {
            Text(
                "Statistiques",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }
        }

        item {
            var selectedTab by remember { mutableStateOf(0) }
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = OrangeEnergy,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Cette semaine", modifier = Modifier.padding(vertical = 12.dp), color = if (selectedTab == 0) OrangeEnergy else TextSecondary)
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("Ce mois", modifier = Modifier.padding(vertical = 12.dp), color = if (selectedTab == 1) OrangeEnergy else TextSecondary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BigStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FitnessCenter,
                        value = if (selectedTab == 0) uiState.weeklyCount.toString() else uiState.monthlyCount.toString(),
                        label = "Séances",
                        color = OrangeEnergy
                    )
                    BigStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Scale,
                        value = formatVolume(if (selectedTab == 0) uiState.weeklyVolume else uiState.monthlyVolume),
                        label = "Volume",
                        color = GreenSuccess
                    )
                    BigStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.EmojiEvents,
                        value = uiState.totalSessions.toString(),
                        label = "Total",
                        color = Gold
                    )
                }
            }
        }

        if (uiState.totalSetsAllTime > 0) {
            item { Spacer(Modifier.height(24.dp)) }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniStatCard(
                        modifier = Modifier.weight(1f),
                        value = uiState.totalSetsAllTime.toString(),
                        label = "Séries totales"
                    )
                    MiniStatCard(
                        modifier = Modifier.weight(1f),
                        value = formatVolume(uiState.weeklyVolume + uiState.monthlyVolume),
                        label = "Volume total"
                    )
                }
            }
        }

        if (uiState.muscleGroups.isNotEmpty()) {
            item { Spacer(Modifier.height(24.dp)) }
            item {
                SectionTitle("Groupes musculaires", Icons.Default.AccessibilityNew)
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.muscleGroups.forEach { mg ->
                            MuscleGroupRow(mg)
                        }
                    }
                }
            }
        }

        if (uiState.topExercises.isNotEmpty()) {
            item { Spacer(Modifier.height(24.dp)) }
            item {
                SectionTitle("Top exercices", Icons.Default.BarChart)
            }
            items(uiState.topExercises) { ex ->
                ExerciseStatCard(
                    ex,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        if (uiState.personalRecords.isNotEmpty()) {
            item { Spacer(Modifier.height(24.dp)) }
            item {
                SectionTitle("Records personnels", Icons.Default.EmojiEvents)
            }
            items(uiState.personalRecords) { pr ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Gold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.EmojiEvents, null, tint = Gold, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(pr.exerciseName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
                        Text("%.1f kg".format(pr.maxWeight), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OrangeEnergy)
                    }
                }
            }
        }

        if (uiState.recentSessions.isNotEmpty()) {
            item { Spacer(Modifier.height(24.dp)) }
            item {
                SectionTitle("Historique", Icons.Default.History)
            }
            items(uiState.recentSessions) { session ->
                SessionCard(session, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }

        if (uiState.recentSessions.isEmpty() && !uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BarChart, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Aucune séance terminée", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Vos statistiques apparaîtront ici après votre première séance.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
    } // end Scaffold
}

@Composable
private fun SectionTitle(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = OrangeEnergy, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun BigStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MiniStatCard(modifier: Modifier = Modifier, value: String, label: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OrangeEnergy)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MuscleGroupRow(mg: MuscleGroupStats) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(mg.name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
            Text("${mg.totalSets} séries · ${"%.0f".format(mg.percentage * 100)}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(OrangeEnergy.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(mg.percentage)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(OrangeEnergy)
            )
        }
    }
}

@Composable
private fun ExerciseStatCard(ex: ExerciseStats, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OrangeEnergy.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FitnessCenter, null, tint = OrangeEnergy, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(ex.exerciseName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "${ex.totalSets} séries · ${ex.totalReps} reps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(formatVolume(ex.totalVolume), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = OrangeEnergy)
        }
    }
}

@Composable
private fun SessionCard(session: WorkoutSessionEntity, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (session.finishedAt != null) GreenSuccess.copy(0.15f) else OrangeEnergy.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (session.finishedAt != null) Icons.Default.CheckCircle else Icons.Default.Pending,
                    null,
                    tint = if (session.finishedAt != null) GreenSuccess else OrangeEnergy
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(session.routineName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "${session.startedAt.toFormattedDate()} · ${session.startedAt.toFormattedTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (session.durationSeconds > 0) {
                Text(session.durationSeconds.toDurationString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatVolume(volume: Float): String = when {
    volume >= 1000 -> "%.1f t".format(volume / 1000f)
    else -> "%.0f kg".format(volume)
}
