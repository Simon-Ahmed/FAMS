package com.fams.app.ui.screens.coordinator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fams.app.domain.model.ScheduleConfig
import com.fams.app.domain.model.SubjectRequirement

@Composable
fun ScheduleConfigTab(uiState: ScheduleUiState, viewModel: ScheduleViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Config", "Requirements")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { i, title ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i },
                    text = { Text(title) })
            }
        }
        when (selectedTab) {
            0 -> ConfigPanel(uiState, viewModel)
            1 -> RequirementsPanel(uiState, viewModel)
        }
    }
}

// ── Config Panel ──────────────────────────────────────────────────────────────

@Composable
fun ConfigPanel(uiState: ScheduleUiState, viewModel: ScheduleViewModel) {
    var maxHours by remember(uiState.config) {
        mutableStateOf(uiState.config.maxTeacherCreditHoursPerWeek.toString())
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Teacher Workload Configuration", style = MaterialTheme.typography.titleMedium)
            Text("Set the maximum credit hours a teacher can be assigned per week across all subjects.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            OutlinedTextField(
                value = maxHours,
                onValueChange = { maxHours = it },
                label = { Text("Max Teacher Credit Hours / Week") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.Schedule, null) },
                supportingText = {
                    Text("e.g. 12 → a teacher with Math(5) + Physics(3) = 8 hrs, can still take 4 more")
                }
            )
        }

        item {
            Button(
                onClick = {
                    val h = maxHours.toIntOrNull() ?: 12
                    viewModel.saveConfig(uiState.config.maxStudentsPerSection, h)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save")
            }
        }
    }
}

// ── Requirements Panel ────────────────────────────────────────────────────────

@Composable
fun RequirementsPanel(uiState: ScheduleUiState, viewModel: ScheduleViewModel) {
    val allSufficient = uiState.requirements.all { it.isSufficient }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Teacher Requirements Analysis",
                style = MaterialTheme.typography.titleMedium)
            Text("Based on ${uiState.sections.size} sections and " +
                "${uiState.config.maxTeacherCreditHoursPerWeek} max credit hours/teacher/week",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (uiState.requirements.isEmpty()) {
            item {
                Text("Add subjects and sections first to see requirements.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            item {
                // Overall status
                Card(colors = CardDefaults.cardColors(
                    containerColor = if (allSufficient)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            if (allSufficient) Icons.Default.CheckCircle else Icons.Default.Warning,
                            null,
                            tint = if (allSufficient)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            if (allSufficient) "All subjects have sufficient teachers"
                            else "Some subjects need more teachers",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (allSufficient)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            items(uiState.requirements) { req ->
                RequirementCard(req)
            }
        }
    }
}

@Composable
fun RequirementCard(req: SubjectRequirement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (req.isSufficient)
                MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(req.subject.name, style = MaterialTheme.typography.titleMedium)
                    Text("${req.subject.creditHours} credit hours/week",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    if (req.isSufficient) Icons.Default.CheckCircle else Icons.Default.Warning,
                    null,
                    tint = if (req.isSufficient) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error
                )
            }

            HorizontalDivider()

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                RequirementStat("Sections", req.sectionCount.toString())
                RequirementStat("Sections/Teacher",
                    "${req.sectionsPerTeacher} max")
                RequirementStat("Teachers Needed",
                    req.minTeachersNeeded.toString(),
                    highlight = true)
                RequirementStat("Assigned",
                    req.assignedTeacherCount.toString(),
                    isError = !req.isSufficient)
            }

            if (!req.isSufficient) {
                val missing = req.minTeachersNeeded - req.assignedTeacherCount
                Text("⚠ Assign $missing more teacher${if (missing > 1) "s" else ""} to ${req.subject.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun RequirementStat(
    label: String,
    value: String,
    highlight: Boolean = false,
    isError: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value,
            style = MaterialTheme.typography.titleMedium,
            color = when {
                isError -> MaterialTheme.colorScheme.error
                highlight -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            })
        Text(label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
