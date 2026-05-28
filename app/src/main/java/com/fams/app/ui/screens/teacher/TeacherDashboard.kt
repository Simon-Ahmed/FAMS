package com.fams.app.ui.screens.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboard(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: TeacherViewModel = viewModel(factory = TeacherViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.successMessage, state.errorMessage) {
        val msg = state.successMessage ?: state.errorMessage
        if (msg != null) { snackbarHostState.showSnackbar(msg); viewModel.clearMessages() }
    }

    // Handle quick action navigation from Home screen
    LaunchedEffect(state.quickActionTab) {
        state.quickActionTab?.let { tab ->
            selectedTab = tab
            viewModel.clearQuickAction()
        }
    }

    val tabs = listOf(
        "Home" to Icons.Default.Home,
        "Schedule" to Icons.Default.CalendarMonth,
        "Classes" to Icons.Default.School,
        "Announce" to Icons.Default.Campaign,
        "Chat" to Icons.Default.Chat,
        "Profile" to Icons.Default.Person
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tabs[selectedTab].first) },
                actions = {
                    BadgedBox(badge = {
                        val unread = state.notifications.count { !it.isRead }
                        if (unread > 0) Badge { Text(unread.toString()) }
                    }) {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Notifications, null)
                        }
                    }
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
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> TeacherHomeScreen(state, viewModel, onSignOut)
                1 -> TeacherScheduleScreen(state, viewModel)
                2 -> TeacherClassesScreen(state, viewModel)
                3 -> com.fams.app.ui.screens.shared.AnnouncementsScreen(
                    announcements = state.announcements,
                    currentUserId = state.currentUser?.uid ?: "",
                    currentUserName = state.currentUser?.fullName ?: "",
                    canPost = true,
                    targetRole = "student",
                    onPost = { title, body ->
                        viewModel.postAnnouncement(
                            com.fams.app.domain.model.Announcement(
                                title = title, body = body,
                                authorId = state.currentUser?.uid ?: "",
                                authorName = state.currentUser?.fullName ?: "",
                                targetRole = "student"
                            )
                        )
                    },
                    onReply = { annId, msg ->
                        viewModel.replyToAnnouncement(annId,
                            com.fams.app.domain.model.AnnouncementReply(
                                authorId = state.currentUser?.uid ?: "",
                                authorName = state.currentUser?.fullName ?: "",
                                message = msg
                            ))
                    }
                )
                4 -> TeacherChatScreen(state, viewModel)
                5 -> TeacherProfileScreen(state, onSignOut, onThemeToggle, isDarkTheme)
            }
        }
    }
}
