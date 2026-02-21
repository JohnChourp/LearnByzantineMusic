package com.johnchourp.learnbyzantinemusic.recordings.index

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RecordingIndexEntity::class],
    version = 1,
    exportSchema = false
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
                ).build().also { instance = it }
            }
        }
    }
}
