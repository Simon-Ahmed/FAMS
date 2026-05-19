package com.fams.app.ui.screens.coordinator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fams.app.domain.model.Section
import com.fams.app.ui.components.SelectableItem
import com.fams.app.ui.components.SelectionModeHeader
import kotlin.math.ceil
import androidx.compose.material3.ExperimentalMaterial3Api


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionsTab(uiState: CoordinatorUiState, viewModel: CoordinatorViewModel) {
    var showAutoDialog by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterSectionId by remember { mutableStateOf("All") }
    var sectionFilterExpanded by remember { mutableStateOf(false) }
    val selectedSections = remember { mutableStateListOf<String>() }

    val filteredSections by remember(uiState.sections, searchQuery, selectedFilterSectionId) {
        derivedStateOf {
            uiState.sections.filter { section ->
                val matchesSearch = searchQuery.isBlank() ||
                    section.name.contains(searchQuery, ignoreCase = true) ||
                    section.id.contains(searchQuery, ignoreCase = true)
                val matchesFilter = selectedFilterSectionId == "All" || section.id == selectedFilterSectionId
                matchesSearch && matchesFilter
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isSelectionMode) {
            SelectionModeHeader(
                totalCount = filteredSections.size,
                selectedCount = selectedSections.size,
                onSelectAll = { selectedSections.clear(); selectedSections.addAll(filteredSections.map { it.id }) },
                onDeselectAll = { selectedSections.clear() },
                onDeleteSelected = { showDeleteConfirm = true },
                onExitSelection = { isSelectionMode = false; selectedSections.clear() }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        if (!isSelectionMode) {
                            Text("Sections (${filteredSections.size}/${uiState.sections.size})", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Search sections") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                                        }
                                    }
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = sectionFilterExpanded,
                                onExpandedChange = { sectionFilterExpanded = it }
                            ) {
                                val sectionFilterLabel = if (selectedFilterSectionId == "All") "All sections" else uiState.sections.find { it.id == selectedFilterSectionId }?.name ?: "All sections"
                                OutlinedTextField(
                                    value = sectionFilterLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Filter by section") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sectionFilterExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = sectionFilterExpanded,
                                    onDismissRequest = { sectionFilterExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All sections") },
                                        onClick = {
                                            selectedFilterSectionId = "All"
                                            sectionFilterExpanded = false
                                        }
                                    )
                                    uiState.sections.forEach { section ->
                                        DropdownMenuItem(
                                            text = { Text(section.name) },
                                            onClick = {
                                                selectedFilterSectionId = section.id
                                                sectionFilterExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = { showConfigDialog = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Max Students per Section: ${uiState.maxStudentsPerSection}")
                            }
                            Spacer(Modifier.height(4.dp))
                            OutlinedButton(onClick = { showAutoDialog = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Auto-Generate from Student Count")
                            }
                            Spacer(Modifier.height(4.dp))
                            val unassignedCount = uiState.students.count { it.sectionId.isBlank() }
                            Button(
                                onClick = { viewModel.autoAssignStudentsToSections() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = unassignedCount > 0 && uiState.sections.isNotEmpty()
                            ) {
                                Icon(Icons.Default.SortByAlpha, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (unassignedCount > 0) "Auto-Assign $unassignedCount Students" else "All Students Assigned")
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    if (filteredSections.isEmpty()) {
                        item {
                            Text(
                                "No sections match your search or filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    items(filteredSections) { section ->
                        SelectableItem(
                            id = section.id,
                            isSelectionMode = isSelectionMode,
                            isSelected = section.id in selectedSections,
                            onLongPress = { isSelectionMode = true; selectedSections.add(section.id) },
                            onToggle = {
                                if (section.id in selectedSections) selectedSections.remove(section.id)
                                else selectedSections.add(section.id)
                            }
                        ) {
                            SectionCard(
                                section = section,
                                onDelete = if (!isSelectionMode) ({ viewModel.deleteSection(section.id) }) else null
                            )
                        }
                    }
                }
            }
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { showManualDialog = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) { Icon(Icons.Default.Add, contentDescription = "Add section") }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${selectedSections.size} Sections?") },
            text = { Text("This will permanently delete the selected sections.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteSections(selectedSections.toList())
                    selectedSections.clear(); isSelectionMode = false; showDeleteConfirm = false
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showConfigDialog) {
        MaxStudentsConfigDialog(
            current = uiState.maxStudentsPerSection,
            onDismiss = { showConfigDialog = false },
            onConfirm = { max -> viewModel.setMaxStudentsPerSection(max); showConfigDialog = false }
        )
    }

    if (showAutoDialog) {
        AutoGenerateSectionsDialog(
            totalStudents = uiState.students.size,
            maxPerSection = uiState.maxStudentsPerSection,
            onDismiss = { showAutoDialog = false },
            onConfirm = { count, capacity -> viewModel.autoGenerateSections(count, capacity); showAutoDialog = false }
        )
    }

    if (showManualDialog) {
        AddSectionDialog(
            onDismiss = { showManualDialog = false },
            onConfirm = { name, capacity -> viewModel.addSection(name, capacity); showManualDialog = false }
        )
    }
}

@Composable
fun MaxStudentsConfigDialog(current: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var value by remember { mutableStateOf(current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Max Students per Section") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Used to auto-calculate how many sections are needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = value, onValueChange = { value = it },
                    label = { Text("Max students per section") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("e.g. 30 → 250 students = 9 sections") })
            }
        },
        confirmButton = { Button(onClick = { onConfirm(value.toIntOrNull() ?: 30) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AutoGenerateSectionsDialog(totalStudents: Int, maxPerSection: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val sectionsNeeded = if (maxPerSection > 0) ceil(totalStudents.toDouble() / maxPerSection).toInt() else 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Auto-Generate Sections") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Registered students: $totalStudents")
                        Text("Max per section: $maxPerSection")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Sections to create: $sectionsNeeded",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary)
                        if (sectionsNeeded in 1..26)
                            Text("Section A – Section ${'A' + sectionsNeeded - 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (maxPerSection <= 0)
                    Text("Set max students per section first.", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = { if (sectionsNeeded > 0) onConfirm(sectionsNeeded, maxPerSection) },
                enabled = sectionsNeeded > 0) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Generate $sectionsNeeded Sections")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SectionCard(section: Section, onDelete: (() -> Unit)?) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val fillPercent = if (section.maxCapacity > 0) section.studentCount.toFloat() / section.maxCapacity else 0f

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(section.name, style = MaterialTheme.typography.titleMedium)
                    Text("${section.studentCount} / ${section.maxCapacity} students",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (onDelete != null) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fillPercent.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = if (fillPercent >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Section") },
            text = { Text("Delete '${section.name}'? This cannot be undone.") },
            confirmButton = {
                Button(onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AddSectionDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("30") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Section") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Section Name (e.g. Section A)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = capacity, onValueChange = { capacity = it },
                    label = { Text("Max Capacity") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, capacity.toIntOrNull() ?: 30) }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
