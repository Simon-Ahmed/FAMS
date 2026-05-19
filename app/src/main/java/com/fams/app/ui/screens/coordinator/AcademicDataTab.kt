package com.fams.app.ui.screens.coordinator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fams.app.ui.components.SelectableItem
import com.fams.app.ui.components.SelectionModeHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicDataTab(uiState: ScheduleUiState, viewModel: ScheduleViewModel) {
    var section by remember { mutableIntStateOf(0) }
    val tabs = listOf("Subjects", "Rooms", "Time Slots")
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = section) {
            tabs.forEachIndexed { i, title ->
                Tab(selected = section == i, onClick = { section = i }, text = { Text(title) })
            }
        }
        when (section) {
            0 -> SubjectsSection(uiState, viewModel)
            1 -> RoomsSection(uiState, viewModel)
            2 -> TimeSlotsSection(uiState, viewModel)
        }
    }
}

// ── Subjects ──────────────────────────────────────────────────────────────────

@Composable
fun SubjectsSection(uiState: ScheduleUiState, viewModel: ScheduleViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isSelectionMode) {
            SelectionModeHeader(
                totalCount = uiState.subjects.size,
                selectedCount = selected.size,
                onSelectAll = { selected.clear(); selected.addAll(uiState.subjects.map { it.id }) },
                onDeselectAll = { selected.clear() },
                onDeleteSelected = { showDeleteConfirm = true },
                onExitSelection = { isSelectionMode = false; selected.clear() }
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isSelectionMode) {
                    item { Text("Subjects (${uiState.subjects.size})", style = MaterialTheme.typography.titleMedium) }
                }
                items(uiState.subjects) { subject ->
                    SelectableItem(
                        id = subject.id,
                        isSelectionMode = isSelectionMode,
                        isSelected = subject.id in selected,
                        onLongPress = { isSelectionMode = true; selected.add(subject.id) },
                        onToggle = { if (subject.id in selected) selected.remove(subject.id) else selected.add(subject.id) }
                    ) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(subject.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${subject.creditHours} credit hours",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (subject.description.isNotBlank())
                                        Text(subject.description, style = MaterialTheme.typography.bodySmall)
                                }
                                if (!isSelectionMode) {
                                    IconButton(onClick = { viewModel.deleteSubject(subject.id) }) {
                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) { Icon(Icons.Default.Add, null) }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${selected.size} Subjects?") },
            text = { Text("This will permanently delete the selected subjects.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteSubjects(selected.toList())
                    selected.clear(); isSelectionMode = false; showDeleteConfirm = false
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
    if (showDialog) {
        AddSubjectDialog(onDismiss = { showDialog = false },
            onConfirm = { name, desc, hours -> viewModel.addSubject(name, desc, hours); showDialog = false })
    }
}

@Composable
fun AddSubjectDialog(onDismiss: () -> Unit, onConfirm: (String, String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("3") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Subject Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it },
                    label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = hours, onValueChange = { hours = it },
                    label = { Text("Credit Hours") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("e.g. 3 = 3 sessions per week") })
            }
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onConfirm(name, desc, hours.toIntOrNull() ?: 3) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Rooms ─────────────────────────────────────────────────────────────────────

@Composable
fun RoomsSection(uiState: ScheduleUiState, viewModel: ScheduleViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isSelectionMode) {
            SelectionModeHeader(
                totalCount = uiState.rooms.size, selectedCount = selected.size,
                onSelectAll = { selected.clear(); selected.addAll(uiState.rooms.map { it.id }) },
                onDeselectAll = { selected.clear() },
                onDeleteSelected = { showDeleteConfirm = true },
                onExitSelection = { isSelectionMode = false; selected.clear() }
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isSelectionMode) {
                    item { Text("Rooms (${uiState.rooms.size})", style = MaterialTheme.typography.titleMedium) }
                }
                items(uiState.rooms) { room ->
                    SelectableItem(id = room.id, isSelectionMode = isSelectionMode, isSelected = room.id in selected,
                        onLongPress = { isSelectionMode = true; selected.add(room.id) },
                        onToggle = { if (room.id in selected) selected.remove(room.id) else selected.add(room.id) }
                    ) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(room.name, style = MaterialTheme.typography.titleMedium)
                                    Text("Capacity: ${room.capacity}", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (!isSelectionMode) {
                                    IconButton(onClick = { viewModel.deleteRoom(room.id) }) {
                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
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
            title = { Text("Delete ${selected.size} Rooms?") },
            text = { Text("Permanently delete selected rooms?") },
            confirmButton = {
                Button(onClick = { viewModel.deleteRooms(selected.toList()); selected.clear(); isSelectionMode = false; showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
    if (showDialog) {
        AddRoomDialog(onDismiss = { showDialog = false },
            onConfirm = { name, cap -> viewModel.addRoom(name, cap); showDialog = false })
    }
}

@Composable
fun AddRoomDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("30") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Room") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Room Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = capacity, onValueChange = { capacity = it },
                    label = { Text("Capacity") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onConfirm(name, capacity.toIntOrNull() ?: 30) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Time Slots ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSlotsSection(uiState: ScheduleUiState, viewModel: ScheduleViewModel) {
    var showAutoDialog by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val days = listOf("Monday","Tuesday","Wednesday","Thursday","Friday")
    val grouped = uiState.timeSlots.groupBy { it.day }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isSelectionMode) {
            SelectionModeHeader(
                totalCount = uiState.timeSlots.size, selectedCount = selected.size,
                onSelectAll = { selected.clear(); selected.addAll(uiState.timeSlots.map { it.id }) },
                onDeselectAll = { selected.clear() },
                onDeleteSelected = { showDeleteConfirm = true },
                onExitSelection = { isSelectionMode = false; selected.clear() }
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    if (!isSelectionMode) {
                        Text("Time Slots (${uiState.timeSlots.size})", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                    }
                    OutlinedButton(onClick = { showAutoDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Auto-Generate Time Slots")
                    }
                    Spacer(Modifier.height(8.dp))
                }
                days.forEach { day ->
                    val slots = grouped[day] ?: emptyList()
                    if (slots.isNotEmpty()) {
                        item {
                            Text(day, style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                        }
                        items(slots) { slot ->
                            SelectableItem(id = slot.id, isSelectionMode = isSelectionMode, isSelected = slot.id in selected,
                                onLongPress = { isSelectionMode = true; selected.add(slot.id) },
                                onToggle = { if (slot.id in selected) selected.remove(slot.id) else selected.add(slot.id) }
                            ) {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Text("${slot.startTime} – ${slot.endTime}", style = MaterialTheme.typography.bodyMedium)
                                        if (!isSelectionMode) {
                                            IconButton(onClick = { viewModel.deleteTimeSlot(slot.id) }) {
                                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            FloatingActionButton(onClick = { showManualDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) { Icon(Icons.Default.Add, null) }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${selected.size} Time Slots?") },
            text = { Text("Permanently delete selected time slots?") },
            confirmButton = {
                Button(onClick = { viewModel.deleteTimeSlots(selected.toList()); selected.clear(); isSelectionMode = false; showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
    if (showAutoDialog) {
        AutoGenerateTimeSlotsDialog(onDismiss = { showAutoDialog = false },
            onConfirm = { days2, s, e, d -> viewModel.autoGenerateTimeSlots(days2, s, e, d); showAutoDialog = false })
    }
    if (showManualDialog) {
        AddTimeSlotDialog(onDismiss = { showManualDialog = false },
            onConfirm = { day, start, end -> viewModel.addTimeSlot(day, start, end); showManualDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoGenerateTimeSlotsDialog(onDismiss: () -> Unit, onConfirm: (List<String>, Int, Int, Int) -> Unit) {
    val allDays = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")
    val selectedDays = remember { mutableStateListOf("Monday","Tuesday","Wednesday","Thursday","Friday") }
    var startHour by remember { mutableStateOf("08") }
    var endHour by remember { mutableStateOf("17") }
    var duration by remember { mutableStateOf("90") }
    val start = startHour.toIntOrNull() ?: 8
    val end = endHour.toIntOrNull() ?: 17
    val dur = duration.toIntOrNull() ?: 90
    val periodsPerDay = if (dur > 0) ((end - start) * 60) / dur else 0

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Auto-Generate Time Slots") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select days:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    allDays.forEach { day ->
                        val sel = day in selectedDays
                        FilterChip(selected = sel, onClick = { if (sel) selectedDays.remove(day) else selectedDays.add(day) },
                            label = { Text(day.take(3)) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startHour, onValueChange = { startHour = it }, label = { Text("Start Hour") },
                        modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), supportingText = { Text("e.g. 08") })
                    OutlinedTextField(value = endHour, onValueChange = { endHour = it }, label = { Text("End Hour") },
                        modifier = Modifier.weight(1f), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), supportingText = { Text("e.g. 17") })
                }
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Period Duration (min)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                if (periodsPerDay > 0 && selectedDays.isNotEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Text("${selectedDays.size} days × $periodsPerDay periods = ${selectedDays.size * periodsPerDay} slots",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (selectedDays.isNotEmpty() && periodsPerDay > 0) onConfirm(selectedDays.toList(), start, end, dur) }) {
                Text("Generate")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimeSlotDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    val days = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")
    var selectedDay by remember { mutableStateOf(days[0]) }
    var dayExpanded by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("09:30") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Time Slot") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = dayExpanded, onExpandedChange = { dayExpanded = it }) {
                    OutlinedTextField(value = selectedDay, onValueChange = {}, readOnly = true, label = { Text("Day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dayExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                        days.forEach { day -> DropdownMenuItem(text = { Text(day) }, onClick = { selectedDay = day; dayExpanded = false }) }
                    }
                }
                OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Start (HH:MM)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("End (HH:MM)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { if (startTime.isNotBlank()) onConfirm(selectedDay, startTime, endTime) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
