package com.fams.app.ui.screens.teacher

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
import com.fams.app.domain.model.AppNotification
import com.fams.app.domain.model.ScheduleEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TeacherHomeScreen(
    state: TeacherUiState,
    viewModel: TeacherViewModel,
    onSignOut: () -> Unit
) {
    val pendingGrading = state.submissions.count { it.grade == null }
    val unreadNotifs = state.notifications.count { !it.isRead }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Welcome, ${state.currentUser?.fullName ?: "Teacher"}",
                style = MaterialTheme.typography.headlineSmall)
            Text(SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Stat cards
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TeacherStatCard(Modifier.weight(1f), Icons.Default.Today,
                    "Today's Classes", state.todayClasses.size.toString(),
                    MaterialTheme.colorScheme.primaryContainer)
                TeacherStatCard(Modifier.weight(1f), Icons.Default.Assignment,
                    "Pending Grading", pendingGrading.toString(),
                    MaterialTheme.colorScheme.secondaryContainer)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TeacherStatCard(Modifier.weight(1f), Icons.Default.Quiz,
                    "Active Quizzes", state.quizzes.count { it.isAvailable }.toString(),
                    MaterialTheme.colorScheme.tertiaryContainer)
                TeacherStatCard(Modifier.weight(1f), Icons.Default.Notifications,
                    "Notifications", unreadNotifs.toString(),
                    MaterialTheme.colorScheme.surfaceVariant)
            }
        }

        // Today's schedule
        item {
            Text("Today's Classes", style = MaterialTheme.typography.titleMedium)
        }
        if (state.todayClasses.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text("No classes today", modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(state.todayClasses) { entry ->
                TodayClassCard(entry)
            }
        }

        // Quick actions
        item {
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionButton(Modifier.weight(1f), Icons.Default.HowToReg, "Attendance") {}
                QuickActionButton(Modifier.weight(1f), Icons.Default.Upload, "Material") {}
                QuickActionButton(Modifier.weight(1f), Icons.Default.Assignment, "Assignment") {}
                QuickActionButton(Modifier.weight(1f), Icons.Default.Quiz, "Quiz") {}
            }
        }

        // Recent notifications
        item { Text("Recent Notifications", style = MaterialTheme.typography.titleMedium) }
        items(state.notifications.take(5)) { notif ->
            NotificationCard(notif, onMarkRead = { viewModel.markNotificationRead(notif.id) })
        }
    }
}

@Composable
fun TeacherStatCard(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector,
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

@Composable
fun TodayClassCard(entry: ScheduleEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier
                .width(60.dp)
                .padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(entry.startTime, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text(entry.endTime, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.subjectName, style = MaterialTheme.typography.titleMedium)
                Text("${entry.sectionName} • ${entry.roomName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun QuickActionButton(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun NotificationCard(notif: AppNotification, onMarkRead: () -> Unit) {
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
            }
            if (!notif.isRead) {
                TextButton(onClick = onMarkRead) { Text("Mark Read") }
            }
        }
    }
}
