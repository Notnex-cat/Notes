@file:OptIn(ExperimentalMaterial3Api::class)

package com.notnex.notes

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.res.stringResource
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
            NotesTheme {
                NotesApp(noteDao)
            }
        }
    }

    @Composable
    fun NotesTheme(
        darkTheme: Boolean = isSystemInDarkTheme(), // Определяет, использовать ли тёмную тему
        content: @Composable () -> Unit
    ) {
        val colors = if (darkTheme) {
            darkColorScheme() // Используйте встроенную тёмную палитру
        } else {
            lightColorScheme() // Используйте встроенную светлую палитру
        }

        MaterialTheme(
            colorScheme = colors,
            //typography = Typography,
            content = content
        )
    }

    @Composable
    fun NotesApp(noteDao: NoteDao) {
        //var notes by remember { mutableStateOf(emptyList<Note>()) }
        val navController = rememberNavController()
        var notes by remember { mutableStateOf(listOf<Note>()) }
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
                                    val newNote = Note(title = title, text = text, timestamp = System.currentTimeMillis())
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
                    title = { Text(if (note == null) stringResource(id = R.string.new_note) else stringResource(id = R.string.edit_note)) },
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
                        text = getTimeAgo(note?.timestamp), // Отображаем время создания
                        style = MaterialTheme.typography.bodyMedium
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
                    label = { Text(stringResource(id = R.string.note_name)) },
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
                    label = { Text(stringResource(id = R.string.note_text)) },
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

    fun getTimeAgo(timestamp: Long?): String {
        if (timestamp == null) return ""

        val now = System.currentTimeMillis()
        val difference = now - timestamp

        val seconds = difference / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Только что"
            minutes < 60 -> "$minutes мин. назад"
            hours < 24 -> "$hours ч. назад"
            else -> "$days дн. назад"
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        NotesApp(noteDao)
    }
}