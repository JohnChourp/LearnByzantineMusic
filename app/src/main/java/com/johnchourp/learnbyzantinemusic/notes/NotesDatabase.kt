package com.johnchourp.learnbyzantinemusic.notes

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * `notes.db`: what the user wrote in «Σημειώσεις».
 *
 * **The primary copy, not the only one.** Every save also writes a full JSON snapshot into the folder
 * the user picked ([NotesBackupManager]), and Android Auto Backup keeps this file (the backup rules in
 * `res/xml` exclude it nowhere). What is in here is still the user's own writing, so it is never
 * thrown away to make an upgrade easier.
 *
 * **Migration policy: explicit migrations only — never destructive.** A schema change raises the
 * version and ships a `Migration` from every earlier version, written against the committed schema in
 * `app/schemas/<this class>/<version>.json`. There is no `fallbackToDestructiveMigration` here, not
 * even on downgrade: a missing migration must fail loudly (Room throws when it opens the database)
 * rather than hand the user an empty notebook. `DatabaseSchemaGuardTest` pins the committed schemas and
 * fails if this file ever allows a destructive fallback.
 *
 * The backup file's own format has a separate version ([NotesBackupCodec.CURRENT_SCHEMA_VERSION]).
 */
@Database(
    entities = [NoteEntity::class],
    version = 1,
    exportSchema = true
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
