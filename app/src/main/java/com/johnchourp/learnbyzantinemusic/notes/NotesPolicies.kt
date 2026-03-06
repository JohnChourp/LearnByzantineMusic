package com.johnchourp.learnbyzantinemusic.notes

fun sortNotesByMostRecent(notes: List<NoteEntity>): List<NoteEntity> {
    return notes.sortedWith(
        compareByDescending<NoteEntity> { it.updatedAtEpochMs }
            .thenByDescending { it.createdAtEpochMs }
            .thenBy { it.id }
    )
}

fun replaceAllNotesWithImport(importedNotes: List<NoteEntity>): List<NoteEntity> {
    return sortNotesByMostRecent(importedNotes)
}

object NotesPendingSyncTracker {
    fun enqueue(existingFileNames: List<String>, newFileName: String): List<String> {
        if (newFileName.isBlank()) {
            return existingFileNames
        }
        return (existingFileNames + newFileName).distinct()
    }

    fun dequeue(existingFileNames: List<String>, syncedFileNames: Set<String>): List<String> {
        if (syncedFileNames.isEmpty()) {
            return existingFileNames
        }
        return existingFileNames.filterNot { it in syncedFileNames }
    }
}
