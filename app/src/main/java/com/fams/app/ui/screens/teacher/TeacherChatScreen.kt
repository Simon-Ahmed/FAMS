package com.fams.app.ui.screens.teacher

import androidx.compose.runtime.Composable
import com.fams.app.ui.screens.shared.SharedChatScreen

@Composable
fun TeacherChatScreen(state: TeacherUiState, viewModel: TeacherViewModel) {
    SharedChatScreen(
        currentUserId = state.currentUser?.uid ?: "",
        currentUserName = state.currentUser?.fullName ?: "",
        chatThreads = state.chatThreads,
        messages = state.messages,
        availableUsers = state.availableUsers,
        onLoadMessages = { otherId -> viewModel.loadMessages(otherId) },
        onSendMessage = { receiverId, receiverName, text ->
            viewModel.sendMessage(receiverId, receiverName, text)
        },
        onLoadAvailableUsers = { viewModel.loadAvailableUsers() }
    )
}
