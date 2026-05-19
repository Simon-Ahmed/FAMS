package com.fams.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Header shown when selection mode is active.
 */
@Composable
fun SelectionModeHeader(
    totalCount: Int,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onExitSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allSelected = selectedCount == totalCount && totalCount > 0

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExitSelection) {
                    Icon(Icons.Default.Close, contentDescription = "Exit selection",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Checkbox(
                    checked = allSelected,
                    onCheckedChange = { if (it) onSelectAll() else onDeselectAll() }
                )
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            IconButton(
                onClick = onDeleteSelected,
                enabled = selectedCount > 0
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete selected",
                    tint = if (selectedCount > 0) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f))
            }
        }
    }
}

/**
 * Wraps any item with long-press to enter selection mode and tap to toggle.
 * Shows a leading checkbox only when selection mode is active.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SelectableItem(
    id: String,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onLongPress: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggle() },
                onLongClick = { if (!isSelectionMode) onLongPress() }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}
