package com.johnchourp.learnbyzantinemusic.recordings.index

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecordingIndexDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<RecordingIndexEntity>)

    @Query("DELETE FROM recordings_index WHERE rootUri = :rootUri")
    suspend fun deleteByRootUri(rootUri: String)

    @Query("DELETE FROM recordings_index WHERE docUri = :docUri")
    suspend fun deleteByDocUri(docUri: String)

    @Query(
        """
        SELECT *
        FROM recordings_index
        WHERE rootUri = :rootUri
          AND parentUri = :parentUri
          AND nameNormalized LIKE :searchPattern
          AND (
            :typeFilter = 0 OR
            (:typeFilter = 1 AND isDirectory = 1) OR
            (:typeFilter = 2 AND isAudio = 1)
          )
        ORDER BY isDirectory DESC, nameNormalized COLLATE NOCASE ASC, relativePath COLLATE NOCASE ASC
        """
    )
    fun managerByName(
        rootUri: String,
        parentUri: String,
        searchPattern: String,
        typeFilter: Int
    ): PagingSource<Int, RecordingIndexEntity>

    @Query(
        """
        SELECT *
        FROM recordings_index
        WHERE rootUri = :rootUri
          AND parentUri = :parentUri
          AND nameNormalized LIKE :searchPattern
          AND (
            :typeFilter = 0 OR
            (:typeFilter = 1 AND isDirectory = 1) OR
            (:typeFilter = 2 AND isAudio = 1)
          )
        ORDER BY isDirectory DESC, createdTs DESC, nameNormalized COLLATE NOCASE ASC
        """
    )
    fun managerByCreated(
        rootUri: String,
        parentUri: String,
        searchPattern: String,
        typeFilter: Int
    ): PagingSource<Int, RecordingIndexEntity>

    @Query(
        """
        SELECT *
        FROM recordings_index
        WHERE rootUri = :rootUri
          AND parentUri = :parentUri
          AND nameNormalized LIKE :searchPattern
          AND (
            :typeFilter = 0 OR
            (:typeFilter = 1 AND isDirectory = 1) OR
            (:typeFilter = 2 AND isAudio = 1)
          )
        ORDER BY isDirectory DESC, updatedTs DESC, nameNormalized COLLATE NOCASE ASC
        """
    )
    fun managerByUpdated(
        rootUri: String,
        parentUri: String,
        searchPattern: String,
        typeFilter: Int
    ): PagingSource<Int, RecordingIndexEntity>

    @Query(
        """
        SELECT *
        FROM recordings_index
        WHERE rootUri = :rootUri
          AND isAudio = 1
          AND nameNormalized LIKE :searchPattern
        ORDER BY nameNormalized COLLATE NOCASE ASC, relativePath COLLATE NOCASE ASC
        """
    )
    fun recentByName(
        rootUri: String,
        searchPattern: String
    ): PagingSource<Int, RecordingIndexEntity>

    @Query(
        """
        SELECT *
        FROM recordings_index
        WHERE rootUri = :rootUri
          AND isAudio = 1
          AND nameNormalized LIKE :searchPattern
        ORDER BY createdTs DESC, nameNormalized COLLATE NOCASE ASC
        """
    )
    fun recentByCreated(
        rootUri: String,
        searchPattern: String
    ): PagingSource<Int, RecordingIndexEntity>

    @Query(
        """
        SELECT *
        FROM recordings_index
        WHERE rootUri = :rootUri
          AND isAudio = 1
          AND nameNormalized LIKE :searchPattern
        ORDER BY updatedTs DESC, nameNormalized COLLATE NOCASE ASC
        """
    )
    fun recentByUpdated(
        rootUri: String,
        searchPattern: String
    ): PagingSource<Int, RecordingIndexEntity>

    @Query(
        """
        SELECT docUri, relativePath, name
        FROM recordings_index
        WHERE rootUri = :rootUri
          AND isDirectory = 1
          AND nameNormalized LIKE :searchPattern
        ORDER BY relativePath COLLATE NOCASE ASC
        """
    )
    suspend fun findMoveTargets(
        rootUri: String,
        searchPattern: String
    ): List<MoveTargetProjection>

    @Query(
        """
        SELECT *
        FROM recordings_index
        WHERE rootUri = :rootUri
          AND isDirectory = 1
          AND relativePath = :relativePath
        LIMIT 1
        """
    )
    suspend fun findDirectoryByRelativePath(
        rootUri: String,
        relativePath: String
    ): RecordingIndexEntity?

    @Query(
        """
        SELECT COUNT(*)
        FROM recordings_index
        WHERE rootUri = :rootUri
        """
    )
    suspend fun countByRootUri(rootUri: String): Int
}

data class MoveTargetProjection(
    val docUri: String,
    val relativePath: String,
    val name: String
)
