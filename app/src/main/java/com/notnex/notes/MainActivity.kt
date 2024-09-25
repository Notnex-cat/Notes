@file:OptIn(ExperimentalMaterial3Api::class)

package com.notnex.notes

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
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

            // Получение всех заметок в фоновом потоке
        lifecycleScope.launch {
            val notes = noteDao.getAllNotes()
            // Обработка данных на главном потоке
        }
        enableEdgeToEdge()
        setContent {
            NotesApp(noteDao)
        }
    }


    @Composable
    fun NotesApp(noteDao: NoteDao) {
        //val scope = rememberCoroutineScope()
        var notes by remember { mutableStateOf(emptyList<Note>()) }
        val navController = rememberNavController()
        //var notes by remember { mutableStateOf(listOf<Note>()) }
        var currentNote by remember { mutableStateOf<Note?>(null) }

        // Load notes from the database
        LaunchedEffect(Unit) {
            notes = noteDao.getAllNotes()
        }

        NavHost(navController, startDestination = "notes_list") {
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
                                    val newNote = Note(title = title, text = text)
                                    noteDao.insert(newNote) // Вставка в базу данных
                                } else {
                                    // Обновляем существующую заметку
                                    val updatedNote = currentNote!!.copy(title = title, text = text)
                                    noteDao.update(updatedNote)
                                }

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
        }
    }

    @Composable
    fun NoteListScreen(
        notes: List<Note>,
        onAddNote: () -> Unit,
        onNoteClick: (Note) -> Unit
    ) {
        Scaffold(
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
            //elevation = 4.dp
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

    @Composable
    fun EditNoteScreen(
        note: Note?,
        onSave: (String, String) -> Unit,
        onBack: () -> Unit
    ) {
        var title by rememberSaveable { mutableStateOf(note?.title ?: "") }
        var text by rememberSaveable { mutableStateOf(note?.text ?: "") }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (note == null) "New Note" else "Edit Note") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            Log.d("EditNoteScreen", "Saving note with title: $title and text: $text")
                            onSave(title, text)
                            onBack()
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                )
            },
            bottomBar = {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    IconButton(onClick = { /* TODO: действие для добавления */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                    IconButton(onClick = { /* TODO: действие для изменения темы */ }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Change Theme"
                        ) // Используем доступную иконку
                    }
                    IconButton(onClick = { /* TODO: действие для текста */ }) {
                        Icon(
                            Icons.Default.MailOutline,
                            contentDescription = "Text"
                        ) // Эта иконка должна работать
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        text = "Изменено только что",
                        style = MaterialTheme.typography.bodyMedium // Обновляем стиль на актуальный
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { /* TODO: действие для меню */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    modifier = Modifier
                        .fillMaxWidth(),
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                    ),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent, // Заменён backgroundColor
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.LightGray,
                        focusedLabelColor = Color.Gray,
                        unfocusedLabelColor = Color.Gray
                    )
                )

                TextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Текст") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                    ),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.Transparent, // Заменён backgroundColor
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.LightGray,
                        focusedLabelColor = Color.Gray,
                        unfocusedLabelColor = Color.Gray
                    )
                )
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        NotesApp(noteDao)
    }
}