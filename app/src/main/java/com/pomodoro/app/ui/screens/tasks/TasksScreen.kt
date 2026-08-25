package com.pomodoro.app.ui.screens.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pomodoro.app.data.model.DefaultPresets
import com.pomodoro.app.data.model.Task
import com.pomodoro.app.data.model.TimerPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onTaskSelected: (Task) -> Unit,
    viewModel: TasksViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tasks",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            FilledIconButton(
                onClick = { viewModel.showAddTaskDialog() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Task list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.tasks, key = { it.id }) { task ->
                TaskItem(
                    task = task,
                    onToggle = { viewModel.toggleTask(task) },
                    onDelete = { viewModel.deleteTask(task) },
                    onSelect = { onTaskSelected(task) }
                )
            }
        }
    }

    if (uiState.isAddingTask) {
        AlertDialog(
            onDismissRequest = { viewModel.hideAddTaskDialog() },
            title = { Text("New Task") },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.newTaskTitle,
                        onValueChange = { viewModel.updateNewTaskTitle(it) },
                        label = { Text("Task Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select Profile:", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    val allPresets = DefaultPresets.all + uiState.customPresets

                    // Simple dropdown or list of chips for presets
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allPresets) { preset ->
                            FilterChip(
                                selected = uiState.selectedPreset == preset,
                                onClick = { viewModel.updateSelectedPreset(preset) },
                                label = { Text(preset.name) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.addTask() },
                    enabled = uiState.newTaskTitle.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideAddTaskDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (task.isCompleted)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surface,
        label = "task_bg"
    )

    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Outlined.CheckCircle
                                 else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (task.isCompleted) "Completed" else "Not completed",
                    tint = if (task.isCompleted) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.onSurface
                )
                if (task.presetName != null) {
                    Text(
                        text = "Profile: ${task.presetName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
