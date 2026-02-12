package com.johnchourp.learnbyzantinemusic.editor.storage

import android.content.Context
import com.johnchourp.learnbyzantinemusic.editor.model.CompositionLine
import com.johnchourp.learnbyzantinemusic.editor.model.CompositionProject
import com.johnchourp.learnbyzantinemusic.editor.model.SymbolPlacement
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ProjectRepository(private val context: Context) {
    private val projectsDir: File
        get() = File(context.filesDir, PROJECTS_DIR).apply { mkdirs() }

    fun saveProject(project: CompositionProject): File {
        val fileName = sanitizeFileName(project.title).ifBlank { "project" } + ".json"
        val file = File(projectsDir, fileName)
        file.writeText(projectToJson(project).toString(2))
        return file
    }

    fun listProjects(): List<File> {
        return projectsDir.listFiles { file -> file.extension.lowercase() == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun loadProject(file: File): CompositionProject {
        return jsonToProject(JSONObject(file.readText()))
    }

    private fun projectToJson(project: CompositionProject): JSONObject {
        return JSONObject().apply {
            put("version", project.version)
            put("title", project.title)
            put("updatedAt", project.updatedAt)
            put("lines", JSONArray().apply {
                project.lines.forEach { line ->
                    put(JSONObject().apply {
                        put("id", line.id)
                        put("lyrics", line.lyrics)
                        put("symbols", JSONArray().apply {
                            line.symbols.forEach { symbol ->
                                put(JSONObject().apply {
                                    put("id", symbol.id)
                                    put("symbolKey", symbol.symbolKey)
                                    put("symbolText", symbol.symbolText)
                                    put("symbolFontId", symbol.symbolFontId)
                                    put("charIndex", symbol.charIndex)
                                    put("dxDp", symbol.dxDp.toDouble())
                                    put("dyDp", symbol.dyDp.toDouble())
                                    put("scale", symbol.scale.toDouble())
                                })
                            }
                        })
                    })
                }
            })
        }
    }

    private fun jsonToProject(json: JSONObject): CompositionProject {
        val linesArray = json.optJSONArray("lines") ?: JSONArray()
        val lines = buildList {
            for (lineIndex in 0 until linesArray.length()) {
                val lineObject = linesArray.getJSONObject(lineIndex)
                val symbolsArray = lineObject.optJSONArray("symbols") ?: JSONArray()
                val symbols = buildList {
                    for (symbolIndex in 0 until symbolsArray.length()) {
                        val symbolObject = symbolsArray.getJSONObject(symbolIndex)
                        add(
                            SymbolPlacement(
                                id = symbolObject.getString("id"),
                                symbolKey = symbolObject.getString("symbolKey"),
                                symbolText = symbolObject.getString("symbolText"),
                                symbolFontId = symbolObject.optString("symbolFontId", "mk_byzantine"),
                                charIndex = symbolObject.getInt("charIndex"),
                                dxDp = symbolObject.optDouble("dxDp", 0.0).toFloat(),
                                dyDp = symbolObject.optDouble("dyDp", 0.0).toFloat(),
                                scale = symbolObject.optDouble("scale", 1.0).toFloat()
                            )
                        )
                    }
                }
                add(
                    CompositionLine(
                        id = lineObject.getString("id"),
                        lyrics = lineObject.optString("lyrics", ""),
                        symbols = symbols
                    )
                )
            }
        }

        return CompositionProject(
            version = json.optInt("version", 1),
            title = json.optString("title", "Σύνθεση"),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
            lines = lines
        )
    }

    private fun sanitizeFileName(input: String): String {
        return input.trim().lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_\u0370-\u03FF\u1F00-\u1FFF-]"), "")
            .take(60)
    }

    private companion object {
        const val PROJECTS_DIR = "composer_projects"
    }
}
