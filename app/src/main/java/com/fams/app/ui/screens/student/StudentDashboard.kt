package com.fams.app.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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

    val drawerState = remember { ModalDrawerState(DrawerValue.Closed) }
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                NavigationDrawerItem(label = { Text("Home") }, selected = selectedTab == 0, onClick = { scope.launch { drawerState.close() }; selectedTab = 0 })
                NavigationDrawerItem(label = { Text("Notes") }, selected = selectedTab == 6, onClick = { scope.launch { drawerState.close() }; selectedTab = 6 })
                NavigationDrawerItem(label = { Text("To‑Do") }, selected = selectedTab == 8, onClick = { scope.launch { drawerState.close() }; selectedTab = 8 })
                NavigationDrawerItem(label = { Text("University Info") }, selected = selectedTab == 7, onClick = { scope.launch { drawerState.close() }; selectedTab = 7 })
                Divider()
                NavigationDrawerItem(label = { Text("Profile") }, selected = selectedTab == 5, onClick = { scope.launch { drawerState.close() }; selectedTab = 5 })
                NavigationDrawerItem(label = { Text("Sign out") }, selected = false, onClick = { scope.launch { drawerState.close() }; onSignOut() })
            }
        }
    ) {
        Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, null) }
                },
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
                6 -> NotesScreen(state, viewModel)
                7 -> UniversityInfoScreen(state, viewModel)
                8 -> TodoScreen(state, viewModel)
            }
        }
    }
}
