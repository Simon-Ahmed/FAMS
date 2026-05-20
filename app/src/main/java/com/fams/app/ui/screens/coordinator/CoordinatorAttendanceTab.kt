package com.fams.app.ui.screens.coordinator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fams.app.domain.model.AttendanceRecord
import com.fams.app.domain.model.AttendanceStatus
import com.fams.app.domain.model.TeacherAttendanceRecord

@Composable
fun CoordinatorAttendanceTab(uiState: CoordinatorUiState, viewModel: CoordinatorViewModel) {
    val sectionMap = uiState.sections.associateBy { it.id }
    val selectedSectionName = when (uiState.attendanceSectionId) {
        "All" -> "All sections"
        else -> sectionMap[uiState.attendanceSectionId]?.name ?: "All sections"
    }

    var sectionMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Attendance Monitoring", style = MaterialTheme.typography.titleLarge)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Filters", style = MaterialTheme.typography.titleMedium)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { sectionMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedSectionName)
                        }
                        DropdownMenu(
                            expanded = sectionMenuExpanded,
                            onDismissRequest = { sectionMenuExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DropdownMenuItem(
                                text = { Text("All sections") },
                                onClick = {
                                    sectionMenuExpanded = false
                                    viewModel.updateAttendanceSection("All")
                                }
                            )
                            uiState.sections.forEach { section ->
                                DropdownMenuItem(
                                    text = { Text(section.name) },
                                    onClick = {
                                        sectionMenuExpanded = false
                                        viewModel.updateAttendanceSection(section.id)
                                    }
                                )
                            }
                        }
                    }
 OutlinedTextField(
                        value = uiState.attendanceDate,
                        onValueChange = viewModel::updateAttendanceDate,
                        label = { Text("Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = viewModel::loadAttendanceSummary,
                    enabled = !uiState.attendanceLoading,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (uiState.attendanceLoading) "Refreshing..." else "Refresh")
                }
            }
        }

        if (uiState.attendanceError != null) {
            Text(
                text = uiState.attendanceError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        val attendanceCount = uiState.attendanceRecords.size
        val presentCount = uiState.attendanceRecords.count { it.status == AttendanceStatus.PRESENT }
        val absentCount = uiState.attendanceRecords.count { it.status == AttendanceStatus.ABSENT }
        val lateCount = uiState.attendanceRecords.count { it.status == AttendanceStatus.LATE }
        val attendancePercentage = if (attendanceCount > 0) (presentCount.toFloat() / attendanceCount) * 100f else 0f

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                title = "Entries",
                value = attendanceCount.toString(),
                icon = Icons.Default.Group,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
            SummaryCard(
                title = "Present",
                value = presentCount.toString(),
                icon = Icons.Default.CheckCircle,
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
            SummaryCard(
                title = "Absent",
                value = absentCount.toString(),
                icon = Icons.Default.Warning,
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Student Attendance Rate", style = MaterialTheme.typography.titleMedium)
                Text("${attendancePercentage.toInt()}%", style = MaterialTheme.typography.headlineLarge)
                LinearProgressIndicator(
                    progress = (attendancePercentage / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                    color = when {
                        attendancePercentage >= 80 -> MaterialTheme.colorScheme.primary
                        attendancePercentage >= 60 -> Color(0xFFFFA000)
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }

        Text("Student Records", style = MaterialTheme.typography.titleMedium)
        if (uiState.attendanceLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.attendanceRecords.isEmpty()) {
            Text("No attendance data found for the selected filters.", style = MaterialTheme.typography.bodyMedium)
        } else {
            uiState.attendanceRecords.forEach { record ->
                AttendanceRecordRow(record)
            }
        }
 Spacer(modifier = Modifier.height(8.dp))
        Text("Teacher Attendance", style = MaterialTheme.typography.titleMedium)
        if (uiState.teacherAttendanceRecords.isEmpty()) {
            Text("No teacher attendance data found.", style = MaterialTheme.typography.bodyMedium)
        } else {
            uiState.teacherAttendanceRecords.forEach { record ->
                TeacherAttendanceRecordRow(record)
            }
        }
    }
}

@Composable
private fun RowScope.SummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color
) {
    Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AttendanceRecordRow(record: AttendanceRecord) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp), shape = RoundedCornerShape(14.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.studentName, style = MaterialTheme.typography.titleSmall)
                Text("${record.sectionId} • ${record.subjectName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(record.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusChip(status = record.status)
        }
    }
}

@Composable
private fun TeacherAttendanceRecordRow(record: TeacherAttendanceRecord) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp), shape = RoundedCornerShape(14.dp)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.teacherName, style = MaterialTheme.typography.titleSmall)
                Text("${record.sectionId} • ${record.subjectName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(record.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusChip(status = record.status)
        }
    }
}

@Composable
private fun StatusChip(status: AttendanceStatus) {
    val (color, label) = when (status) {
        AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.primary to "Present"
        AttendanceStatus.LATE -> Color(0xFFFFA000) to "Late"
        AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error to "Absent"
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = label,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}