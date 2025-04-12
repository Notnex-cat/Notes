@file:OptIn(ExperimentalMaterial3Api::class)

package com.notnex.notes.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.notnex.notes.R
import com.notnex.notes.database.Note


@Composable
fun NoteListScreen(
    notes: SnapshotStateList<Note>,
    selectedNotes: SnapshotStateList<Note>,
    isSelectionMode: Boolean,
    onToggleSelectionMode: (Note) -> Unit,
    onExitSelectionMode: () -> Unit,
    onAddNote: () -> Unit,
    onNoteClick: (Note) -> Unit,
    //onLogInClick: () -> Unit,
    onSettingsClick: () -> Unit,
    deleteNotes: (List<Note>) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) { // При нажатии "Назад" выходим из режима выделения
        onExitSelectionMode()
        selectedNotes.clear()
    }

    Scaffold(
        topBar = {
            if (!isSelectionMode) {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings)) },
                                onClick = {
                                    onSettingsClick()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.about_app)) },
                                onClick = {
                                    showAboutDialog = true
                                    showMenu = false
                                }
                            )
//                            DropdownMenuItem(
//                                text = { Text(stringResource(R.string.sign_in)) },
//                                onClick = {
//                                    onLogInClick()
//                                    showMenu = false
//                                }
//                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("${selectedNotes.size} выбрано") },
                    navigationIcon = {
                        IconButton(onClick = {
                            selectedNotes.clear()
                            onExitSelectionMode()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Отменить выбор")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            deleteNotes(selectedNotes.toList())
                            notes.removeAll(selectedNotes)
                            selectedNotes.clear()
                            onExitSelectionMode()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = onAddNote) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить заметку")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding)
        ) {
            items(notes) { note ->
                NoteItem(
                    note = note,
                    isSelected = selectedNotes.contains(note),
                    onClick = {
                        if (isSelectionMode) {
                            onToggleSelectionMode(note) // Передаём Note
                        } else {
                            onNoteClick(note)
                        }
                    },
                    onLongClick = {
                        if (!isSelectionMode) {
                            onToggleSelectionMode(note) // Передаём Note
                        }
                    }
                )
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.about_app)) },
            text = { Text("Это приложение для заметок. Версия 1.0.0.") },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItem(
    note: Note,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor =
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}