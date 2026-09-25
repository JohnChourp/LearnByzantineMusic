package com.johnchourp.learnbyzantinemusic.notes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotesDao {
    // Every note: the search box filters in memory (NotesSearch), since LIKE folds case for ASCII only.
    @Query(
        """
        SELECT *
        FROM notes
        ORDER BY updatedAtEpochMs DESC, createdAtEpochMs DESC
        """
    )
    fun observeAll(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT *
        FROM notes
        ORDER BY updatedAtEpochMs DESC, createdAtEpochMs DESC
        """
    )
    suspend fun getAllForSnapshot(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM notes")
    suspend fun deleteAll()
}
