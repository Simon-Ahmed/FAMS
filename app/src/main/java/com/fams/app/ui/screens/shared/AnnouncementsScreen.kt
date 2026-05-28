package com.fams.app.ui.screens.shared

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
import com.fams.app.domain.model.Announcement
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reusable announcements screen.
 * canPost = true for coordinator and teacher
 * canReply = true for all roles
 */
@Composable
fun AnnouncementsScreen(
    announcements: List<Announcement>,
    currentUserId: String,
    currentUserName: String,
    canPost: Boolean,
    targetRole: String,          // "all", "student", "teacher", or sectionId
    onPost: (title: String, body: String) -> Unit,
    onReply: (announcementId: String, message: String) -> Unit
) {
    var showPostDialog by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var replyingToId by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (announcements.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Campaign, null, modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("No announcements yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (canPost) {
                        Spacer(Modifier.height(4.dp))
                        Text("Tap + to post an announcement",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(announcements) { ann ->
                    val isExpanded = expandedId == ann.id
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ann.title, style = MaterialTheme.typography.titleMedium)
                                    Text("${ann.authorName} • ${
                                        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                                            .format(Date(ann.createdAt))
                                    }", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {
                                    expandedId = if (isExpanded) null else ann.id
                                }) {
                                    Icon(
                                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        null
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Text(ann.body, style = MaterialTheme.typography.bodyMedium,
                                maxLines = if (isExpanded) Int.MAX_VALUE else 2)

                            if (isExpanded) {
                                // Replies
                                if (ann.replies.isNotEmpty()) {
                                    Spacer(Modifier.height(12.dp))
                                    HorizontalDivider()
                                    Spacer(Modifier.height(8.dp))
                                    Text("Replies (${ann.replies.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(4.dp))
                                    ann.replies.forEach { reply ->
                                        Card(modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()) {
                                                    Text(reply.authorName,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary)
                                                    Text(SimpleDateFormat("MMM d HH:mm", Locale.getDefault())
                                                        .format(Date(reply.timestamp)),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Text(reply.message,
                                                    style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                    }
                                }

                                // Reply button
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { replyingToId = ann.id },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Reply, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Reply")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (canPost) {
            FloatingActionButton(
                onClick = { showPostDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "Post announcement") }
        }
    }

    // Post dialog
    if (showPostDialog) {
        PostAnnouncementDialog(
            onDismiss = { showPostDialog = false },
            onConfirm = { title, body ->
                onPost(title, body)
                showPostDialog = false
            }
        )
    }

    // Reply dialog
    replyingToId?.let { annId ->
        ReplyDialog(
            onDismiss = { replyingToId = null },
            onConfirm = { message ->
                onReply(annId, message)
                replyingToId = null
            }
        )
    }
}

@Composable
fun PostAnnouncementDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post Announcement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Title *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = body, onValueChange = { body = it },
                    label = { Text("Message *") }, modifier = Modifier.fillMaxWidth(), maxLines = 5)
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank() && body.isNotBlank()) onConfirm(title, body) }) {
                Text("Post")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ReplyDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var message by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reply") },
        text = {
            OutlinedTextField(value = message, onValueChange = { message = it },
                label = { Text("Your reply") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)
        },
        confirmButton = {
            Button(onClick = { if (message.isNotBlank()) onConfirm(message) }) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
