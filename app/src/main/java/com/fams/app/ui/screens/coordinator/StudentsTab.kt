package com.fams.app.ui.screens.coordinator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fams.app.domain.model.User
import com.fams.app.domain.model.UserRole
import com.fams.app.ui.components.SelectableItem
import com.fams.app.ui.components.SelectionModeHeader
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentsTab(
    uiState: CoordinatorUiState,
    viewModel: CoordinatorViewModel,
    coordinatorEmail: String
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    var sectionFilterExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isSelectionMode) {
            SelectionModeHeader(
                totalCount = viewModel.filteredStudents.size,
                selectedCount = selected.size,
                onSelectAll = { selected.clear(); selected.addAll(viewModel.filteredStudents.map { it.uid }) },
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
                            Text("Students (${viewModel.filteredStudents.size}/${uiState.students.size})",
                                style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Search students") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotBlank()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
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
                                val sectionFilterLabel = if (uiState.selectedSectionId == "All") {
                                    "All sections"
                                } else {
                                    uiState.sections.find { it.id == uiState.selectedSectionId }?.name ?: "All sections"
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
                                            viewModel.updateSelectedSection("All")
                                            sectionFilterExpanded = false
                                        }
                                    )
                                    uiState.sections.forEach { section ->
                                        DropdownMenuItem(
                                            text = { Text(section.name) },
                                            onClick = {
                                                viewModel.updateSelectedSection(section.id)
                                                sectionFilterExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            BulkImportStudentsButton(uiState, viewModel, coordinatorEmail)
                            Spacer(Modifier.height(4.dp))
                            ExcelFormatHint(isStudent = true)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    if (viewModel.filteredStudents.isEmpty()) {
                        item {
                            Text(
                                "No students match the current search or section filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    items(viewModel.filteredStudents) { student ->
                        SelectableItem(
                            id = student.uid,
                            isSelectionMode = isSelectionMode,
                            isSelected = student.uid in selected,
                            onLongPress = { isSelectionMode = true; selected.add(student.uid) },
                            onToggle = {
                                if (student.uid in selected) selected.remove(student.uid)
                                else selected.add(student.uid)
                            }
                        ) {
                            StudentCard(
                                student = student,
                                sections = uiState.sections,
                                isSelectionMode = isSelectionMode,
                                onPromote = { viewModel.promoteToClassRep(student.uid) },
                                onDemote = { viewModel.demoteToStudent(student.uid) },
                                onMoveSection = { viewModel.moveStudentToSection(student.uid, it) }
                            )
                        }
                    }
                }
            }
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) { Icon(Icons.Default.PersonAdd, contentDescription = "Add student") }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${selected.size} Students?") },
            text = { Text("This will permanently delete the selected student accounts.") },
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
        AddStudentDialog(
            sections = uiState.sections,
            onDismiss = { showAddDialog = false },
            onConfirm = { fullName, email, phone, studentId, sectionId ->
                viewModel.addStudent(fullName, email, phone, studentId, sectionId)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCard(
    student: User,
    sections: List<com.fams.app.domain.model.Section>,
    isSelectionMode: Boolean = false,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onMoveSection: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showCredentials by remember { mutableStateOf(false) }
    val isClassRep = student.role == UserRole.CLASS_REP
    val sectionName = sections.find { it.id == student.sectionId }?.name ?: "No section"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(student.fullName, style = MaterialTheme.typography.titleMedium)
                        if (isClassRep) { Badge { Text("Rep") } }
                    }
                    Text(student.email, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(sectionName, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                if (!isSelectionMode) {
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

            if (expanded && !isSelectionMode) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                if (student.studentId.isNotBlank())
                    Text("ID: ${student.studentId}", style = MaterialTheme.typography.bodySmall)
                if (student.phone.isNotBlank())
                    Text("Phone: ${student.phone}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isClassRep) {
                        OutlinedButton(onClick = onPromote) {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Make Rep")
                        }
                    } else {
                        OutlinedButton(onClick = onDemote) {
                            Icon(Icons.Default.StarBorder, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Remove Rep")
                        }
                    }
                    OutlinedButton(onClick = { showMoveDialog = true }) {
                        Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Move")
                    }
                }
            }
        }
    }

    if (showCredentials) {
        CredentialsDialog(fullName = student.fullName, email = student.email,
            password = student.generatedPassword, onDismiss = { showCredentials = false })
    }
    if (showMoveDialog) {
        MoveSectionDialog(sections = sections, currentSectionId = student.sectionId,
            onDismiss = { showMoveDialog = false },
            onConfirm = { sectionId -> onMoveSection(sectionId); showMoveDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentDialog(
    sections: List<com.fams.app.domain.model.Section>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var selectedSectionId by remember { mutableStateOf("") }
    var sectionExpanded by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Student") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it },
                    label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it },
                    label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it },
                    label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = studentId, onValueChange = { studentId = it },
                    label = { Text("Student ID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                ExposedDropdownMenuBox(expanded = sectionExpanded, onExpandedChange = { sectionExpanded = it }) {
                    OutlinedTextField(
                        value = sections.find { it.id == selectedSectionId }?.name ?: "Select Section (optional)",
                        onValueChange = {}, readOnly = true, label = { Text("Section") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sectionExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = sectionExpanded, onDismissRequest = { sectionExpanded = false }) {
                        sections.forEach { section ->
                            DropdownMenuItem(
                                text = { Text("${section.name} (${section.studentCount}/${section.maxCapacity})") },
                                onClick = { selectedSectionId = section.id; sectionExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (fullName.isNotBlank() && email.isNotBlank()) onConfirm(fullName, email, phone, studentId, selectedSectionId) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun MoveSectionDialog(
    sections: List<com.fams.app.domain.model.Section>,
    currentSectionId: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedSectionId by remember { mutableStateOf(currentSectionId) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Move to Section") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sections.forEach { section ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = selectedSectionId == section.id,
                            onClick = { selectedSectionId = section.id })
                        Text("${section.name} (${section.studentCount}/${section.maxCapacity})")
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(selectedSectionId) }) { Text("Move") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CredentialsDialog(fullName: String, email: String, password: String, onDismiss: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Credentials — $fullName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            SelectionContainer {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Email", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(email, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(email))
                                Toast.makeText(context, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy email")
                            }
                        }
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            SelectionContainer {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Password", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(if (password.isBlank()) "Not available" else password,
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            IconButton(onClick = {
                                if (password.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(password))
                                    Toast.makeText(context, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Password not available", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy password")
                            }
                        }
                    }
                }
                Text("Share these credentials securely.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Close") } }
    )
}
