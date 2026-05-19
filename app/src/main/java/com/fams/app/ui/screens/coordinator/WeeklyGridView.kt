package com.fams.app.ui.screens.coordinator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fams.app.domain.model.ScheduleEntry

@Composable
fun WeeklyGridView(
    entries: List<ScheduleEntry>,
    modifier: Modifier = Modifier
) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

    // Collect all unique time slots sorted
    val timeSlots = entries
        .map { it.startTime to it.endTime }
        .distinct()
        .sortedBy { it.first }

    if (timeSlots.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center) {
            Text("No schedule entries to display",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // Build lookup: day+time -> list of entries
    val entryMap = entries.groupBy { it.day to it.startTime }

    val horizontalScroll = rememberScrollState()

    val cellWidth = 140.dp
    val timeColWidth = 72.dp
    val cellHeight = 80.dp
    val headerHeight = 40.dp

    Column(modifier = modifier) {
        // Section filter chips
        val sections = entries.map { it.sectionName }.distinct().sorted()
        var selectedSection by remember { mutableStateOf("All") }

        if (sections.size > 1) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedSection == "All",
                    onClick = { selectedSection = "All" },
                    label = { Text("All") }
                )
                sections.forEach { section ->
                    FilterChip(
                        selected = selectedSection == section,
                        onClick = { selectedSection = section },
                        label = { Text(section) }
                    )
                }
            }
        }

        val filteredEntries = if (selectedSection == "All") entries
            else entries.filter { it.sectionName == selectedSection }
        val filteredMap = filteredEntries.groupBy { it.day to it.startTime }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
        ) {
            Column {
                // Header row — day names
                Row {
                    // Empty corner cell
                    Box(
                        modifier = Modifier
                            .width(timeColWidth)
                            .height(headerHeight)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    days.forEach { day ->
                        Box(
                            modifier = Modifier
                                .width(cellWidth)
                                .height(headerHeight)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.take(3),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                // Time slot rows
                timeSlots.forEach { (start, end) ->
                    Row {
                        // Time label
                        Box(
                            modifier = Modifier
                                .width(timeColWidth)
                                .height(cellHeight)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(start, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(end, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Day cells
                        days.forEach { day ->
                            val cellEntries = filteredMap[day to start] ?: emptyList()
                            Box(
                                modifier = Modifier
                                    .width(cellWidth)
                                    .height(cellHeight)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(2.dp)
                            ) {
                                if (cellEntries.isEmpty()) {
                                    // Empty cell
                                } else if (cellEntries.size == 1) {
                                    GridCell(entry = cellEntries[0])
                                } else {
                                    // Multiple sections in same slot
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        cellEntries.take(3).forEach { entry ->
                                            GridCell(entry = entry, compact = true)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridCell(entry: ScheduleEntry, compact: Boolean = false) {
    val bgColor = MaterialTheme.colorScheme.primaryContainer
    val textColor = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Column {
            Text(
                text = entry.subjectName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (compact) 9.sp else 11.sp
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!compact) {
                Text(
                    text = entry.teacherName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = textColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${entry.sectionName} • ${entry.roomName}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = textColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = entry.sectionName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = textColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
