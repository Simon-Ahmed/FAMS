package com.fams.app.ui.screens.coordinator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fams.app.domain.model.Subject
import com.fams.app.domain.model.User
import com.fams.app.ui.components.SelectableItem
import com.fams.app.ui.components.SelectionModeHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeachersTab(
    uiState: CoordinatorUiState,
    viewModel: CoordinatorViewModel,
    coordinatorEmail: String
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var assignTeacher by remember { mutableStateOf<User?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterSectionId by remember { mutableStateOf("All") }
    var sectionFilterExpanded by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }

    val filteredTeachers by remember(uiState.teachers, uiState.sections, searchQuery, selectedFilterSectionId) {
        derivedStateOf {
            uiState.teachers.filter { teacher ->
                val matchesSearch = searchQuery.isBlank() ||
                    teacher.fullName.contains(searchQuery, ignoreCase = true) ||
                    teacher.email.contains(searchQuery, ignoreCase = true) ||
                    teacher.teacherId.contains(searchQuery, ignoreCase = true)
                val matchesSection = when {
                    selectedFilterSectionId == "All" -> true
                    selectedFilterSectionId == "No section" -> teacher.sectionId.isBlank()
                    else -> teacher.sectionId == selectedFilterSectionId
                }
                matchesSearch && matchesSection
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isSelectionMode) {
            SelectionModeHeader(
                totalCount = filteredTeachers.size,
                selectedCount = selected.size,
                onSelectAll = { selected.clear(); selected.addAll(filteredTeachers.map { it.uid }) },
                onDeselectAll = { selected.clear() },
                onDeleteSelected = { showDeleteConfirm = true },
                onExitSelection = { isSelectionMode = false; selected.clear() }
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
                            Text("Teachers (${filteredTeachers.size}/${uiState.teachers.size})",
                                style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Search teachers") },
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
                                val sectionFilterLabel = when (selectedFilterSectionId) {
                                    "All" -> "All sections"
                                    "No section" -> "No section"
                                    else -> uiState.sections.find { it.id == selectedFilterSectionId }?.name ?: "All sections"
                                }
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
                                    DropdownMenuItem(
                                        text = { Text("No section") },
                                        onClick = {
                                            selectedFilterSectionId = "No section"
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
                            BulkImportTeachersButton(uiState, viewModel, coordinatorEmail)
                            Spacer(Modifier.height(4.dp))
                            ExcelFormatHint(isStudent = false)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    if (filteredTeachers.isEmpty()) {
                        item {
                            Text(
                                "No teachers match the current search or section filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    items(filteredTeachers) { teacher ->
                        val assignedSubjects = uiState.teacherSubjects
                            .filter { it.teacherId == teacher.uid }
                            .mapNotNull { ts -> uiState.subjects.find { it.id == ts.subjectId } }

                        SelectableItem(
                            id = teacher.uid,
                            isSelectionMode = isSelectionMode,
                            isSelected = teacher.uid in selected,
                            onLongPress = { isSelectionMode = true; selected.add(teacher.uid) },
                            onToggle = {
                                if (teacher.uid in selected) selected.remove(teacher.uid)
                                else selected.add(teacher.uid)
                            }
                        ) {
                            TeacherCard(
                                teacher = teacher,
                                isSelectionMode = isSelectionMode,
                                assignedSubjects = assignedSubjects,
                                onAssign = {
                                    assignTeacher = teacher
                                    showAssignDialog = true
                                }
                            )
                        }
                    }
                }
            }
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) { Icon(Icons.Default.PersonAdd, contentDescription = "Add teacher") }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${selected.size} Teachers?") },
            text = { Text("This will permanently delete the selected teacher accounts.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteUsers(selected.toList())
                    selected.clear(); isSelectionMode = false; showDeleteConfirm = false
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showAddDialog) {
        AddTeacherDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { fullName, email, phone, teacherId, specialization ->
                viewModel.addTeacher(fullName, email, phone, teacherId, specialization)
                showAddDialog = false
            }
        )
    }

    if (showAssignDialog && assignTeacher != null) {
        AssignTeacherToSubjectDialog(
            teacher = assignTeacher!!,
            subjects = uiState.subjects,
            assignedSubjectIds = uiState.teacherSubjects
                .filter { it.teacherId == assignTeacher!!.uid }
                .map { it.subjectId }.toSet(),
            onDismiss = {
                assignTeacher = null
                showAssignDialog = false
            },
            onConfirm = { teacherId, subjectId ->
                viewModel.assignTeacherToSubject(teacherId, subjectId)
                assignTeacher = null
                showAssignDialog = false
            }
        )
    }
}

@Composable
fun TeacherCard(
    teacher: User,
    isSelectionMode: Boolean = false,
    assignedSubjects: List<Subject> = emptyList(),
    onAssign: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var showCredentials by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(teacher.fullName, style = MaterialTheme.typography.titleMedium)
                    Text(teacher.email, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (teacher.specialization.isNotBlank())
                        Text(teacher.specialization, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                }
                if (!isSelectionMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (onAssign != null) {
                            IconButton(onClick = onAssign) {
                                Icon(Icons.Default.Assignment, contentDescription = "Assign subject",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        IconButton(onClick = { showCredentials = true }) {
                            Icon(Icons.Default.Key, contentDescription = "View credentials",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand")
                        }
                    }
                }
            }
            if (expanded && !isSelectionMode) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                if (teacher.teacherId.isNotBlank())
                    Text("ID: ${teacher.teacherId}", style = MaterialTheme.typography.bodySmall)
                if (teacher.phone.isNotBlank())
                    Text("Phone: ${teacher.phone}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text("Assigned subjects:", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (assignedSubjects.isEmpty()) {
                    Text("None", style = MaterialTheme.typography.bodySmall)
                } else {
                    assignedSubjects.forEach { subject ->
                        Text("• ${subject.name}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showCredentials) {
        CredentialsDialog(fullName = teacher.fullName, email = teacher.email,
            password = teacher.generatedPassword, onDismiss = { showCredentials = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignTeacherToSubjectDialog(
    teacher: User,
    subjects: List<Subject>,
    assignedSubjectIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign ${teacher.fullName} to subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Teacher: ${teacher.fullName}", style = MaterialTheme.typography.bodyMedium)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = subjects.find { it.id == selectedSubjectId }?.name ?: "Select Subject",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        subjects.forEach { subject ->
                            val alreadyAssigned = subject.id in assignedSubjectIds
                            DropdownMenuItem(
                                text = {
                                    Text(if (alreadyAssigned) "${subject.name} (already assigned)" else subject.name)
                                },
                                onClick = {
                                    if (!alreadyAssigned) {
                                        selectedSubjectId = subject.id
                                    }
                                    expanded = false
                                },
                                enabled = !alreadyAssigned
                            )
                        }
                    }
                }
                if (subjects.isEmpty()) {
                    Text("No subjects available. Add subjects from the schedule module first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (assignedSubjectIds.isNotEmpty()) {
                    Text("Already assigned to: ${subjects.filter { it.id in assignedSubjectIds }.joinToString { it.name }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(teacher.uid, selectedSubjectId) }, enabled = selectedSubjectId.isNotBlank()) {
                Text("Assign")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddTeacherDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String, String) -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var teacherId by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Teacher") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it },
                    label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it },
                    label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it },
                    label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = teacherId, onValueChange = { teacherId = it },
                    label = { Text("Teacher ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = specialization, onValueChange = { specialization = it },
                    label = { Text("Specialization") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { if (fullName.isNotBlank() && email.isNotBlank()) onConfirm(fullName, email, phone, teacherId, specialization) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
