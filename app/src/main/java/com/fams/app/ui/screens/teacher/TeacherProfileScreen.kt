package com.fams.app.ui.screens.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TeacherProfileScreen(
    state: TeacherUiState,
    onSignOut: () -> Unit,
    onThemeToggle: () -> Unit,
    isDarkTheme: Boolean
) {
    val user = state.currentUser
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            // Avatar + name
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AccountCircle, null,
                    modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(user?.fullName ?: "Teacher", style = MaterialTheme.typography.headlineSmall)
                Text("Teacher", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Profile Information", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    ProfileRow(Icons.Default.Email, "Email", user?.email ?: "")
                    ProfileRow(Icons.Default.Phone, "Phone", user?.phone.takeIf { !it.isNullOrBlank() } ?: "Not set")
                    ProfileRow(Icons.Default.School, "Specialization",
                        user?.specialization.takeIf { !it.isNullOrBlank() } ?: "Not set")
                    ProfileRow(Icons.Default.Badge, "Teacher ID",
                        user?.teacherId.takeIf { !it.isNullOrBlank() } ?: "Not set")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Teaching Summary", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    val sections = state.schedule.map { it.sectionName }.distinct()
                    val subjects = state.schedule.map { it.subjectName }.distinct()
                    Text("Sections: ${sections.joinToString(", ").ifBlank { "None assigned" }}",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Subjects: ${subjects.joinToString(", ").ifBlank { "None assigned" }}",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Total classes/week: ${state.schedule.size}",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    ListItem(headlineContent = { Text("Dark Mode") },
                        leadingContent = { Icon(if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode, null) },
                        trailingContent = { Switch(checked = isDarkTheme, onCheckedChange = { onThemeToggle() }) })
                }
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
fun ProfileRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
