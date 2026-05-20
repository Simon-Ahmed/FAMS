package com.fams.app.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fams.app.domain.model.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(state: StudentUiState, viewModel: StudentViewModel) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Notes", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Body") }, modifier = Modifier.fillMaxWidth())

        Button(onClick = {
            if (title.isNotBlank()) {
                viewModel.addNote(title, body)
                title = ""; body = ""
            }
        }) { Text("Add Note") }

        Divider()

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.notes) { n: Note ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(n.title, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(n.body, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
