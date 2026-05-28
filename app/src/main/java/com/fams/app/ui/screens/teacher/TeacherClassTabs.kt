package com.fams.app.ui.screens.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fams.app.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

// ── Attendance ────────────────────────────────────────────────────────────────

@Composable
fun AttendanceTab(state: TeacherUiState, viewModel: TeacherViewModel, sectionId: String) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todaySubjects = state.schedule.filter { it.sectionId == sectionId }
    var selectedEntry by remember { mutableStateOf<ScheduleEntry?>(null) }
    val attendanceMap = remember { mutableStateMapOf<String, AttendanceStatus>() }

    if (selectedEntry == null) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Select class to take attendance", style = MaterialTheme.typography.titleMedium) }
            items(todaySubjects) { entry ->
                Card(modifier = Modifier.fillMaxWidth(), onClick = {
                    selectedEntry = entry
                    state.sectionStudents.forEach { s -> attendanceMap[s.uid] = AttendanceStatus.PRESENT }
                }) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(entry.subjectName, style = MaterialTheme.typography.titleMedium)
                            Text("${entry.day} ${entry.startTime}–${entry.endTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.HowToReg, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    } else {
        val entry = selectedEntry!!
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedEntry = null }) { Icon(Icons.Default.ArrowBack, null) }
                Text("${entry.subjectName} Attendance", style = MaterialTheme.typography.titleMedium)
            }
            LazyColumn(modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.sectionStudents) { student ->
                    val status = attendanceMap[student.uid] ?: AttendanceStatus.PRESENT
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(student.fullName, style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(AttendanceStatus.PRESENT to "P",
                                    AttendanceStatus.ABSENT to "A",
                                    AttendanceStatus.LATE to "L").forEach { (s, label) ->
                                    FilterChip(selected = status == s,
                                        onClick = { attendanceMap[student.uid] = s },
                                        label = { Text(label) })
                                }
                            }
                        }
                    }
                }
            }
            Button(onClick = {
                val records = state.sectionStudents.map { student ->
                    AttendanceRecord(studentId = student.uid, studentName = student.fullName,
                        teacherId = state.currentUser?.uid ?: "",
                        sectionId = sectionId, subjectId = entry.subjectId,
                        subjectName = entry.subjectName, date = today,
                        status = attendanceMap[student.uid] ?: AttendanceStatus.PRESENT,
                        sessionId = "${entry.subjectId}_$today")
                }
                viewModel.submitAttendance(records)
                selectedEntry = null
            }, modifier = Modifier.fillMaxWidth().padding(16.dp)) { Text("Submit Attendance") }
        }
    }
}

// ── Materials ─────────────────────────────────────────────────────────────────

@Composable
fun MaterialsTab(state: TeacherUiState, viewModel: TeacherViewModel, sectionId: String) {
    var showAddDialog by remember { mutableStateOf(false) }
    val mats = state.materials.filter { it.sectionId == sectionId }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Materials (${mats.size})", style = MaterialTheme.typography.titleMedium) }
            items(mats) { mat ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, null,
                                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                            Column {
                                Text(mat.title, style = MaterialTheme.typography.titleSmall)
                                Text(mat.subjectName, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { viewModel.deleteMaterial(mat.id, sectionId) }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, null) }
    }
    if (showAddDialog) {
        val subjects = state.schedule.filter { it.sectionId == sectionId }.map { it.subjectId to it.subjectName }.distinct()
        AddMaterialDialog(subjects = subjects, onDismiss = { showAddDialog = false },
            onConfirm = { title, subId, subName ->
                viewModel.addMaterial(Material(title = title, subjectId = subId, subjectName = subName,
                    sectionId = sectionId, uploadedBy = state.currentUser?.uid ?: "",
                    uploaderName = state.currentUser?.fullName ?: ""))
                showAddDialog = false
            })
    }
}

@Composable
fun AddMaterialDialog(subjects: List<Pair<String,String>>, onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var sel by remember { mutableStateOf(subjects.firstOrNull()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Material") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                subjects.forEach { (id, name) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = sel?.first == id, onClick = { sel = id to name })
                        Text(name)
                    }
                }
                Text("File upload requires Firebase Storage (Blaze plan).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = { val s = sel ?: return@Button; if (title.isNotBlank()) onConfirm(title, s.first, s.second) }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Assignments ───────────────────────────────────────────────────────────────

@Composable
fun AssignmentsTab(state: TeacherUiState, viewModel: TeacherViewModel, sectionId: String) {
    var showCreate by remember { mutableStateOf(false) }
    var selectedAssignment by remember { mutableStateOf<Assignment?>(null) }
    val assignments = state.assignments.filter { it.sectionId == sectionId }

    if (selectedAssignment == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Assignments (${assignments.size})", style = MaterialTheme.typography.titleMedium) }
                items(assignments) { a ->
                    Card(modifier = Modifier.fillMaxWidth(), onClick = {
                        selectedAssignment = a; viewModel.loadSubmissions(a.id)
                    }) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(a.title, style = MaterialTheme.typography.titleMedium)
                            Text(a.subjectName, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                            Text("Due: ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(a.deadline))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            FloatingActionButton(onClick = { showCreate = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, null) }
        }
    } else {
        val a = selectedAssignment!!
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedAssignment = null }) { Icon(Icons.Default.ArrowBack, null) }
                Text(a.title, style = MaterialTheme.typography.titleMedium)
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Submissions (${state.submissions.size})", style = MaterialTheme.typography.titleMedium) }
                items(state.submissions) { sub ->
                    SubmissionCard(sub, onGrade = { g, f -> viewModel.gradeSubmission(sub.id, g, f) })
                }
            }
        }
    }

    if (showCreate) {
        val subjects = state.schedule.filter { it.sectionId == sectionId }.map { it.subjectId to it.subjectName }.distinct()
        CreateAssignmentDialog(subjects = subjects, onDismiss = { showCreate = false },
            onConfirm = { title, desc, subId, subName, deadline ->
                viewModel.createAssignment(Assignment(title = title, description = desc,
                    subjectId = subId, subjectName = subName, sectionId = sectionId,
                    teacherId = state.currentUser?.uid ?: "", deadline = deadline))
                showCreate = false
            })
    }
}

@Composable
fun SubmissionCard(sub: Submission, onGrade: (Double, String) -> Unit) {
    var showGrade by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(sub.studentName, style = MaterialTheme.typography.titleSmall)
                if (sub.grade != null) Badge { Text("${sub.grade.toInt()}") }
                else TextButton(onClick = { showGrade = true }) { Text("Grade") }
            }
            if (sub.textResponse.isNotBlank())
                Text(sub.textResponse, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (sub.feedback.isNotBlank())
                Text("Feedback: ${sub.feedback}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
        }
    }
    if (showGrade) GradeDialog(onDismiss = { showGrade = false },
        onConfirm = { g, f -> onGrade(g, f); showGrade = false })
}

@Composable
fun GradeDialog(onDismiss: () -> Unit, onConfirm: (Double, String) -> Unit) {
    var grade by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Grade Submission") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = grade, onValueChange = { grade = it },
                    label = { Text("Grade (0-100)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = feedback, onValueChange = { feedback = it },
                    label = { Text("Feedback") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
            }
        },
        confirmButton = {
            Button(onClick = { grade.toDoubleOrNull()?.let { onConfirm(it, feedback) } }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CreateAssignmentDialog(subjects: List<Pair<String,String>>, onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Long) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var sel by remember { mutableStateOf(subjects.firstOrNull()) }
    var days by remember { mutableStateOf("7") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Create Assignment") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it },
                    label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                OutlinedTextField(value = days, onValueChange = { days = it },
                    label = { Text("Days until deadline") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                subjects.forEach { (id, name) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = sel?.first == id, onClick = { sel = id to name })
                        Text(name)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val s = sel ?: return@Button
                val deadline = System.currentTimeMillis() + (days.toLongOrNull() ?: 7L) * 86400000L
                if (title.isNotBlank()) onConfirm(title, desc, s.first, s.second, deadline)
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Quizzes ───────────────────────────────────────────────────────────────────

@Composable
fun QuizzesTab(state: TeacherUiState, viewModel: TeacherViewModel, sectionId: String) {
    var showCreate by remember { mutableStateOf(false) }
    var selectedQuiz by remember { mutableStateOf<Quiz?>(null) }
    val quizzes = state.quizzes.filter { it.sectionId == sectionId }

    if (showCreate) {
        val subjects = state.schedule
            .filter { it.sectionId == sectionId }
            .map { it.subjectId to it.subjectName }
            .distinct()
        CreateQuizScreen(
            subjects = subjects,
            sectionId = sectionId,
            teacherId = state.currentUser?.uid ?: "",
            onDismiss = { showCreate = false },
            onConfirm = { quiz ->
                viewModel.createQuiz(quiz)
                showCreate = false
            }
        )
        return
    }

    if (selectedQuiz == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Quizzes (${quizzes.size})", style = MaterialTheme.typography.titleMedium) }
                if (quizzes.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Quiz, null, modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Text("No quizzes yet. Tap + to create one.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                items(quizzes) { quiz ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(quiz.title, style = MaterialTheme.typography.titleMedium)
                                    Text("${quiz.questions.size} questions • ${quiz.subjectName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        if (quiz.timerMode == TimerMode.TOTAL)
                                            "Total: ${quiz.totalTimeSeconds / 60} min"
                                        else "${quiz.perQuestionSeconds}s per question",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(if (quiz.isAvailable) "Live" else "Hidden",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (quiz.isAvailable) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Switch(checked = quiz.isAvailable,
                                            onCheckedChange = { viewModel.setQuizAvailability(quiz.id, it) })
                                    }
                                    TextButton(onClick = {
                                        selectedQuiz = quiz
                                        viewModel.loadQuizSessions(quiz.id)
                                    }) { Text("Results") }
                                }
                            }
                        }
                    }
                }
            }
            FloatingActionButton(onClick = { showCreate = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                Icon(Icons.Default.Add, null)
            }
        }
    } else {
        // Results / leaderboard view
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedQuiz = null }) { Icon(Icons.Default.ArrowBack, null) }
                Column {
                    Text("${selectedQuiz!!.title} — Results", style = MaterialTheme.typography.titleMedium)
                    Text("${state.quizSessions.size} submissions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.quizSessions.isEmpty()) {
                    item { Text("No submissions yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                itemsIndexed(state.quizSessions.sortedByDescending { it.score }) { index, session ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("#${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = when (index) {
                                        0 -> androidx.compose.ui.graphics.Color(0xFFFFD700)
                                        1 -> androidx.compose.ui.graphics.Color(0xFFC0C0C0)
                                        2 -> androidx.compose.ui.graphics.Color(0xFFCD7F32)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    })
                                Text(session.studentName, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("${session.score.toInt()} / ${session.maxScore.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

// ── Grades ────────────────────────────────────────────────────────────────────

@Composable
fun GradesTab(state: TeacherUiState, viewModel: TeacherViewModel, sectionId: String) {
    var showGrade by remember { mutableStateOf(false) }
    var selStudent by remember { mutableStateOf<User?>(null) }
    var selSubject by remember { mutableStateOf<Pair<String,String>?>(null) }
    val subjects = state.schedule.filter { it.sectionId == sectionId }.map { it.subjectId to it.subjectName }.distinct()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Grades", style = MaterialTheme.typography.titleMedium) }
        items(state.sectionStudents) { student ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(student.fullName, style = MaterialTheme.typography.titleSmall)
                    subjects.forEach { (subId, subName) ->
                        val grade = state.grades.find { it.studentId == student.uid && it.subjectId == subId }
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(subName, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (grade != null)
                                    Text("${grade.value.toInt()} (${grade.letterGrade})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary)
                                TextButton(onClick = { selStudent = student; selSubject = subId to subName; showGrade = true }) {
                                    Text(if (grade != null) "Edit" else "Add")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGrade && selStudent != null && selSubject != null) {
        GradeDialog(onDismiss = { showGrade = false },
            onConfirm = { g, _ ->
                viewModel.saveGrade(Grade(studentId = selStudent!!.uid, studentName = selStudent!!.fullName,
                    subjectId = selSubject!!.first, subjectName = selSubject!!.second,
                    teacherId = state.currentUser?.uid ?: "", value = g), sectionId)
                showGrade = false
            })
    }
}
