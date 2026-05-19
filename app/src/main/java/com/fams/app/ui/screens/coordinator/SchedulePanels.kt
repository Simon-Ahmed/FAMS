package com.fams.app.ui.screens.coordinator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fams.app.domain.model.*
import com.fams.app.ui.components.SelectableItem
import com.fams.app.ui.components.SelectionModeHeader

// ── Teacher-Subject Panel ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherSubjectsPanel(uiState: ScheduleUiState, viewModel: ScheduleViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    val teacherById = uiState.teachers.associateBy { it.uid }
    val subjectById = uiState.subjects.associateBy { it.id }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isSelectionMode) {
            SelectionModeHeader(
                totalCount = uiState.teacherSubjects.size, selectedCount = selected.size,
                onSelectAll = { selected.clear(); selected.addAll(uiState.teacherSubjects.map { it.id }) },
                onDeselectAll = { selected.clear() },
                onDeleteSelected = { showDeleteConfirm = true },
                onExitSelection = { isSelectionMode = false; selected.clear() }
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isSelectionMode) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Assigned Teachers", style = MaterialTheme.typography.titleMedium)
                            Text("Each row shows a teacher and the subject they are assigned to.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(uiState.teacherSubjects) { ts ->
                    val teacher = teacherById[ts.teacherId]
                    val subject = subjectById[ts.subjectId]
                    SelectableItem(id = ts.id, isSelectionMode = isSelectionMode, isSelected = ts.id in selected,
                        onLongPress = { isSelectionMode = true; selected.add(ts.id) },
                        onToggle = { if (ts.id in selected) selected.remove(ts.id) else selected.add(ts.id) }
                    ) {
                        ListItemCard(
                            title = teacher?.fullName ?: "Unknown Teacher",
                            subtitle = subject?.name ?: "Unknown Subject",
                            onDelete = if (!isSelectionMode) ({ viewModel.removeTeacherSubject(ts.id) }) else null
                        )
                    }
                }
            }
            FloatingActionButton(onClick = { showDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) { Icon(Icons.Default.Add, null) }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove ${selected.size} Assignments?") },
            text = { Text("Remove selected teacher-subject assignments?") },
            confirmButton = {
                Button(onClick = { viewModel.removeTeacherSubjects(selected.toList()); selected.clear(); isSelectionMode = false; showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
    if (showDialog) {
        AssignTeacherSubjectDialog(teachers = uiState.teachers, subjects = uiState.subjects,
            onDismiss = { showDialog = false },
            onConfirm = { tid, sid -> viewModel.assignTeacherToSubject(tid, sid); showDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignTeacherSubjectDialog(
    teachers: List<User>,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var selectedTeacherId by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf("") }
    var teacherExpanded by remember { mutableStateOf(false) }
    var subjectExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Teacher to Subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = teacherExpanded, onExpandedChange = { teacherExpanded = it }) {
                    OutlinedTextField(
                        value = teachers.find { it.uid == selectedTeacherId }?.fullName ?: "Select Teacher",
                        onValueChange = {}, readOnly = true, label = { Text("Teacher") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(teacherExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = teacherExpanded, onDismissRequest = { teacherExpanded = false }) {
                        teachers.forEach { t ->
                            DropdownMenuItem(text = { Text(t.fullName) },
                                onClick = { selectedTeacherId = t.uid; teacherExpanded = false })
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = subjectExpanded, onExpandedChange = { subjectExpanded = it }) {
                    OutlinedTextField(
                        value = subjects.find { it.id == selectedSubjectId }?.name ?: "Select Subject",
                        onValueChange = {}, readOnly = true, label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(subjectExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = subjectExpanded, onDismissRequest = { subjectExpanded = false }) {
                        subjects.forEach { s ->
                            DropdownMenuItem(text = { Text(s.name) },
                                onClick = { selectedSubjectId = s.id; subjectExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (selectedTeacherId.isNotBlank() && selectedSubjectId.isNotBlank())
                    onConfirm(selectedTeacherId, selectedSubjectId)
            }) { Text("Assign") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Generate Panel ────────────────────────────────────────────────────────────

@Composable
fun GeneratePanel(uiState: ScheduleUiState, viewModel: ScheduleViewModel) {

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Constraints", style = MaterialTheme.typography.titleMedium) }

        item {
            ConstraintSlider(
                label = "Max classes per day per section: ${uiState.maxClassesPerDay}",
                value = uiState.maxClassesPerDay.toFloat(),
                range = 1f..8f,
                onValueChange = { viewModel.setMaxClassesPerDay(it.toInt()) }
            )
        }

        item { HorizontalDivider() }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Input Summary", style = MaterialTheme.typography.titleSmall)
                    Text("Sections: ${uiState.sections.size}")
                    Text("Subjects: ${uiState.subjects.size} (${uiState.subjects.sumOf { it.creditHours }} total credit hours)")
                    Text("Teachers: ${uiState.teachers.size}")
                    Text("Rooms: ${uiState.rooms.size}")
                    Text("Time Slots: ${uiState.timeSlots.size}")
                    Text("Teacher-Subject links: ${uiState.teacherSubjects.size}")
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.generateSchedule() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !uiState.isGenerating
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Generating...")
                } else {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Generate Timetable")
                }
            }
        }

        if (uiState.unscheduledWarnings.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Unscheduled Items", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        uiState.unscheduledWarnings.forEach { w ->
                            Text("• $w", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }

        if (uiState.generatedEntries.isNotEmpty()) {
            item {
                Text("Generated Schedule (${uiState.generatedEntries.size} sessions)",
                    style = MaterialTheme.typography.titleMedium)
            }
            item {
                WeeklyGridView(entries = uiState.generatedEntries)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.generateSchedule() },
                        modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Regenerate")
                    }
                    Button(onClick = { viewModel.publishSchedule() },
                        modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Publish, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Publish")
                    }
                }
            }
        }

        if (uiState.publishedEntries.isNotEmpty() && uiState.generatedEntries.isEmpty()) {
            item {
                Text("Published Schedule (${uiState.publishedEntries.size} sessions)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            item {
                WeeklyGridView(entries = uiState.publishedEntries)
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.deletePublishedSchedule() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Published Schedule")
                }
            }
        }
    }
}

// ── Shared components ─────────────────────────────────────────────────────────

@Composable
fun ConstraintSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        val steps = ((range.endInclusive - range.start).toInt() - 1).coerceAtLeast(0)
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps)
    }
}

@Composable
fun ListItemCard(title: String, subtitle: String, onDelete: (() -> Unit)?) {
    var showConfirm by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onDelete != null) {
                IconButton(onClick = { showConfirm = true }) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (showConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete") },
            text = { Text("Delete '$title'?") },
            confirmButton = {
                Button(onClick = { onDelete(); showConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } }
        )
    }
}
