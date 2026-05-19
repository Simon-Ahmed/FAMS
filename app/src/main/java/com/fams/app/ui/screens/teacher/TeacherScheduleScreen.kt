package com.fams.app.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fams.app.domain.model.ScheduleEntry

@Composable
fun TeacherScheduleScreen(state: TeacherUiState, viewModel: TeacherViewModel) {
    val days = listOf("Monday","Tuesday","Wednesday","Thursday","Friday")
    var selectedDay by remember { mutableStateOf("Monday") }
    val dayEntries = state.schedule.filter { it.day == selectedDay }.sortedBy { it.startTime }

    Column(modifier = Modifier.fillMaxSize()) {
        // Day selector
        Row(modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEach { day ->
                FilterChip(
                    selected = selectedDay == day,
                    onClick = { selectedDay = day },
                    label = { Text(day.take(3)) }
                )
            }
        }

        if (dayEntries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EventBusy, null, modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("No classes on $selectedDay",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dayEntries) { entry ->
                    ScheduleEntryDetailCard(entry)
                }
            }
        }
    }
}

@Composable
fun ScheduleEntryDetailCard(entry: ScheduleEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(entry.startTime, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                Text("–", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(entry.endTime, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.subjectName, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(entry.sectionName) },
                        leadingIcon = { Icon(Icons.Default.Group, null, Modifier.size(16.dp)) })
                    AssistChip(onClick = {}, label = { Text(entry.roomName) },
                        leadingIcon = { Icon(Icons.Default.Room, null, Modifier.size(16.dp)) })
                }
            }
        }
    }
}
