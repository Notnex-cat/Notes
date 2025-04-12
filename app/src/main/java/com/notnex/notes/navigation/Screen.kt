package com.notnex.notes.navigation

sealed class Screen(val route: String) {
    data object NoteList : Screen("notes_list")
    data object EditNote : Screen("edit_note")
    data object Settings : Screen("notes_settings")
    data object Auth : Screen("log_in")
}