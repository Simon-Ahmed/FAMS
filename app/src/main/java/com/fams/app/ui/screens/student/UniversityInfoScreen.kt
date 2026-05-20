package com.fams.app.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fams.app.domain.model.UniversityInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversityInfoScreen(state: StudentUiState, viewModel: StudentViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("University Info", style = MaterialTheme.typography.titleLarge)
        Divider()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.uniInfo) { u: UniversityInfo ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(u.title, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(u.body, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
