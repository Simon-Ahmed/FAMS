package com.fams.app.ui.screens.classrep

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fams.app.domain.model.Announcement
import com.fams.app.domain.model.AnnouncementReply
import com.fams.app.ui.screens.shared.AnnouncementsScreen
import com.fams.app.ui.screens.student.QuizTakingScreen


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassRepDashboard(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: ClassRepViewModel = viewModel(factory = ClassRepViewModel.Factory)
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Observe messages from ViewModel
    LaunchedEffect(state.successMessage, state.errorMessage) {
        val msg = state.successMessage ?: state.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    // Handle Quiz Overlay
    if (state.activeQuiz != null) {
        QuizTakingScreen(
            quiz = state.activeQuiz!!,
            onSubmit = { answers -> viewModel.submitQuiz(answers) },
            quizSessions = emptyList(),
            currentUserId = state.currentUser?.uid ?: ""
        )
        return
    }

    val tabs = listOf(
        "Home" to Icons.Default.Home,
        "Schedule" to Icons.Default.CalendarMonth,
        "Attendance" to Icons.Default.HowToReg,
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
                        IconButton(onClick = { }) { Icon(Icons.Default.Notifications, null) }
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
                        icon = { Icon(icon, label) },
                        label = { Text(label) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> ClassRepHomeScreen(state, viewModel)
                1 -> ClassRepScheduleScreen(state)
                2 -> ClassRepAttendanceScreen(state, viewModel)
                3 -> AnnouncementsScreen(
                    announcements = state.announcements,
                    currentUserId = state.currentUser?.uid ?: "",
                    currentUserName = state.currentUser?.fullName ?: "",
                    canPost = true,
                    targetRole = state.currentUser?.sectionId ?: "student",
                    onPost = { title, body ->
                        viewModel.postAnnouncement(
                            Announcement(
                                title = title,
                                body = body,
                                authorId = state.currentUser?.uid ?: "",
                                authorName = state.currentUser?.fullName ?: "",
                                targetRole = state.currentUser?.sectionId ?: "student"
                            )
                        )
                    },
                    onReply = { annId, msg ->
                        viewModel.replyToAnnouncement(
                            annId,
                            AnnouncementReply(
                                authorId = state.currentUser?.uid ?: "",
                                authorName = state.currentUser?.fullName ?: "",
                                message = msg
                            )
                        )
                    }
                )
                4 -> ClassRepChatScreen(state, viewModel)
                5 -> ClassRepProfileScreen(state, onSignOut, onThemeToggle, isDarkTheme)
            }
        }
    }
}