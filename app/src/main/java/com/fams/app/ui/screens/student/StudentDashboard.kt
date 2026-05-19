package com.fams.app.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: StudentViewModel = viewModel(factory = StudentViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.successMessage, state.errorMessage) {
        val msg = state.successMessage ?: state.errorMessage
        if (msg != null) { snackbarHostState.showSnackbar(msg); viewModel.clearMessages() }
    }

    // If quiz is active, show quiz screen full-screen
    if (state.activeQuiz != null) {
        QuizTakingScreen(quiz = state.activeQuiz!!, onSubmit = { answers -> viewModel.submitQuiz(answers) })
        return
    }

    val tabs = listOf(
        "Home" to Icons.Default.Home,
        "Schedule" to Icons.Default.CalendarMonth,
        "Courses" to Icons.Default.MenuBook,
        "Notifications" to Icons.Default.Notifications,
        "Chat" to Icons.Default.Chat,
        "Profile" to Icons.Default.Person
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tabs[selectedTab].first) },
                actions = {
                    IconButton(onClick = onThemeToggle) {
                        Icon(if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, null)
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
                tabs.forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        icon = {
                            if (i == 3) {
                                val unread = state.notifications.count { !it.isRead }
                                BadgedBox(badge = { if (unread > 0) Badge { Text(unread.toString()) } }) {
                                    Icon(icon, label)
                                }
                            } else Icon(icon, label)
                        },
                        label = { Text(label) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> StudentHomeScreen(state, viewModel)
                1 -> StudentScheduleScreen(state)
                2 -> StudentCoursesScreen(state, viewModel)
                3 -> StudentNotificationsScreen(state, viewModel)
                4 -> StudentChatScreen(state, viewModel)
                5 -> StudentProfileScreen(state, onSignOut, onThemeToggle, isDarkTheme)
            }
        }
    }
}
