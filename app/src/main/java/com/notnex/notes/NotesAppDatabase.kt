package com.notnex.notes

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Note::class], version = 1)
abstract class NotesAppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}