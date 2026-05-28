package com.fams.app.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fams.app.domain.model.TodoItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(state: StudentUiState, viewModel: StudentViewModel) {
    var newTodo by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("To‑Do", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = newTodo, onValueChange = { newTodo = it }, modifier = Modifier.weight(1f), placeholder = { Text("New task...") })
            Button(onClick = {
                if (newTodo.isNotBlank()) {
                    viewModel.addTodo(newTodo)
                    newTodo = ""
                }
            }) { Text("Add") }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.todos) { t: TodoItem ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = t.done, onCheckedChange = { viewModel.toggleTodo(t.id, it) })
                        Text(t.title, modifier = Modifier.padding(start = 8.dp))
                    }
                    IconButton(onClick = { viewModel.deleteTodo(t.id) }) { Icon(Icons.Default.Delete, null) }
                }
            }
        }
    }
}
