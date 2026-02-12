package com.johnchourp.learnbyzantinemusic.editor

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.editor.export.PngExporter
import com.johnchourp.learnbyzantinemusic.editor.model.SymbolDefinition
import com.johnchourp.learnbyzantinemusic.editor.palette.SymbolPaletteAdapter
import com.johnchourp.learnbyzantinemusic.editor.storage.ProjectRepository
import com.johnchourp.learnbyzantinemusic.editor.view.LineEditorView
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ComposerActivity : ComponentActivity(), LineEditorView.Listener {
    private val viewModel = ComposerViewModel()

    private lateinit var repository: ProjectRepository
    private lateinit var exporter: PngExporter

    private lateinit var titleInput: EditText
    private lateinit var linesContainer: LinearLayout
    private lateinit var sheetContainer: LinearLayout
    private lateinit var selectionInfo: TextView

    private lateinit var symbolsRecycler: RecyclerView
    private lateinit var symbolAdapter: SymbolPaletteAdapter
    private var symbolTypefaces: Map<String, Typeface> = emptyMap()
    private var symbols: List<SymbolDefinition> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_composer)

        repository = ProjectRepository(this)
        exporter = PngExporter(this)

        titleInput = findViewById(R.id.composer_project_title)
        linesContainer = findViewById(R.id.composer_lines_container)
        sheetContainer = findViewById(R.id.composer_sheet_container)
        selectionInfo = findViewById(R.id.composer_selection_info)
        symbolsRecycler = findViewById(R.id.composer_symbols_recycler)

        symbolTypefaces = loadLocalTypefaces()
        symbols = loadSymbols()

        symbolAdapter = SymbolPaletteAdapter(symbols, symbolTypefaces) { symbol ->
            viewModel.insertSymbol(symbol)
            renderLines()
            updateSelectionInfo()
        }

        symbolsRecycler.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        symbolsRecycler.adapter = symbolAdapter

        titleInput.setText(viewModel.project.title)
        titleInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.setTitle(titleInput.text.toString().trim())
            }
        }

        findViewById<Button>(R.id.composer_new_btn).setOnClickListener {
            viewModel.resetProject()
            titleInput.setText(viewModel.project.title)
            renderLines()
            updateSelectionInfo()
        }

        findViewById<Button>(R.id.composer_add_line_btn).setOnClickListener {
            viewModel.addEmptyLine()
            renderLines()
            updateSelectionInfo()
        }

        findViewById<Button>(R.id.composer_save_btn).setOnClickListener {
            saveCurrentProject()
        }

        findViewById<Button>(R.id.composer_open_btn).setOnClickListener {
            openProjectDialog()
        }

        findViewById<Button>(R.id.composer_export_btn).setOnClickListener {
            exportPng()
        }

        findViewById<Button>(R.id.composer_images_btn).setOnClickListener {
            startActivity(Intent(this, ComposerImagesActivity::class.java))
        }

        findViewById<Button>(R.id.composer_undo_btn).setOnClickListener {
            val changed = viewModel.undo()
            if (!changed) {
                toast(getString(R.string.composer_nothing_to_undo))
            }
            renderLines()
            updateSelectionInfo()
        }

        findViewById<Button>(R.id.composer_redo_btn).setOnClickListener {
            val changed = viewModel.redo()
            if (!changed) {
                toast(getString(R.string.composer_nothing_to_redo))
            }
            renderLines()
            updateSelectionInfo()
        }

        findViewById<Button>(R.id.composer_delete_symbol_btn).setOnClickListener {
            viewModel.removeSelectedSymbol()
            renderLines()
            updateSelectionInfo()
        }

        bindNudgeButtons()
        renderLines()
        updateSelectionInfo()
    }

    override fun onLineSelected(lineId: String, cursorIndex: Int) {
        viewModel.setSelectedLine(lineId, cursorIndex)
        updateSelectionInfo()
    }

    override fun onLyricsChanged(lineId: String, lyrics: String, commitHistory: Boolean) {
        viewModel.updateLyrics(lineId, lyrics, addHistory = true)
    }

    override fun onSymbolSelected(lineId: String, symbolId: String?) {
        viewModel.selectSymbol(lineId, symbolId)
        updateSelectionInfo()
    }

    override fun onSymbolMoveCommitted(lineId: String, symbolId: String, dxDp: Float, dyDp: Float) {
        viewModel.moveSymbol(lineId, symbolId, dxDp, dyDp)
        renderLines()
        updateSelectionInfo()
    }

    private fun bindNudgeButtons() {
        findViewById<Button>(R.id.composer_nudge_left_1).setOnClickListener { nudge(-1f, 0f) }
        findViewById<Button>(R.id.composer_nudge_right_1).setOnClickListener { nudge(1f, 0f) }
        findViewById<Button>(R.id.composer_nudge_up_1).setOnClickListener { nudge(0f, -1f) }
        findViewById<Button>(R.id.composer_nudge_down_1).setOnClickListener { nudge(0f, 1f) }
        findViewById<Button>(R.id.composer_nudge_left_4).setOnClickListener { nudge(-4f, 0f) }
        findViewById<Button>(R.id.composer_nudge_right_4).setOnClickListener { nudge(4f, 0f) }
        findViewById<Button>(R.id.composer_nudge_up_4).setOnClickListener { nudge(0f, -4f) }
        findViewById<Button>(R.id.composer_nudge_down_4).setOnClickListener { nudge(0f, 4f) }
    }

    private fun nudge(dxDp: Float, dyDp: Float) {
        val before = viewModel.selectedSymbolId
        viewModel.nudgeSelectedSymbol(dxDp, dyDp)
        if (before == null) {
            toast(getString(R.string.composer_select_symbol_first))
            return
        }
        renderLines()
        updateSelectionInfo()
    }

    private fun renderLines() {
        linesContainer.removeAllViews()

        viewModel.project.lines.forEachIndexed { index, line ->
            val lineView = LineEditorView(this)
            lineView.setSymbolTypefaces(symbolTypefaces)
            lineView.setListener(this)
            val selectedSymbol = if (line.id == viewModel.selectedLineId) {
                viewModel.selectedSymbolId
            } else {
                null
            }
            lineView.bind(line, selectedSymbol)

            val layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.topMargin = if (index == 0) 0 else dp(6f)
            linesContainer.addView(lineView, layoutParams)
        }
    }

    private fun loadSymbols(): List<SymbolDefinition> {
        return try {
            val json = assets.open(LOCAL_SYMBOLS_ASSET).bufferedReader().use { it.readText() }
            parseSymbolsJson(json)
        } catch (_: Exception) {
            toast(getString(R.string.composer_local_symbols_missing))
            toast(getString(R.string.composer_run_local_assets_script))
            fallbackSymbols()
        }
    }

    private fun fallbackSymbols(): List<SymbolDefinition> {
        return try {
            val json = assets.open(LEGACY_SYMBOLS_ASSET).bufferedReader().use { it.readText() }
            parseSymbolsJson(json)
        } catch (_: Exception) {
            toast(getString(R.string.composer_symbols_load_error))
            emptyList()
        }
    }

    private fun parseSymbolsJson(json: String): List<SymbolDefinition> {
        val array = JSONArray(json)
        val parsed = buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SymbolDefinition(
                        key = item.getString("key"),
                        label = item.optString("label", item.getString("key")),
                        text = item.getString("text"),
                        fontId = item.optString("fontId", DEFAULT_FONT_ID),
                        category = item.optString("category", "mk"),
                        defaultDyDp = item.optDouble("defaultDyDp", -24.0).toFloat()
                    )
                )
            }
        }
        if (parsed.isEmpty()) {
            toast(getString(R.string.composer_local_symbols_incomplete))
        }
        return parsed
    }

    private fun loadLocalTypefaces(): Map<String, Typeface> {
        val loaded = mutableMapOf<String, Typeface>()
        val missingFiles = mutableListOf<String>()

        REQUIRED_FONTS.forEach { (fontId, fileName) ->
            runCatching {
                Typeface.createFromAsset(assets, "$LOCAL_FONTS_DIR/$fileName")
            }.onSuccess { typeface ->
                loaded[fontId] = typeface
            }.onFailure {
                missingFiles += fileName
            }
        }

        if (loaded.isEmpty()) {
            toast(getString(R.string.composer_local_fonts_missing))
            toast(getString(R.string.composer_run_local_assets_script))
            return mapOf(DEFAULT_FONT_ID to Typeface.DEFAULT)
        }

        val defaultTypeface = loaded[DEFAULT_FONT_ID] ?: Typeface.DEFAULT
        if (!loaded.containsKey(DEFAULT_FONT_ID)) {
            loaded[DEFAULT_FONT_ID] = defaultTypeface
        }
        REQUIRED_FONTS.keys.forEach { fontId ->
            if (!loaded.containsKey(fontId)) {
                loaded[fontId] = defaultTypeface
            }
        }

        if (missingFiles.isNotEmpty()) {
            toast(getString(R.string.composer_local_fonts_partial_missing, missingFiles.joinToString(", ")))
            toast(getString(R.string.composer_run_local_assets_script))
        }

        return loaded
    }

    private fun saveCurrentProject() {
        viewModel.setTitle(titleInput.text.toString().trim())
        val file = repository.saveProject(viewModel.project)
        toast(getString(R.string.composer_saved_to, file.name))
    }

    private fun openProjectDialog() {
        val projects = repository.listProjects()
        if (projects.isEmpty()) {
            toast(getString(R.string.composer_no_saved_projects))
            return
        }

        val labels = projects.map { file ->
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(Date(file.lastModified()))
            "${file.nameWithoutExtension} ($date)"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.composer_open)
            .setItems(labels) { _, which ->
                loadProjectFile(projects[which])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun loadProjectFile(file: File) {
        try {
            val project = repository.loadProject(file)
            viewModel.loadProject(project)
            titleInput.setText(project.title)
            renderLines()
            updateSelectionInfo()
            toast(getString(R.string.composer_loaded_from, file.name))
        } catch (exception: Exception) {
            toast(getString(R.string.composer_load_error))
        }
    }

    private fun exportPng() {
        measureForExport(sheetContainer)
        val file = exporter.export(sheetContainer)
        toast(getString(R.string.composer_exported_to, file.absolutePath))
    }

    private fun measureForExport(view: View) {
        val parentWidth = view.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels - dp(24f)
        val widthSpec = View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }

    private fun updateSelectionInfo() {
        val lineIndex = viewModel.project.lines.indexOfFirst { it.id == viewModel.selectedLineId }
        val lineDisplay = if (lineIndex >= 0) {
            (lineIndex + 1).toString()
        } else {
            "-"
        }
        val symbol = viewModel.selectedSymbolId ?: "-"
        val cursor = viewModel.selectedCursorIndex

        selectionInfo.text = getString(
            R.string.composer_selection_info,
            lineDisplay,
            cursor.toString(),
            symbol
        )
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dp(value: Float): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private companion object {
        const val LOCAL_SYMBOLS_ASSET = "editor_symbols_local_v1.json"
        const val LEGACY_SYMBOLS_ASSET = "editor_symbols_v1.json"
        const val LOCAL_FONTS_DIR = "local_fonts"
        const val DEFAULT_FONT_ID = "mk_byzantine"
        val REQUIRED_FONTS = linkedMapOf(
            "mk_byzantine" to "mk_byzantine.ttf",
            "mk_ison" to "mk_ison.ttf",
            "mk_fthores" to "mk_fthores.ttf",
            "mk_loipa" to "mk_loipa.ttf",
            "mk_xronos" to "mk_xronos.ttf"
        )
    }
}
