package com.fams.app.ui.screens.classrep

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
fun ClassRepHomeScreen(state: ClassRepUiState, viewModel: ClassRepViewModel) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayTeacherAtt = state.teacherAttendance.filter { it.date == today }
    val pendingClasses = state.todayClasses.filter { entry ->
        todayTeacherAtt.none { it.subjectId == entry.subjectId }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Class Rep — ${state.currentUser?.fullName ?: ""}",
                style = MaterialTheme.typography.headlineSmall)
            Text(SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RepStatCard(Modifier.weight(1f), Icons.Default.Today, "Today's Classes",
                    state.todayClasses.size.toString(), MaterialTheme.colorScheme.primaryContainer)
                RepStatCard(Modifier.weight(1f), Icons.Default.HowToReg, "Marked",
                    todayTeacherAtt.size.toString(), MaterialTheme.colorScheme.secondaryContainer)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RepStatCard(Modifier.weight(1f), Icons.Default.Warning, "Pending",
                    pendingClasses.size.toString(),
                    if (pendingClasses.isNotEmpty()) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.tertiaryContainer)
                RepStatCard(Modifier.weight(1f), Icons.Default.Campaign, "Announcements",
                    state.announcements.size.toString(), MaterialTheme.colorScheme.surfaceVariant)
            }
        }

        item { Text("Today's Classes — Teacher Status", style = MaterialTheme.typography.titleMedium) }
        items(state.todayClasses) { entry ->
            val att = todayTeacherAtt.find { it.subjectId == entry.subjectId }
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.subjectName, style = MaterialTheme.typography.titleSmall)
                        Text("${entry.teacherName} • ${entry.startTime}–${entry.endTime}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (att != null) {
                        val (color, label) = when (att.status) {
                            AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.primary to "Present"
                            AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error to "Absent"
                            AttendanceStatus.LATE -> androidx.compose.ui.graphics.Color(0xFFFFA000) to "Late"
                        }
                        Badge(containerColor = color) { Text(label) }
                    } else {
                        AssistChip(onClick = {}, label = { Text("Not marked") })
                    }
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
fun RepStatCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector,
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
fun ClassRepScheduleScreen(state: ClassRepUiState) {
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
                                Text(entry.roomName, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Attendance Screen ─────────────────────────────────────────────────────────

@Composable
fun ClassRepAttendanceScreen(state: ClassRepUiState, viewModel: ClassRepViewModel) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayAtt = state.teacherAttendance.filter { it.date == today }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Teacher Attendance", style = MaterialTheme.typography.titleMedium)
            Text("Mark attendance for teachers in your section today.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        items(state.todayClasses) { entry ->
            val existing = todayAtt.find { it.subjectId == entry.subjectId }
            var selectedStatus by remember { mutableStateOf(existing?.status) }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(entry.subjectName, style = MaterialTheme.typography.titleMedium)
                    Text("${entry.teacherName} • ${entry.startTime}–${entry.endTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))

                    if (existing != null) {
                        val (color, label) = when (existing.status) {
                            AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.primary to "Present"
                            AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error to "Absent"
                            AttendanceStatus.LATE -> androidx.compose.ui.graphics.Color(0xFFFFA000) to "Late"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = color, modifier = Modifier.size(20.dp))
                            Text("Marked as $label", color = color, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(AttendanceStatus.PRESENT to "Present",
                                AttendanceStatus.ABSENT to "Absent",
                                AttendanceStatus.LATE to "Late").forEach { (status, label) ->
                                FilterChip(selected = selectedStatus == status,
                                    onClick = { selectedStatus = status },
                                    label = { Text(label) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            selectedStatus?.let { s ->
                                viewModel.markTeacherAttendance(entry.teacherId, entry.teacherName,
                                    entry.subjectId, entry.subjectName, s)
                            }
                        }, enabled = selectedStatus != null, modifier = Modifier.fillMaxWidth()) {
                            Text("Submit")
                        }
                    }
                }
            }
        }

        if (state.todayClasses.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No classes scheduled today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Chat Screen ───────────────────────────────────────────────────────────────

@Composable
fun ClassRepChatScreen(state: ClassRepUiState, viewModel: ClassRepViewModel) {
    var selectedThread by remember { mutableStateOf<ChatThread?>(null) }
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    if (selectedThread == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Messages", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp))
            if (state.chatThreads.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No conversations yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.chatThreads) { thread ->
                        val me = state.currentUser?.uid ?: ""
                        val otherName = thread.participantNames.entries
                            .firstOrNull { it.key != me }?.value ?: "Unknown"
                        Card(modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                selectedThread = thread
                                val otherId = thread.participantIds.firstOrNull { it != me } ?: ""
                                viewModel.loadMessages(otherId)
                            }) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountCircle, null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text(otherName, style = MaterialTheme.typography.titleSmall)
                                        Text(thread.lastMessage, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                                if (thread.unreadCount > 0) Badge { Text(thread.unreadCount.toString()) }
                            }
                        }
                    }
                }
            }
        }
    } else {
        val thread = selectedThread!!
        val me = state.currentUser?.uid ?: ""
        val otherId = thread.participantIds.firstOrNull { it != me } ?: ""
        val otherName = thread.participantNames[otherId] ?: "Unknown"

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedThread = null }) { Icon(Icons.Default.ArrowBack, null) }
                Text(otherName, style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider()
            LazyColumn(state = listState, modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.messages) { msg ->
                    val isMe = msg.senderId == me
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start) {
                        Card(colors = CardDefaults.cardColors(
                            containerColor = if (isMe) MaterialTheme.colorScheme.primary
                                             else MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(12.dp).widthIn(max = 260.dp)) {
                                Text(msg.message,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
            Row(modifier = Modifier.padding(8.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = messageText, onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f), placeholder = { Text("Type a message...") }, maxLines = 3)
                IconButton(onClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(otherId, otherName, messageText)
                        messageText = ""
                    }
                }, enabled = messageText.isNotBlank()) {
                    Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// ── Profile Screen ────────────────────────────────────────────────────────────

@Composable
fun ClassRepProfileScreen(state: ClassRepUiState, onSignOut: () -> Unit,
    onThemeToggle: () -> Unit, isDarkTheme: Boolean) {
    val user = state.currentUser
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AccountCircle, null,
                    modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(user?.fullName ?: "Class Rep", style = MaterialTheme.typography.headlineSmall)
                Badge { Text("Class Representative") }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Profile Information", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    RepProfileRow(Icons.Default.Email, "Email", user?.email ?: "")
                    RepProfileRow(Icons.Default.Phone, "Phone", user?.phone.takeIf { !it.isNullOrBlank() } ?: "Not set")
                    RepProfileRow(Icons.Default.Badge, "Student ID", user?.studentId.takeIf { !it.isNullOrBlank() } ?: "Not set")
                    RepProfileRow(Icons.Default.Group, "Section",
                        state.schedule.firstOrNull()?.sectionName ?: user?.sectionId ?: "Not assigned")
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
fun RepProfileRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
