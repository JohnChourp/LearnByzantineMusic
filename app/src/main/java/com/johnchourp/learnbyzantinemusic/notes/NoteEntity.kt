package com.johnchourp.learnbyzantinemusic.notes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["updatedAtEpochMs"]),
        Index(value = ["createdAtEpochMs"])
    ]
)
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val body: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)
