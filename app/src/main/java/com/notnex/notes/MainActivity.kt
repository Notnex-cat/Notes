package com.notnex.notes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.notnex.notes.database.Note
import com.notnex.notes.database.NoteDao
import com.notnex.notes.database.NotesAppDatabase
import com.notnex.notes.navigation.Screen
import com.notnex.notes.screens.AuthScreen
import com.notnex.notes.screens.EditNoteScreen
import com.notnex.notes.screens.NoteListScreen
import com.notnex.notes.screens.Settings
import com.notnex.notes.ui.theme.NotesTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var db: NotesAppDatabase
    private lateinit var noteDao: NoteDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firestore
        val fs = Firebase.firestore

        // Initialize Room database
        db = Room.databaseBuilder(
            applicationContext,
            NotesAppDatabase::class.java, "notes-database"
        ).build()
        noteDao = db.noteDao()

        enableEdgeToEdge()
        setContent {

            NotesTheme {

                CheckAndRequestStoragePermission()

                val notes = remember { mutableStateListOf<Note>() }
                val selectedNotes = remember { mutableStateListOf<Note>() }
                var isSelectionMode by remember { mutableStateOf(false) }

                // Load notes from the database
                LaunchedEffect(Unit) {
                    val databaseNotes = noteDao.getAllNotes()
                    notes.clear()
                    notes.addAll(databaseNotes)
                }

                val navController = rememberNavController()
                var currentNote by remember { mutableStateOf<Note?>(null) }

                NavHost(
                    navController = navController,
                    startDestination = Screen.NoteList.route
                ) {
                    composable(Screen.NoteList.route) {
                        NoteListScreen(
                            notes = notes,
                            selectedNotes = selectedNotes,
                            isSelectionMode = isSelectionMode,
                            onToggleSelectionMode = { note ->
                                if (selectedNotes.contains(note)) {
                                    selectedNotes.remove(note)
                                } else {
                                    selectedNotes.add(note)
                                }
                                isSelectionMode = selectedNotes.isNotEmpty()
                            },
                            onExitSelectionMode = {
                                selectedNotes.clear()
                                isSelectionMode = false
                            },
                            onAddNote = {
                                currentNote = null
                                navController.navigate(Screen.EditNote.route)
                            },
                            onNoteClick = { note ->
                                if (isSelectionMode) {
                                    if (selectedNotes.contains(note)) {
                                        selectedNotes.remove(note)
                                    } else {
                                        selectedNotes.add(note)
                                    }
                                    isSelectionMode = selectedNotes.isNotEmpty()
                                } else {
                                    currentNote = note
                                    navController.navigate(Screen.EditNote.route)
                                }
                            },
                            onSettingsClick = {
                                navController.navigate(Screen.Settings.route)
                            },
//                            onLogInClick = {
//                                navController.navigate(Screen.Auth.route)
//                            },
                            deleteNotes = { notesToDelete ->
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        noteDao.delete(notesToDelete)
                                    }
                                    notes.removeAll(notesToDelete)
                                    selectedNotes.clear()
                                    isSelectionMode = false
                                }
                            }
                        )
                    }
                    composable(Screen.EditNote.route) {
                        EditNoteScreen(
                            note = currentNote,
                            onSave = { title, text, imagePath ->
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        if (currentNote == null) {
                                            val newNote = Note(
                                                title = title,
                                                text = text,
                                                timestamp = System.currentTimeMillis(),
                                                imagePath = imagePath
                                            )
                                            noteDao.insert(newNote)
                                            fs.collection("notes")
                                                .document().set(
                                                    Note(
                                                        id = newNote.id,
                                                        title = title,
                                                        text = text,
                                                        timestamp = System.currentTimeMillis(),
                                                        imagePath = imagePath
                                                    )
                                                )
                                        } else {
                                            val updatedNote = currentNote!!.copy(
                                                title = title,
                                                text = text,
                                                imagePath = imagePath
                                            )
                                            noteDao.update(updatedNote)
                                            fs.collection("notes")
                                                .document().set(
                                                    Note(
                                                        id = updatedNote.id,
                                                        title = title,
                                                        text = text,
                                                        timestamp = System.currentTimeMillis(),
                                                        imagePath = imagePath
                                                    )
                                                )
                                        }
                                    }
                                    val databaseNotes = noteDao.getAllNotes()
                                    notes.clear()
                                    notes.addAll(databaseNotes)
                                    navController.navigateUp()
                                }
                            },
                            onBack = { navController.navigateUp() }
                        )
                    }
                    composable(Screen.Settings.route) {
                        Settings(
                            onLogInClick = {
                            navController.navigate(Screen.Auth.route)
                        },
                            onBack = {
                            navController.navigateUp()
                        })
                    }
                    composable(Screen.Auth.route) {
                        AuthScreen(
                            onMainScreen = {
                                navController.navigate(Screen.NoteList.route)

                            }
                        )
                    }
                }
            }
        }


    }

    @Composable
    fun CheckAndRequestStoragePermission() {
        val context = LocalContext.current
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val permissionState = remember(permission) {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            )
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            permissionState.value = isGranted
        }

        LaunchedEffect(Unit) {
            if (!permissionState.value) {
                launcher.launch(permission)
            }
        }
    }
}