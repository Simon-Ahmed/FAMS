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
import com.fams.app.ui.components.SelectableItem
import com.fams.app.ui.components.SelectionModeHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAssignTab(uiState: ScheduleUiState, viewModel: ScheduleViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Assignments", "Unavailability")
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { i, title ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(title) })
            }
        }
        when (selectedTab) {
            0 -> TeacherSubjectsPanel(uiState, viewModel)
            1 -> UnavailabilityPanel(uiState, viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnavailabilityPanel(uiState: ScheduleUiState, viewModel: ScheduleViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isSelectionMode) {
            SelectionModeHeader(
                totalCount = uiState.unavailabilities.size, selectedCount = selected.size,
                onSelectAll = { selected.clear(); selected.addAll(uiState.unavailabilities.map { it.id }) },
                onDeselectAll = { selected.clear() },
                onDeleteSelected = { showDeleteConfirm = true },
                onExitSelection = { isSelectionMode = false; selected.clear() }
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isSelectionMode) {
                    item {
                        Text("Teacher Unavailability", style = MaterialTheme.typography.titleMedium)
                        Text("Long press an entry to select and delete multiple.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                items(uiState.unavailabilities) { u ->
                    val teacher = uiState.teachers.find { it.uid == u.teacherId }
                    val slot = uiState.timeSlots.find { it.id == u.timeSlotId }
                    SelectableItem(
                        id = u.id, isSelectionMode = isSelectionMode, isSelected = u.id in selected,
                        onLongPress = { isSelectionMode = true; selected.add(u.id) },
                        onToggle = { if (u.id in selected) selected.remove(u.id) else selected.add(u.id) }
                    ) {
                        ListItemCard(
                            title = teacher?.fullName ?: "Unknown Teacher",
                            subtitle = slot?.let { "${it.day} ${it.startTime}–${it.endTime}" } ?: "Unknown Slot",
                            onDelete = if (!isSelectionMode) ({ viewModel.removeUnavailability(u.id) }) else null
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
            title = { Text("Remove ${selected.size} Unavailabilities?") },
            text = { Text("Remove selected unavailability entries?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.removeUnavailabilities(selected.toList())
                    selected.clear(); isSelectionMode = false; showDeleteConfirm = false
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
    if (showDialog) {
        AddUnavailabilityDialog(teachers = uiState.teachers, timeSlots = uiState.timeSlots,
            onDismiss = { showDialog = false },
            onConfirm = { tid, sid -> viewModel.addUnavailability(tid, sid); showDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUnavailabilityDialog(
    teachers: List<com.fams.app.domain.model.User>,
    timeSlots: List<com.fams.app.domain.model.TimeSlot>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var selectedTeacherId by remember { mutableStateOf("") }
    var selectedSlotId by remember { mutableStateOf("") }
    var teacherExpanded by remember { mutableStateOf(false) }
    var slotExpanded by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Unavailability") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = teacherExpanded, onExpandedChange = { teacherExpanded = it }) {
                    OutlinedTextField(
                        value = teachers.find { it.uid == selectedTeacherId }?.fullName ?: "Select Teacher",
                        onValueChange = {}, readOnly = true, label = { Text("Teacher") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(teacherExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = teacherExpanded, onDismissRequest = { teacherExpanded = false }) {
                        teachers.forEach { t ->
                            DropdownMenuItem(text = { Text(t.fullName) },
                                onClick = { selectedTeacherId = t.uid; teacherExpanded = false })
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = slotExpanded, onExpandedChange = { slotExpanded = it }) {
                    OutlinedTextField(
                        value = timeSlots.find { it.id == selectedSlotId }
                            ?.let { "${it.day} ${it.startTime}–${it.endTime}" } ?: "Select Time Slot",
                        onValueChange = {}, readOnly = true, label = { Text("Time Slot") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(slotExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = slotExpanded, onDismissRequest = { slotExpanded = false }) {
                        timeSlots.forEach { slot ->
                            DropdownMenuItem(
                                text = { Text("${slot.day} ${slot.startTime}–${slot.endTime}") },
                                onClick = { selectedSlotId = slot.id; slotExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (selectedTeacherId.isNotBlank() && selectedSlotId.isNotBlank())
                    onConfirm(selectedTeacherId, selectedSlotId)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
