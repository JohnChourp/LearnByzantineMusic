package com.johnchourp.learnbyzantinemusic

import com.johnchourp.learnbyzantinemusic.docs.KotlinSource
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The Room schemas are committed, and nobody changes one without a new version (ClickUp `869f5x2a2`).
 *
 * Room exports each database's schema to `app/schemas/<class>/<version>.json` while kapt runs, which is
 * before the unit tests. So an entity changed **without** raising the version rewrites the committed
 * file for the current version, and its `identityHash` stops matching the one pinned here — this test
 * turns red instead of the change shipping blind. On users' devices Room would refuse to open such a
 * database at all ("changed schema but forgot to update the version number").
 *
 * Raising the version is the legitimate path, and it is deliberate here too: every version from 1 to
 * the current one needs its committed file **and** its pinned hash, so the new schema file cannot be
 * forgotten.
 *
 * The migration policy of each database is in its KDoc: the notes are migrated explicitly and never
 * destructively; the recordings index is a rebuildable cache and may be dropped.
 */
class DatabaseSchemaGuardTest {

    private class Db(
        val source: String,
        val className: String,
        /** identityHash of every committed version, from app/schemas/<className>/<version>.json. */
        val pinned: Map<Int, String>,
        val mayFallBackToDestructiveMigration: Boolean,
    )

    private val databases = listOf(
        Db(
            source = "com/johnchourp/learnbyzantinemusic/notes/NotesDatabase.kt",
            className = "com.johnchourp.learnbyzantinemusic.notes.NotesDatabase",
            pinned = mapOf(1 to "5b37afe8d3c243e2c42d75c9ae0fce14"),
            mayFallBackToDestructiveMigration = false,
        ),
        Db(
            source = "com/johnchourp/learnbyzantinemusic/recordings/index/RecordingsDatabase.kt",
            className = "com.johnchourp.learnbyzantinemusic.recordings.index.RecordingsDatabase",
            pinned = mapOf(1 to "127925a6521a3e41cb70f5c4cac46d22"),
            mayFallBackToDestructiveMigration = true,
        ),
    )

    private val schemasDir: File by lazy {
        listOf(File("app/schemas"), File("schemas")).firstOrNull { it.isDirectory }
            ?: error("Cannot locate app/schemas from ${File("").absolutePath}")
    }

    private fun code(db: Db): String = KotlinSource.withoutComments(File(KotlinSource.mainRoot, db.source).readText())

    /** The `@Database(...)` arguments of [db], read from its source. */
    private fun annotation(db: Db): String =
        Regex("""@Database\s*\(([^)]*)\)""").find(code(db))?.groupValues?.get(1)
            ?: error("no @Database annotation in ${db.source}")

    private fun currentVersion(db: Db): Int =
        Regex("""\bversion\s*=\s*(\d+)""").find(annotation(db))?.groupValues?.get(1)?.toInt()
            ?: error("no version in the @Database of ${db.source}")

    @Test
    fun everyDatabaseExportsItsSchema() {
        databases.forEach { db ->
            assertTrue("${db.source} must export its schema", Regex("""\bexportSchema\s*=\s*true\b""").containsMatchIn(annotation(db)))
        }
    }

    @Test
    fun everyVersionUpToTheCurrentOneIsPinned() {
        databases.forEach { db ->
            val version = currentVersion(db)
            assertEquals(
                "${db.className} is at version $version: pin the identityHash of every version from 1 to it " +
                    "(each from its committed app/schemas file)",
                (1..version).toSet(),
                db.pinned.keys
            )
        }
    }

    @Test
    fun everyCommittedSchemaMatchesItsPinnedIdentity() {
        databases.forEach { db ->
            val dir = File(schemasDir, db.className)
            val files = dir.listFiles { file -> file.extension == "json" }.orEmpty().associateBy { it.nameWithoutExtension.toInt() }
            assertEquals("${db.className}: one committed schema per version", db.pinned.keys, files.keys)
            db.pinned.forEach { (version, identityHash) ->
                val database = JSONObject(files.getValue(version).readText()).getJSONObject("database")
                assertEquals("${db.className} $version.json", version, database.getInt("version"))
                assertEquals(
                    "${db.className} version $version changed without a new version: raise it and write the migration",
                    identityHash,
                    database.getString("identityHash")
                )
            }
        }
    }

    @Test
    fun onlyTheRebuildableIndexMayFallBackToDestructiveMigration() {
        // The notes are the user's own writing: a missing migration must fail loudly, never start over.
        val destructive = Regex("""\bfallbackToDestructiveMigration\w*\s*\(""")
        databases.forEach { db ->
            assertEquals(
                "${db.source} and fallbackToDestructiveMigration",
                db.mayFallBackToDestructiveMigration,
                destructive.containsMatchIn(code(db))
            )
        }
        val elsewhere = KotlinSource.mainRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { destructive.containsMatchIn(KotlinSource.withoutComments(it.readText())) }
            .map { it.name }
            .toList()
        assertEquals("only the recordings index is ever built destructively", listOf("RecordingsDatabase.kt"), elsewhere)
    }
}
