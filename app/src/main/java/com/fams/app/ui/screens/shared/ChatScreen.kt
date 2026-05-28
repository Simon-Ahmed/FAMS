package com.fams.app.ui.screens.shared

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
import com.fams.app.domain.model.ChatMessage
import com.fams.app.domain.model.ChatThread
import com.fams.app.domain.model.User
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reusable chat screen used by Teacher, ClassRep.
 * Supports starting new conversations from a user list.
 */
@Composable
fun SharedChatScreen(
    currentUserId: String,
    currentUserName: String,
    chatThreads: List<ChatThread>,
    messages: List<ChatMessage>,
    availableUsers: List<User>,          // users this role can chat with
    onLoadMessages: (otherId: String) -> Unit,
    onSendMessage: (receiverId: String, receiverName: String, text: String) -> Unit,
    onLoadAvailableUsers: () -> Unit
) {
    var selectedThread by remember { mutableStateOf<ChatThread?>(null) }
    var selectedNewUser by remember { mutableStateOf<User?>(null) }
    var messageText by remember { mutableStateOf("") }
    var showUserPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // Determine active conversation
    val activeOtherId = when {
        selectedThread != null -> selectedThread!!.participantIds.firstOrNull { it != currentUserId } ?: ""
        selectedNewUser != null -> selectedNewUser!!.uid
        else -> ""
    }
    val activeOtherName = when {
        selectedThread != null -> selectedThread!!.participantNames[activeOtherId] ?: "Unknown"
        selectedNewUser != null -> selectedNewUser!!.fullName
        else -> ""
    }
    val inConversation = activeOtherId.isNotBlank()

    if (!inConversation) {
        // Thread list
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text("Messages", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp))

                if (chatThreads.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Chat, null, modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("No conversations yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Tap + to start a new conversation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(chatThreads) { thread ->
                            val otherName = thread.participantNames.entries
                                .firstOrNull { it.key != currentUserId }?.value ?: "Unknown"
                            val otherId = thread.participantIds.firstOrNull { it != currentUserId } ?: ""
                            Card(modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    selectedThread = thread
                                    selectedNewUser = null
                                    onLoadMessages(otherId)
                                }) {
                                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountCircle, null,
                                            modifier = Modifier.size(44.dp),
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

            FloatingActionButton(
                onClick = {
                    onLoadAvailableUsers()
                    showUserPicker = true
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) { Icon(Icons.Default.Edit, contentDescription = "New conversation") }
        }
    } else {
        // Conversation view
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    selectedThread = null
                    selectedNewUser = null
                }) { Icon(Icons.Default.ArrowBack, null) }
                Icon(Icons.Default.AccountCircle, null,
                    modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(activeOtherName, style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider()

            LazyColumn(state = listState, modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (messages.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center) {
                            Text("Send a message to start the conversation",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                items(messages) { msg ->
                    val isMe = msg.senderId == currentUserId
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
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") }, maxLines = 3)
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(activeOtherId, activeOtherName, messageText)
                            messageText = ""
                            // After sending, reload messages
                            onLoadMessages(activeOtherId)
                        }
                    },
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // User picker dialog
    if (showUserPicker) {
        AlertDialog(
            onDismissRequest = { showUserPicker = false },
            title = { Text("New Conversation") },
            text = {
                if (availableUsers.isEmpty()) {
                    Text("No users available to message.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(availableUsers) { user ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                onClick = {
                                    selectedNewUser = user
                                    selectedThread = null
                                    showUserPicker = false
                                    onLoadMessages(user.uid)
                                }) {
                                Row(modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountCircle, null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text(user.fullName, style = MaterialTheme.typography.titleSmall)
                                        Text(user.role.value.replace("_", " ")
                                            .replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showUserPicker = false }) { Text("Cancel") } }
        )
    }
}
