package com.johnchourp.learnbyzantinemusic.recordings.index

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * `recordings_index.db`: an index of what is in the user's recordings folder — a cache, not user data.
 *
 * **Migration policy: destructive is acceptable, because the index is rebuildable.** Every row is
 * derived from the folder, and an empty index is filled again the next time «Διαχείριση
 * ηχογραφήσεων» opens (`RecordingsManagerViewModel.requestReindex`). So a schema change may raise the
 * version without a `Migration`, and Room drops and recreates the tables — that is what
 * `fallbackToDestructiveMigration` below allows. It changes nothing while the version is 1. The schema
 * is still exported to `app/schemas/<this class>/`, so every change to it is committed and reviewed.
 *
 * Not in Android Auto Backup: its folder URIs would mean nothing on a device without the folder
 * permission, which is never backed up or transferred (see the backup rules in `res/xml`).
 */
@Database(
    entities = [RecordingIndexEntity::class],
    version = 1,
    exportSchema = true
)
abstract class RecordingsDatabase : RoomDatabase() {
    abstract fun recordingIndexDao(): RecordingIndexDao

    companion object {
        @Volatile
        private var instance: RecordingsDatabase? = null

        fun getInstance(context: Context): RecordingsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RecordingsDatabase::class.java,
                    "recordings_index.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
