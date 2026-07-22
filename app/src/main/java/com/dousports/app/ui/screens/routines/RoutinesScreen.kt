package com.dousports.app.ui.screens.routines

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dousports.app.data.local.entity.RoutineEntity
import com.dousports.app.ui.theme.OrangeEnergy
import com.dousports.app.utils.toFormattedDate

@Composable
fun RoutinesScreen(
    autoImportCode: String? = null,
    onAutoImportConsumed: () -> Unit = {},
    onCreateRoutine: () -> Unit,
    onEditRoutine: (Long) -> Unit,
    onStartRoutine: (Long, Boolean) -> Unit,
    onNavigateToSchedule: () -> Unit = {},
    onOpenQrScanner: () -> Unit = {},
    viewModel: RoutinesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var deleteTarget by remember { mutableStateOf<RoutineEntity?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importInitialCode by remember { mutableStateOf("") }

    LaunchedEffect(autoImportCode) {
        if (!autoImportCode.isNullOrBlank()) {
            importInitialCode = autoImportCode
            viewModel.previewImport(autoImportCode)
            showImportDialog = true
            onAutoImportConsumed()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateRoutine,
                containerColor = OrangeEnergy,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) {
                Icon(Icons.Default.Add, "Nouvelle routine")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    "Mes Routines",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            importInitialCode = ""
                            showImportDialog = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeEnergy),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangeEnergy),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Importer", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onOpenQrScanner,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeEnergy),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangeEnergy),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Scanner", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onNavigateToSchedule,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeEnergy),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangeEnergy),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Planning", fontSize = 13.sp)
                    }
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OrangeEnergy)
                }
            } else if (uiState.routines.isEmpty()) {
                EmptyState(onCreate = onCreateRoutine)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.routines, key = { it.id }) { routine ->
                        RoutineListCard(
                            routine = routine,
                            onStart = { onStartRoutine(routine.id, routine.isTimed) },
                            onEdit = { onEditRoutine(routine.id) },
                            onDelete = { deleteTarget = routine },
                            onDuplicate = { viewModel.duplicateRoutine(routine.id) },
                            onShare = { viewModel.shareRoutine(routine.id) }
                        )
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    deleteTarget?.let { routine ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Supprimer cette routine ?") },
            text = { Text("La routine \"${routine.name}\" sera supprimée définitivement.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRoutine(routine)
                        deleteTarget = null
                    }
                ) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Annuler") }
            }
        )
    }

    // Share dialog
    val shareCode = uiState.shareCode
    val shareQr = uiState.shareQr
    val shareRoutineName = uiState.shareRoutineName
    if (shareCode != null && shareRoutineName != null) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = viewModel::clearShare,
            title = { Text("Partager — $shareRoutineName") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (shareQr != null) {
                        Image(
                            bitmap = shareQr.asImageBitmap(),
                            contentDescription = "QR code",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Text(
                        "Scanne le QR code ou copie le code ci-dessous pour partager cette routine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = shareCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Code de partage") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeEnergy
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("routine", shareCode))
                        viewModel.clearShare()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeEnergy)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copier le code")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::clearShare) { Text("Fermer") }
            }
        )
    }

    // Import dialog
    if (showImportDialog) {
        ImportRoutineDialog(
            initialCode = importInitialCode,
            importPreview = uiState.importPreview,
            importError = uiState.importError,
            onPreview = viewModel::previewImport,
            onConfirm = {
                viewModel.confirmImport()
                showImportDialog = false
                importInitialCode = ""
            },
            onDismiss = {
                viewModel.clearImport()
                showImportDialog = false
                importInitialCode = ""
            }
        )
    }
}

@Composable
private fun ImportRoutineDialog(
    initialCode: String = "",
    importPreview: com.dousports.app.utils.RoutineShareDto?,
    importError: String?,
    onPreview: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var codeInput by remember { mutableStateOf(initialCode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importer une routine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    label = { Text("Code de partage") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OrangeEnergy),
                    keyboardOptions = KeyboardOptions.Default
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            codeInput = text
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeEnergy),
                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangeEnergy)
                    ) {
                        Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Coller", fontSize = 13.sp)
                    }
                    Button(
                        onClick = { onPreview(codeInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeEnergy),
                        enabled = codeInput.isNotBlank()
                    ) { Text("Vérifier") }
                }

                if (importError != null) {
                    Text(importError, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }

                if (importPreview != null) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(importPreview.name, fontWeight = FontWeight.Bold)
                            if (importPreview.description.isNotBlank()) {
                                Text(importPreview.description, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                "${importPreview.exercises.size} exercice(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = OrangeEnergy
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (importPreview != null) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeEnergy)
                ) { Text("Importer") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun RoutineListCard(
    routine: RoutineEntity,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit = {},
    onShare: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OrangeEnergy.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FitnessCenter, null, tint = OrangeEnergy, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    routine.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (routine.description.isNotBlank()) {
                    Text(
                        routine.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                routine.lastPerformedAt?.let {
                    Text(
                        "Dernière fois : ${it.toFormattedDate()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onStart) {
                Icon(Icons.Default.PlayArrow, "Démarrer", tint = OrangeEnergy)
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, "Options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Modifier") },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { menuExpanded = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Dupliquer") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                        onClick = { menuExpanded = false; onDuplicate() }
                    )
                    DropdownMenuItem(
                        text = { Text("Partager") },
                        leadingIcon = { Icon(Icons.Default.Share, null) },
                        onClick = { menuExpanded = false; onShare() }
                    )
                    DropdownMenuItem(
                        text = { Text("Supprimer", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete, null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onCreate: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.AddCircleOutline, null,
                tint = OrangeEnergy,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Aucune routine",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Créez votre premier programme d'entraînement",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onCreate,
                colors = ButtonDefaults.buttonColors(containerColor = OrangeEnergy)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Créer une routine")
            }
        }
    }
}
