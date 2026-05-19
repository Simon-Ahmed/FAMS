package com.fams.app.ui.screens.student

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

// ── Home Screen ───────────────────────────────────────────────────────────────

@Composable
fun StudentHomeScreen(state: StudentUiState, viewModel: StudentViewModel) {
    val gpa = if (state.grades.isEmpty()) 0.0
              else state.grades.map { it.value }.average()
    val attendanceRate = if (state.attendanceRecords.isEmpty()) 0f
        else state.attendanceRecords.count { it.status == AttendanceStatus.PRESENT }.toFloat() /
             state.attendanceRecords.size * 100f
    val upcoming = state.assignments.filter { it.deadline > System.currentTimeMillis() }
        .sortedBy { it.deadline }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Hello, ${state.currentUser?.fullName?.split(" ")?.firstOrNull() ?: "Student"}!",
                style = MaterialTheme.typography.headlineSmall)
            Text(SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), Icons.Default.Today, "Today's Classes",
                    state.todayClasses.size.toString(), MaterialTheme.colorScheme.primaryContainer)
                StatCard(Modifier.weight(1f), Icons.Default.BarChart, "Attendance",
                    "${attendanceRate.toInt()}%", MaterialTheme.colorScheme.secondaryContainer)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), Icons.Default.Assignment, "Pending",
                    upcoming.size.toString(), MaterialTheme.colorScheme.tertiaryContainer)
                StatCard(Modifier.weight(1f), Icons.Default.Grade, "GPA",
                    String.format("%.1f", gpa / 10), MaterialTheme.colorScheme.surfaceVariant)
            }
        }

        item { Text("Today's Schedule", style = MaterialTheme.typography.titleMedium) }
        if (state.todayClasses.isEmpty()) {
            item { Card(modifier = Modifier.fillMaxWidth()) {
                Text("No classes today", modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        } else {
            items(state.todayClasses) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.width(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(entry.startTime, style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary)
                            Text(entry.endTime, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.subjectName, style = MaterialTheme.typography.titleMedium)
                            Text("${entry.teacherName} • ${entry.roomName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item { Text("Upcoming Deadlines", style = MaterialTheme.typography.titleMedium) }
        items(upcoming.take(3)) { a ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(a.title, style = MaterialTheme.typography.titleSmall)
                        Text(a.subjectName, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Text(SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(a.deadline)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        }

        item { Text("Announcements", style = MaterialTheme.typography.titleMedium) }
        items(state.announcements.take(3)) { ann ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(ann.title, style = MaterialTheme.typography.titleSmall)
                    Text(ann.body, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    Text("— ${ann.authorName}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Schedule Screen ───────────────────────────────────────────────────────────

@Composable
fun StudentScheduleScreen(state: StudentUiState) {
    val days = listOf("Monday","Tuesday","Wednesday","Thursday","Friday")
    var selectedDay by remember { mutableStateOf("Monday") }
    val dayEntries = state.schedule.filter { it.day == selectedDay }.sortedBy { it.startTime }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEach { day ->
                FilterChip(selected = selectedDay == day, onClick = { selectedDay = day },
                    label = { Text(day.take(3)) })
            }
        }
        if (dayEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No classes on $selectedDay", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dayEntries) { entry ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(entry.startTime, style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary)
                                Text(entry.endTime, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.subjectName, style = MaterialTheme.typography.titleMedium)
                                Text(entry.teacherName, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistChip(onClick = {}, label = { Text(entry.roomName) },
                                        leadingIcon = { Icon(Icons.Default.Room, null, Modifier.size(14.dp)) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Courses Screen ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCoursesScreen(state: StudentUiState, viewModel: StudentViewModel) {
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
                0 -> StudentMaterialsTab(state, subId)
                1 -> StudentAssignmentsTab(state, viewModel, subId)
                2 -> StudentQuizzesTab(state, viewModel, subId)
                3 -> StudentGradesTab(state, subId)
                4 -> StudentAttendanceTab(state, subId)
            }
        }
    }
}

@Composable
fun StudentMaterialsTab(state: StudentUiState, subjectId: String) {
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
fun StudentAssignmentsTab(state: StudentUiState, viewModel: StudentViewModel, subjectId: String) {
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
                            else -> Button(onClick = { showSubmitDialog = a },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) { Text("Submit") }
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
fun StudentQuizzesTab(state: StudentUiState, viewModel: StudentViewModel, subjectId: String) {
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
fun StudentGradesTab(state: StudentUiState, subjectId: String) {
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
fun StudentAttendanceTab(state: StudentUiState, subjectId: String) {
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

// ── Notifications Screen ──────────────────────────────────────────────────────

@Composable
fun StudentNotificationsScreen(state: StudentUiState, viewModel: StudentViewModel) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Notifications", style = MaterialTheme.typography.titleMedium)
                val unread = state.notifications.count { !it.isRead }
                if (unread > 0) Badge { Text(unread.toString()) }
            }
        }
        if (state.notifications.isEmpty()) {
            item { Text("No notifications.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(state.notifications) { notif ->
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (!notif.isRead) MaterialTheme.colorScheme.primaryContainer
                                     else MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(notif.title, style = MaterialTheme.typography.titleSmall)
                        Text(notif.body, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(SimpleDateFormat("MMM d HH:mm", Locale.getDefault()).format(Date(notif.createdAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!notif.isRead) {
                        TextButton(onClick = { viewModel.markNotificationRead(notif.id) }) { Text("Read") }
                    }
                }
            }
        }
    }
}

// ── Profile Screen ────────────────────────────────────────────────────────────

@Composable
fun StudentProfileScreen(state: StudentUiState, onSignOut: () -> Unit,
    onThemeToggle: () -> Unit, isDarkTheme: Boolean) {
    val user = state.currentUser
    val gpa = if (state.grades.isEmpty()) 0.0 else state.grades.map { it.value }.average()
    val attendanceRate = if (state.attendanceRecords.isEmpty()) 0f
        else state.attendanceRecords.count { it.status == AttendanceStatus.PRESENT }.toFloat() /
             state.attendanceRecords.size * 100f

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AccountCircle, null,
                    modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(user?.fullName ?: "Student", style = MaterialTheme.typography.headlineSmall)
                Text("Student", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Profile Information", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    ProfileInfoRow(Icons.Default.Email, "Email", user?.email ?: "")
                    ProfileInfoRow(Icons.Default.Phone, "Phone", user?.phone.takeIf { !it.isNullOrBlank() } ?: "Not set")
                    ProfileInfoRow(Icons.Default.Badge, "Student ID", user?.studentId.takeIf { !it.isNullOrBlank() } ?: "Not set")
                    ProfileInfoRow(Icons.Default.Group, "Section",
                        state.schedule.firstOrNull()?.sectionName ?: user?.sectionId ?: "Not assigned")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Academic Summary", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(String.format("%.1f", gpa / 10), style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary)
                            Text("GPA", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${attendanceRate.toInt()}%", style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary)
                            Text("Attendance", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.grades.size.toString(), style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary)
                            Text("Grades", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(headlineContent = { Text("Dark Mode") },
                    leadingContent = { Icon(if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode, null) },
                    trailingContent = { Switch(checked = isDarkTheme, onCheckedChange = { onThemeToggle() }) })
            }
        }
        item {
            Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
}

@Composable
fun ProfileInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
