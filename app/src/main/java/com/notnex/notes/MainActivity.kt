@file:OptIn(ExperimentalMaterial3Api::class)

package com.notnex.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.notnex.notes.database.Note
import com.notnex.notes.database.NoteDao
import com.notnex.notes.database.NotesAppDatabase
import com.notnex.notes.screens.EditNoteScreen
import com.notnex.notes.screens.Settings
import com.notnex.notes.ui.theme.NotesTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var db: NotesAppDatabase
    private lateinit var noteDao: NoteDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Room database
        db = Room.databaseBuilder(
            applicationContext,
            NotesAppDatabase::class.java, "notes-database"
        ).build()
        noteDao = db.noteDao()

        enableEdgeToEdge()
        setContent {
            NotesTheme {
                NotesApp(noteDao)
            }
        }
    }

    @Suppress("SpellCheckingInspection")
    @Composable
    fun NotesApp(noteDao: NoteDao) {

        //подключение к файрсторе
        val fs = Firebase.firestore

        val navController = rememberNavController()
        var notes by remember { mutableStateOf(listOf<Note>()) }
        var currentNote by remember { mutableStateOf<Note?>(null) }

        // Load notes from the database
        LaunchedEffect(Unit) {
            notes = noteDao.getAllNotes()
        }

        NavHost(
            navController,
            startDestination = "notes_list", //стартовая страница
        ) {
            composable("notes_list") {
                NoteListScreen(
                    notes = notes,
                    onAddNote = {
                        currentNote = null
                        navController.navigate("edit_note")
                    },
                    onNoteClick = { note ->
                        currentNote = note
                        navController.navigate("edit_note")
                    },
                    onSettingsClick = {
                        navController.navigate("notes_settings")
                    }
                )
            }
            composable("edit_note") {
                EditNoteScreen(
                    note = currentNote,
                    onSave = { title, text ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                if (currentNote == null) {
                                    // Создаем новую заметку без явного указания id
                                    val newNote = Note(title = title, text = text, timestamp = System.currentTimeMillis())
                                    noteDao.insert(newNote) // Вставка в базу данных
                                    fs.collection("notes")
                                        .document().set(
                                            Note(
                                                title = title,
                                                text = text
                                            )
                                        )
                                } else {
                                    // Обновляем существующую заметку
                                    val updatedNote = currentNote!!.copy(title = title, text = text)
                                    noteDao.update(updatedNote)
                                    /*fs.collection("notes").document().update(Note(
                                        title = title,
                                        text = text
                                    ))*/
                                }
                                //в базу отправляем заметку

                                // Получаем все заметки
                                notes = noteDao.getAllNotes()
                            }
                            // Возвращаемся на предыдущий экран
                            navController.navigateUp()
                        }
                    },
                    onBack = {
                        navController.navigateUp()
                    }
                )
            }
            composable("notes_settings") {
                Settings()
            }
        }
    }

    @Composable
    fun NoteListScreen(
        notes: List<Note>,
        onAddNote: () -> Unit,
        onSettingsClick: () -> Unit,
        onNoteClick: (Note) -> Unit
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.app_name)) },
                    actions = {
                        IconButton(onClick = {
                            onSettingsClick()
                        }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddNote) {
                    Icon(Icons.Default.Add, contentDescription = "Add Note")
                }
            }
        ) { innerPadding ->
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(notes) { note ->
                    NoteItem(note = note, onClick = { onNoteClick(note) })
                }
            }
        }
    }

    @Composable
    fun NoteItem(note: Note, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(text = note.title, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = note.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        NotesApp(noteDao)
    }
}