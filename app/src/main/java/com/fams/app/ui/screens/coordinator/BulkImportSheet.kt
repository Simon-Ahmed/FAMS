package com.fams.app.ui.screens.coordinator

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun BulkImportStudentsButton(
    uiState: CoordinatorUiState,
    viewModel: CoordinatorViewModel,
    coordinatorEmail: String
) {
    val context = LocalContext.current
    var showPasswordDialog by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var showFailedDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingUri = uri
            showPasswordDialog = true
        }
    }

    OutlinedButton(
        onClick = { launcher.launch("*/*") },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Import Students from Excel")
    }

    if (uiState.importFailedRows.isNotEmpty()) {
        TextButton(onClick = { showFailedDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("${uiState.importFailedRows.size} rows failed — tap to view",
                color = MaterialTheme.colorScheme.error)
        }
    }

    if (showPasswordDialog) {
        ConfirmPasswordDialog(
            onDismiss = { showPasswordDialog = false; pendingUri = null },
            onConfirm = { password ->
                pendingUri?.let {
                    viewModel.bulkImportStudents(context, it, coordinatorEmail, password)
                }
                showPasswordDialog = false
                pendingUri = null
            }
        )
    }

    if (showFailedDialog) {
        ImportErrorsDialog(
            rows = uiState.importFailedRows,
            onDismiss = { showFailedDialog = false; viewModel.clearImportErrors() }
        )
    }
}

@Composable
fun BulkImportTeachersButton(
    uiState: CoordinatorUiState,
    viewModel: CoordinatorViewModel,
    coordinatorEmail: String
) {
    val context = LocalContext.current
    var showPasswordDialog by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var showFailedDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingUri = uri
            showPasswordDialog = true
        }
    }

    OutlinedButton(
        onClick = { launcher.launch("*/*") },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Import Teachers from Excel")
    }

    if (uiState.importFailedRows.isNotEmpty()) {
        TextButton(onClick = { showFailedDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("${uiState.importFailedRows.size} rows failed — tap to view",
                color = MaterialTheme.colorScheme.error)
        }
    }

    if (showPasswordDialog) {
        ConfirmPasswordDialog(
            onDismiss = { showPasswordDialog = false; pendingUri = null },
            onConfirm = { password ->
                pendingUri?.let {
                    viewModel.bulkImportTeachers(context, it, coordinatorEmail, password)
                }
                showPasswordDialog = false
                pendingUri = null
            }
        )
    }

    if (showFailedDialog) {
        ImportErrorsDialog(
            rows = uiState.importFailedRows,
            onDismiss = { showFailedDialog = false; viewModel.clearImportErrors() }
        )
    }
}

@Composable
fun ConfirmPasswordDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Your Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter your coordinator password to keep your session active during import.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Your Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (password.isNotBlank()) onConfirm(password) }) {
                Text("Start Import")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ImportErrorsDialog(rows: List<String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Errors") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(rows) { row ->
                    Text("• $row", style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ExcelFormatHint(isStudent: Boolean) {
    Card(colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text("Excel Format", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            if (isStudent) {
                Text("Column A: Student ID\nColumn B: Full Name\nColumn C: Email\nColumn D: Phone",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Column A: Teacher ID\nColumn B: Full Name\nColumn C: Email\nColumn D: Phone\nColumn E: Specialization",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
