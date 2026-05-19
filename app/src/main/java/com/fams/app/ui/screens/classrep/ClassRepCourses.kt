package com.fams.app.ui.screens.classrep

import androidx.compose.foundation.horizontalScroll
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
import com.fams.app.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassRepCoursesScreen(state: ClassRepUiState, viewModel: ClassRepViewModel) {
    val subjects = state.schedule.map { it.subjectId to it.subjectName }.distinct()
    var selectedSubject by remember { mutableStateOf<Pair<String,String>?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    if (selectedSubject == null) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("My Courses", style = MaterialTheme.typography.titleMedium) }
            items(subjects) { (id, name) ->
                Card(modifier = Modifier.fillMaxWidth(), onClick = { selectedSubject = id to name }) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(name, style = MaterialTheme.typography.titleMedium)
                            val teacher = state.schedule.find { it.subjectId == id }?.teacherName ?: ""
                            if (teacher.isNotBlank())
                                Text(teacher, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
    } else {
        val (subId, subName) = selectedSubject!!
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                IconButton(onClick = { selectedSubject = null }) { Icon(Icons.Default.ArrowBack, null) }
                Text(subName, style = MaterialTheme.typography.titleLarge)
            }
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                listOf("Materials","Assignments","Quizzes","Grades","Attendance").forEachIndexed { i, t ->
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(t) })
                }
            }
            when (selectedTab) {
                0 -> ClassRepMaterialsTab(state, subId)
                1 -> ClassRepAssignmentsTab(state, viewModel, subId)
                2 -> ClassRepQuizzesTab(state, viewModel, subId)
                3 -> ClassRepGradesTab(state, subId)
                4 -> ClassRepAttendanceTab(state, subId)
            }
        }
    }
}

@Composable
fun ClassRepMaterialsTab(state: ClassRepUiState, subjectId: String) {
    val mats = state.materials.filter { it.subjectId == subjectId }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Materials (${mats.size})", style = MaterialTheme.typography.titleMedium) }
        if (mats.isEmpty()) item { Text("No materials uploaded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(mats) { mat ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, null,
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mat.title, style = MaterialTheme.typography.titleSmall)
                        Text("By ${mat.uploaderName}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ClassRepAssignmentsTab(state: ClassRepUiState, viewModel: ClassRepViewModel, subjectId: String) {
    val assignments = state.assignments.filter { it.subjectId == subjectId }
    var showSubmitDialog by remember { mutableStateOf<Assignment?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Assignments (${assignments.size})", style = MaterialTheme.typography.titleMedium) }
        items(assignments) { a ->
            val submission = state.mySubmissions[a.id]
            val isPast = a.deadline < System.currentTimeMillis()
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(a.title, style = MaterialTheme.typography.titleMedium)
                        when {
                            submission?.grade != null -> Badge { Text("${submission.grade.toInt()}") }
                            submission != null -> AssistChip(onClick = {}, label = { Text("Submitted") })
                            isPast -> AssistChip(onClick = {}, label = { Text("Missed") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer))
                            else -> Button(onClick = { showSubmitDialog = a }, modifier = Modifier.fillMaxWidth()) { Text("Submit") }
                        }
                    }
                    if (a.description.isNotBlank())
                        Text(a.description, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Due: ${SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(a.deadline))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (submission?.feedback?.isNotBlank() == true)
                        Text("Feedback: ${submission.feedback}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    showSubmitDialog?.let { a ->
        var text by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showSubmitDialog = null },
            title = { Text("Submit: ${a.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = text, onValueChange = { text = it },
                        label = { Text("Your answer") }, modifier = Modifier.fillMaxWidth(), maxLines = 5)
                    Text("File upload requires Firebase Storage (Blaze plan).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.submitAssignment(a.id, text); showSubmitDialog = null }) { Text("Submit") }
            },
            dismissButton = { TextButton(onClick = { showSubmitDialog = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ClassRepQuizzesTab(state: ClassRepUiState, viewModel: ClassRepViewModel, subjectId: String) {
    val quizzes = state.quizzes.filter { it.subjectId == subjectId && it.isAvailable }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Available Quizzes", style = MaterialTheme.typography.titleMedium) }
        if (quizzes.isEmpty()) item { Text("No quizzes available.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(quizzes) { quiz ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(quiz.title, style = MaterialTheme.typography.titleMedium)
                    Text("${quiz.questions.size} questions • ${quiz.timerMode.value} timer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.startQuiz(quiz) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Start Quiz")
                    }
                }
            }
        }
    }
}

@Composable
fun ClassRepGradesTab(state: ClassRepUiState, subjectId: String) {
    val grades = state.grades.filter { it.subjectId == subjectId }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Grades", style = MaterialTheme.typography.titleMedium) }
        if (grades.isEmpty()) item { Text("No grades recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(grades) { grade ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(grade.subjectName, style = MaterialTheme.typography.titleSmall)
                        Text(SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(grade.recordedAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${grade.value.toInt()}", style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary)
                        Text(grade.letterGrade, style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun ClassRepAttendanceTab(state: ClassRepUiState, subjectId: String) {
    val records = state.attendanceRecords.filter { it.subjectId == subjectId }
    val present = records.count { it.status == AttendanceStatus.PRESENT }
    val rate = if (records.isEmpty()) 0f else present.toFloat() / records.size * 100f

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Attendance Rate", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${rate.toInt()}%", style = MaterialTheme.typography.headlineMedium,
                            color = when { rate >= 80 -> MaterialTheme.colorScheme.primary
                                rate >= 60 -> androidx.compose.ui.graphics.Color(0xFFFFA000)
                                else -> MaterialTheme.colorScheme.error })
                        Text("$present / ${records.size} classes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { (rate / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }
        items(records.sortedByDescending { it.date }) { record ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(record.date, style = MaterialTheme.typography.bodyMedium)
                    val (color, label) = when (record.status) {
                        AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.primary to "Present"
                        AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error to "Absent"
                        AttendanceStatus.LATE -> androidx.compose.ui.graphics.Color(0xFFFFA000) to "Late"
                    }
                    Text(label, style = MaterialTheme.typography.labelMedium, color = color)
                }
            }
        }
    }
}
