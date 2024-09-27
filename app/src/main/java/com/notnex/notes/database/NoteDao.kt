package com.notnex.notes.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes")
    suspend fun getAllNotes(): List<Note> // Добавь suspend

    @Insert
    suspend fun insert(note: Note) // Добавь suspend

    @Update
    suspend fun update(note: Note) // Добавь suspend

    @Delete
    suspend fun delete(note: Note) // Добавь suspend
}

