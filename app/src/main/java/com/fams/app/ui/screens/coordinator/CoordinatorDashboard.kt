package com.fams.app.ui.screens.coordinator

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fams.app.di.FirebaseModule

sealed class CoordinatorTab(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Home     : CoordinatorTab("Home",     Icons.Default.Dashboard)
    object Students : CoordinatorTab("Students", Icons.Default.People)
    object Teachers : CoordinatorTab("Teachers", Icons.Default.School)
    object Sections : CoordinatorTab("Sections", Icons.Default.GridView)
    object Schedule : CoordinatorTab("Schedule", Icons.Default.CalendarMonth)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoordinatorDashboard(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: CoordinatorViewModel = viewModel(factory = CoordinatorViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf<CoordinatorTab>(CoordinatorTab.Home) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coordinatorEmail = remember { FirebaseModule.auth.currentUser?.email ?: "" }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    uiState.generatedCredential?.let { (email, password) ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Account Created") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Share these login credentials with the user:")
                    OutlinedCard {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Email:", style = MaterialTheme.typography.labelLarge)
                            Text(email, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("Password:", style = MaterialTheme.typography.labelLarge)
                            Text(password, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text(
                        "You will be signed out after dismissing. Sign back in as coordinator.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearMessages()
                    onSignOut()
                }) { Text("Done — Sign me back in") }
            }
        )
    }

    val tabs = listOf(
        CoordinatorTab.Home,
        CoordinatorTab.Students,
        CoordinatorTab.Teachers,
        CoordinatorTab.Sections,
        CoordinatorTab.Schedule
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedTab.label) },
                actions = {
                    IconButton(onClick = onThemeToggle) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode
                                          else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme"
                        )
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                CoordinatorTab.Home     -> CoordinatorHomeTab(uiState)
                CoordinatorTab.Students -> StudentsTab(uiState, viewModel, coordinatorEmail)
                CoordinatorTab.Teachers -> TeachersTab(uiState, viewModel, coordinatorEmail)
                CoordinatorTab.Sections -> SectionsTab(uiState, viewModel)
                CoordinatorTab.Schedule -> ScheduleScreen()
            }
        }
    }
}
