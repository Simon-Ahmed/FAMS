package com.fams.app.ui.screens.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fams.app.domain.model.ChatThread
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TeacherChatScreen(state: TeacherUiState, viewModel: TeacherViewModel) {
    var selectedThread by remember { mutableStateOf<ChatThread?>(null) }
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }

    if (selectedThread == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("Messages", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp))
            if (state.chatThreads.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No conversations yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.chatThreads) { thread ->
                        val me = state.currentUser?.uid ?: ""
                        val otherName = thread.participantNames.entries
                            .firstOrNull { it.key != me }?.value ?: "Unknown"
                        Card(modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                selectedThread = thread
                                val otherId = thread.participantIds.firstOrNull { it != me } ?: ""
                                viewModel.loadMessages(otherId)
                            }) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountCircle, null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text(otherName, style = MaterialTheme.typography.titleSmall)
                                        Text(thread.lastMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1)
                                    }
                                }
                                if (thread.unreadCount > 0) Badge { Text(thread.unreadCount.toString()) }
                            }
                        }
                    }
                }
            }
        }
    } else {
        val thread = selectedThread!!
        val me = state.currentUser?.uid ?: ""
        val otherId = thread.participantIds.firstOrNull { it != me } ?: ""
        val otherName = thread.participantNames[otherId] ?: "Unknown"

        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedThread = null }) { Icon(Icons.Default.ArrowBack, null) }
                Icon(Icons.Default.AccountCircle, null,
                    modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(otherName, style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider()
            LazyColumn(state = listState, modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.messages) { msg ->
                    val isMe = msg.senderId == me
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start) {
                        Card(colors = CardDefaults.cardColors(
                            containerColor = if (isMe) MaterialTheme.colorScheme.primary
                                             else MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(12.dp).widthIn(max = 260.dp)) {
                                Text(msg.message,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
            Row(modifier = Modifier.padding(8.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = messageText, onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f), placeholder = { Text("Type a message...") },
                    maxLines = 3)
                IconButton(onClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(otherId, otherName, messageText)
                        messageText = ""
                    }
                }, enabled = messageText.isNotBlank()) {
                    Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
