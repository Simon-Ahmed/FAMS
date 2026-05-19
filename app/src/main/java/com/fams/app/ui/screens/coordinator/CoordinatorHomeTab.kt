package com.fams.app.ui.screens.coordinator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun CoordinatorHomeTab(uiState: CoordinatorUiState) {
    val totalStudents = uiState.students.size
    val totalTeachers = uiState.teachers.size
    val totalSections = uiState.sections.size
    val classReps = uiState.students.count { it.role.value == "class_rep" }
    val scheduleStatus = uiState.scheduleStatus
    val pendingComplaints = uiState.pendingComplaints
    val attendanceRate = uiState.attendanceRate

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Overview", style = MaterialTheme.typography.titleLarge)

        // Main stats grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Modifier.weight(1f), Icons.Default.People, "Students", totalStudents.toString(),
                MaterialTheme.colorScheme.primaryContainer)
            StatCard(Modifier.weight(1f), Icons.Default.School, "Teachers", totalTeachers.toString(),
                MaterialTheme.colorScheme.secondaryContainer)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Modifier.weight(1f), Icons.Default.GridView, "Sections", totalSections.toString(),
                MaterialTheme.colorScheme.tertiaryContainer)
            StatCard(Modifier.weight(1f), Icons.Default.SupervisorAccount, "Class Reps", classReps.toString(),
                MaterialTheme.colorScheme.surfaceVariant)
        }

        HorizontalDivider()
        Text("Academic Status", style = MaterialTheme.typography.titleMedium)

        // Schedule status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (scheduleStatus == "Published")
                    MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = if (scheduleStatus == "Published")
                            Icons.Default.CalendarMonth else Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = if (scheduleStatus == "Published")
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer
                    )
                    Column {
                        Text("Schedule",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (scheduleStatus == "Published")
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer)
                        Text(scheduleStatus,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (scheduleStatus == "Published")
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                Icon(
                    imageVector = if (scheduleStatus == "Published")
                        Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (scheduleStatus == "Published")
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Attendance rate card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.BarChart, null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text("Student Attendance Rate",
                            style = MaterialTheme.typography.titleSmall)
                    }
                    Text("${attendanceRate.toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = when {
                            attendanceRate >= 80 -> MaterialTheme.colorScheme.primary
                            attendanceRate >= 60 -> Color(0xFFFFA000)
                            else -> MaterialTheme.colorScheme.error
                        })
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (attendanceRate / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        attendanceRate >= 80 -> MaterialTheme.colorScheme.primary
                        attendanceRate >= 60 -> Color(0xFFFFA000)
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }

        // Pending complaints card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (pendingComplaints > 0)
                    MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Report, null,
                        tint = if (pendingComplaints > 0)
                            MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                    Column {
                        Text("Pending Complaints",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (pendingComplaints > 0)
                                MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(pendingComplaints.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (pendingComplaints > 0)
                                MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (pendingComplaints > 0) {
                    Badge { Text(pendingComplaints.toString()) }
                }
            }
        }

        // Section fill overview
        if (uiState.sections.isNotEmpty()) {
            HorizontalDivider()
            Text("Section Capacity", style = MaterialTheme.typography.titleMedium)
            uiState.sections.forEach { section ->
                val fill = if (section.maxCapacity > 0)
                    section.studentCount.toFloat() / section.maxCapacity else 0f
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(section.name, style = MaterialTheme.typography.bodyMedium)
                        Text("${section.studentCount}/${section.maxCapacity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { fill.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (fill >= 1f) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer
) {
    Card(modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium)
            Text(text = label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
