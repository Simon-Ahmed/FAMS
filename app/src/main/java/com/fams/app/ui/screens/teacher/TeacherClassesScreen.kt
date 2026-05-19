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
import com.fams.app.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherClassesScreen(state: TeacherUiState, viewModel: TeacherViewModel) {
    // Unique sections from the teacher's schedule
    val sections = state.schedule.map { it.sectionId to it.sectionName }.distinct()

    var selectedSection by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Handle quick action pre-selection from Home screen
    LaunchedEffect(state.quickActionSubTab, state.quickActionSectionId) {
        val subTab = state.quickActionSubTab
        val sectionId = state.quickActionSectionId
        if (subTab != null) {
            selectedTab = subTab
            // Auto-select section: use provided sectionId or first available
            val target = if (sectionId != null)
                sections.firstOrNull { it.first == sectionId }
            else sections.firstOrNull()
            if (target != null) {
                selectedSection = target
                viewModel.loadSectionData(target.first)
            }
        }
    }

    if (selectedSection == null) {
        // Section list
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("My Classes", style = MaterialTheme.typography.titleMedium)
                Text("Tap a section to manage attendance, materials, assignments, quizzes and grades.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
            }
            if (sections.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.School, null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("No classes assigned yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("The coordinator needs to publish a schedule first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            items(sections) { (id, name) ->
                val subjects = state.schedule
                    .filter { it.sectionId == id }
                    .map { it.subjectName }
                    .distinct()
                val studentCount = state.sectionStudents.size

                Card(modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        selectedSection = id to name
                        viewModel.loadSectionData(id)
                    }) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleMedium)
                            Text(subjects.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.Default.ChevronRight, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    } else {
        val (sectionId, sectionName) = selectedSection!!

        Column(modifier = Modifier.fillMaxSize()) {
            // Back button + section name
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                IconButton(onClick = { selectedSection = null }) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
                Column {
                    Text(sectionName, style = MaterialTheme.typography.titleLarge)
                    val subjects = state.schedule
                        .filter { it.sectionId == sectionId }
                        .map { it.subjectName }.distinct()
                    Text(subjects.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }

            ScrollableTabRow(selectedTabIndex = selectedTab) {
                listOf("Attendance", "Materials", "Assignments", "Quizzes", "Grades")
                    .forEachIndexed { i, t ->
                        Tab(selected = selectedTab == i,
                            onClick = { selectedTab = i },
                            text = { Text(t) })
                    }
            }

            when (selectedTab) {
                0 -> AttendanceTab(state, viewModel, sectionId)
                1 -> MaterialsTab(state, viewModel, sectionId)
                2 -> AssignmentsTab(state, viewModel, sectionId)
                3 -> QuizzesTab(state, viewModel, sectionId)
                4 -> GradesTab(state, viewModel, sectionId)
            }
        }
    }
}
