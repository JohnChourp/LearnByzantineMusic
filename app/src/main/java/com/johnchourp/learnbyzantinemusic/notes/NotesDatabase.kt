package com.johnchourp.learnbyzantinemusic.notes

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun notesDao(): NotesDao

    companion object {
        @Volatile
        private var instance: NotesDatabase? = null

        fun getInstance(context: Context): NotesDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NotesDatabase::class.java,
                    "notes.db"
                ).build().also { instance = it }
            }
        }
    }
}
