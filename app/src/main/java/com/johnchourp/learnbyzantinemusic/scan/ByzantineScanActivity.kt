package com.johnchourp.learnbyzantinemusic.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import com.johnchourp.learnbyzantinemusic.analysis.ByzantineMelodyAnalyzer
import com.johnchourp.learnbyzantinemusic.analysis.MelodyAnalysisRequest
import com.johnchourp.learnbyzantinemusic.analysis.RecognizedNeumeEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class ByzantineScanActivity : BaseActivity() {
    private lateinit var modeSpinner: Spinner
    private lateinit var baseSpinner: Spinner
    private lateinit var takePhotoButton: Button
    private lateinit var galleryButton: Button
    private lateinit var analyzeButton: Button
    private lateinit var rotateLeft90Button: Button
    private lateinit var rotateRight90Button: Button
    private lateinit var resetAdjustmentsButton: Button
    private lateinit var sourceImageView: ImageView
    private lateinit var croppedImageView: ImageView
    private lateinit var analysisStatus: TextView
    private lateinit var notePathText: TextView
    private lateinit var symbolsContainer: LinearLayout
    private lateinit var melodyPathView: MelodyPathView
    private lateinit var rotationSeek: SeekBar
    private lateinit var cropLeftSeek: SeekBar
    private lateinit var cropRightSeek: SeekBar
    private lateinit var cropTopSeek: SeekBar
    private lateinit var cropBottomSeek: SeekBar
    private lateinit var rotationValue: TextView
    private lateinit var cropLeftValue: TextView
    private lateinit var cropRightValue: TextView
    private lateinit var cropTopValue: TextView
    private lateinit var cropBottomValue: TextView

    private val analyzer: ByzantineMelodyAnalyzer by lazy { ByzantineMelodyAnalyzer(this) }

    private var selectedBitmap: Bitmap? = null
    private var originalBitmap: Bitmap? = null

    private val modeOptions by lazy {
        listOf(
            ModeItem("first", getString(R.string.mode_first)),
            ModeItem("second", getString(R.string.mode_second)),
            ModeItem("third", getString(R.string.mode_third)),
            ModeItem("fourth", getString(R.string.mode_fourth)),
            ModeItem("plagal_first", getString(R.string.mode_plagal_first)),
            ModeItem("plagal_second", getString(R.string.mode_plagal_second)),
            ModeItem("varys", getString(R.string.mode_varys)),
            ModeItem("plagal_fourth", getString(R.string.mode_plagal_fourth))
        )
    }

    private val baseOptions by lazy {
        listOf("Νη", "Πα", "Βου", "Γα", "Δι", "Κε", "Ζω")
    }

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchInAppCamera()
            } else {
                showStatus(getString(R.string.byz_scan_camera_permission_denied))
                Toast.makeText(this, R.string.byz_scan_camera_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }

    private val inAppCameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) {
                return@registerForActivityResult
            }
            val uri = InAppCameraActivity.extractUri(result.data) ?: return@registerForActivityResult
            consumeUri(uri)
        }

    private val pickFromGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) {
                return@registerForActivityResult
            }
            consumeUri(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isTemporarilyDisabled()) {
            Toast.makeText(this, R.string.byz_scan_temporarily_disabled, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setContentView(R.layout.layout_byzantine_scan)

        modeSpinner = findViewById(R.id.byz_mode_spinner)
        baseSpinner = findViewById(R.id.byz_base_spinner)
        takePhotoButton = findViewById(R.id.byz_take_photo_btn)
        galleryButton = findViewById(R.id.byz_gallery_btn)
        analyzeButton = findViewById(R.id.byz_analyze_btn)
        rotateLeft90Button = findViewById(R.id.byz_rotate_left_90_btn)
        rotateRight90Button = findViewById(R.id.byz_rotate_right_90_btn)
        resetAdjustmentsButton = findViewById(R.id.byz_reset_adjustments_btn)
        sourceImageView = findViewById(R.id.byz_source_image)
        croppedImageView = findViewById(R.id.byz_cropped_image)
        analysisStatus = findViewById(R.id.byz_analysis_status)
        notePathText = findViewById(R.id.byz_note_path_text)
        symbolsContainer = findViewById(R.id.byz_symbols_container)
        melodyPathView = findViewById(R.id.byz_melody_path_view)
        rotationSeek = findViewById(R.id.byz_rotation_seek)
        cropLeftSeek = findViewById(R.id.byz_crop_left_seek)
        cropRightSeek = findViewById(R.id.byz_crop_right_seek)
        cropTopSeek = findViewById(R.id.byz_crop_top_seek)
        cropBottomSeek = findViewById(R.id.byz_crop_bottom_seek)
        rotationValue = findViewById(R.id.byz_rotation_value)
        cropLeftValue = findViewById(R.id.byz_crop_left_value)
        cropRightValue = findViewById(R.id.byz_crop_right_value)
        cropTopValue = findViewById(R.id.byz_crop_top_value)
        cropBottomValue = findViewById(R.id.byz_crop_bottom_value)

        setupSpinners()
        setupAdjustmentControls()
        setupActions()
    }

    private fun setupSpinners() {
        val modeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modeOptions.map { it.label })
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modeSpinner.adapter = modeAdapter

        val baseAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, baseOptions)
        baseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        baseSpinner.adapter = baseAdapter
        baseSpinner.setSelection(0, false)
    }

    private fun setupAdjustmentControls() {
        val seekListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateAdjustmentLabels()
                if (fromUser) {
                    renderLivePreview(showStatusMessage = true)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        }

        rotationSeek.setOnSeekBarChangeListener(seekListener)
        cropLeftSeek.setOnSeekBarChangeListener(seekListener)
        cropRightSeek.setOnSeekBarChangeListener(seekListener)
        cropTopSeek.setOnSeekBarChangeListener(seekListener)
        cropBottomSeek.setOnSeekBarChangeListener(seekListener)

        resetAdjustments()
    }

    private fun setupActions() {
        takePhotoButton.setOnClickListener { ensureCameraAndCapture() }
        galleryButton.setOnClickListener { pickFromGalleryLauncher.launch("image/*") }
        rotateLeft90Button.setOnClickListener {
            val current = currentRotationDegrees()
            val target = (current - 90).coerceIn(-90, 90)
            rotationSeek.progress = target + ROTATION_SEEK_CENTER
            renderLivePreview(showStatusMessage = true)
        }
        rotateRight90Button.setOnClickListener {
            val current = currentRotationDegrees()
            val target = (current + 90).coerceIn(-90, 90)
            rotationSeek.progress = target + ROTATION_SEEK_CENTER
            renderLivePreview(showStatusMessage = true)
        }
        resetAdjustmentsButton.setOnClickListener {
            resetAdjustments()
            renderLivePreview(showStatusMessage = true)
        }
        analyzeButton.setOnClickListener { runAnalysis() }
    }

    private fun ensureCameraAndCapture() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchInAppCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchInAppCamera() {
        val intent = Intent(this, InAppCameraActivity::class.java)
        inAppCameraLauncher.launch(intent)
    }

    private fun consumeUri(uri: Uri) {
        val bitmap = decodeBitmap(uri)
        if (bitmap == null) {
            showStatus(getString(R.string.byz_scan_load_failed))
            return
        }

        val scaled = downscaleForPreview(bitmap)
        originalBitmap = scaled
        selectedBitmap = scaled
        resetAdjustments()
        renderLivePreview(showStatusMessage = false)

        symbolsContainer.removeAllViews()
        melodyPathView.setNotePath(emptyList())
        notePathText.text = getString(R.string.byz_scan_note_path_placeholder)
        showStatus(getString(R.string.byz_scan_ready_to_analyze))
    }

    private fun runAnalysis() {
        val bitmap = selectedBitmap
        if (bitmap == null) {
            showStatus(getString(R.string.byz_scan_no_image_selected))
            return
        }

        val mode = modeOptions.getOrNull(modeSpinner.selectedItemPosition)?.id ?: "first"
        val base = baseOptions.getOrNull(baseSpinner.selectedItemPosition) ?: baseOptions.first()

        analyzeButton.isEnabled = false
        showStatus(getString(R.string.byz_scan_running))

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    analyzer.analyze(
                        MelodyAnalysisRequest(
                            modeId = mode,
                            basePhthong = base,
                            bitmap = bitmap
                        )
                    )
                }

                croppedImageView.setImageBitmap(result.cropBitmap)
                croppedImageView.visibility = View.VISIBLE
                renderSymbols(result.events)
                notePathText.text = if (result.notePath.isEmpty()) {
                    getString(R.string.byz_scan_no_symbols_found)
                } else {
                    getString(R.string.byz_scan_note_path_template, result.notePath.joinToString(" → "))
                }
                melodyPathView.setNotePath(result.notePath, result.modeHeights)
                showStatus(
                    getString(
                        R.string.byz_scan_result_template,
                        result.events.size,
                        result.unknownCount,
                        result.lowConfidenceCount
                    )
                )
            } catch (_: Throwable) {
                showStatus(getString(R.string.byz_scan_analysis_failed))
                Toast.makeText(this@ByzantineScanActivity, R.string.byz_scan_analysis_failed, Toast.LENGTH_SHORT).show()
            } finally {
                analyzeButton.isEnabled = true
            }
        }
    }

    private fun renderLivePreview(showStatusMessage: Boolean) {
        val original = originalBitmap ?: return
        val rotated = rotateBitmap(original, currentRotationDegrees())
        sourceImageView.setImageBitmap(rotated)
        sourceImageView.visibility = View.VISIBLE

        val cropRect = buildCropRect(rotated.width, rotated.height)
        val cropped = runCatching {
            Bitmap.createBitmap(rotated, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
                .copy(Bitmap.Config.ARGB_8888, false)
        }.getOrNull() ?: return

        selectedBitmap = cropped
        croppedImageView.setImageBitmap(cropped)
        croppedImageView.visibility = View.VISIBLE

        if (showStatusMessage) {
            showStatus(
                getString(
                    R.string.byz_scan_live_preview_template,
                    cropRect.width(),
                    cropRect.height(),
                    currentRotationDegrees()
                )
            )
        }
    }

    private fun rotateBitmap(source: Bitmap, angleDegrees: Int): Bitmap {
        if (angleDegrees == 0) {
            return source
        }
        val matrix = Matrix().apply { postRotate(angleDegrees.toFloat()) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            .copy(Bitmap.Config.ARGB_8888, false)
    }

    private fun buildCropRect(width: Int, height: Int): Rect {
        val normalized = normalizeCropPercentages(
            left = cropLeftSeek.progress,
            right = cropRightSeek.progress,
            top = cropTopSeek.progress,
            bottom = cropBottomSeek.progress
        )

        val leftPx = ((width * normalized.left) / 100f).toInt().coerceIn(0, (width - 1).coerceAtLeast(0))
        val rightPx = ((width * normalized.right) / 100f).toInt().coerceIn(leftPx + 1, width)
        val topPx = ((height * normalized.top) / 100f).toInt().coerceIn(0, (height - 1).coerceAtLeast(0))
        val bottomPx = ((height * normalized.bottom) / 100f).toInt().coerceIn(topPx + 1, height)

        return Rect(leftPx, topPx, rightPx, bottomPx)
    }

    private fun normalizeCropPercentages(left: Int, right: Int, top: Int, bottom: Int): CropPercent {
        var normalizedLeft = left.coerceIn(0, 95)
        var normalizedRight = right.coerceIn(5, 100)
        var normalizedTop = top.coerceIn(0, 95)
        var normalizedBottom = bottom.coerceIn(5, 100)

        if (normalizedRight - normalizedLeft < MIN_CROP_GAP_PERCENT) {
            normalizedRight = (normalizedLeft + MIN_CROP_GAP_PERCENT).coerceAtMost(100)
            normalizedLeft = (normalizedRight - MIN_CROP_GAP_PERCENT).coerceAtLeast(0)
        }
        if (normalizedBottom - normalizedTop < MIN_CROP_GAP_PERCENT) {
            normalizedBottom = (normalizedTop + MIN_CROP_GAP_PERCENT).coerceAtMost(100)
            normalizedTop = (normalizedBottom - MIN_CROP_GAP_PERCENT).coerceAtLeast(0)
        }
        return CropPercent(normalizedLeft, normalizedRight, normalizedTop, normalizedBottom)
    }

    private fun resetAdjustments() {
        rotationSeek.progress = ROTATION_SEEK_CENTER
        cropLeftSeek.progress = 0
        cropRightSeek.progress = 100
        cropTopSeek.progress = 0
        cropBottomSeek.progress = 100
        updateAdjustmentLabels()
    }

    private fun currentRotationDegrees(): Int {
        val degrees = rotationSeek.progress - ROTATION_SEEK_CENTER
        return if (abs(degrees) < 1) 0 else degrees
    }

    private fun updateAdjustmentLabels() {
        val rotation = currentRotationDegrees()
        rotationValue.text = getString(R.string.byz_scan_rotation_template, rotation)
        cropLeftValue.text = getString(R.string.byz_scan_crop_left_template, cropLeftSeek.progress)
        cropRightValue.text = getString(R.string.byz_scan_crop_right_template, cropRightSeek.progress)
        cropTopValue.text = getString(R.string.byz_scan_crop_top_template, cropTopSeek.progress)
        cropBottomValue.text = getString(R.string.byz_scan_crop_bottom_template, cropBottomSeek.progress)
    }

    private fun renderSymbols(symbols: List<RecognizedNeumeEvent>) {
        symbolsContainer.removeAllViews()
        if (symbols.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = getString(R.string.byz_scan_no_symbols_found)
                textSize = 15f
            }
            symbolsContainer.addView(emptyView)
            return
        }

        for (symbol in symbols) {
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                val pad = dp(6)
                setPadding(pad, pad, pad, pad)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val image = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            image.setImageResource(resolveEventDrawable(symbol.baseSymbolId))

            val tokenText = TextView(this).apply {
                text = "${symbol.displayNameEl} (${symbol.baseToken ?: "?"})"
                textSize = 12f
                gravity = Gravity.CENTER
            }

            val noteText = TextView(this).apply {
                text = symbol.noteLabel
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(this@ByzantineScanActivity, R.color.mode_hard_chromatic_blue))
            }

            val confidenceText = TextView(this).apply {
                text = getString(
                    R.string.byz_scan_event_info_template,
                    (symbol.confidence * 100f).toInt(),
                    symbol.durationBeats
                )
                textSize = 11f
                gravity = Gravity.CENTER
            }

            item.addView(image)
            item.addView(tokenText)
            item.addView(noteText)
            item.addView(confidenceText)
            symbolsContainer.addView(item)
        }
    }

    private fun resolveEventDrawable(symbolId: String): Int =
        when (symbolId) {
            "petasti" -> R.drawable.flyer
            "apostrophos" -> R.drawable.apostrophe
            "oligon" -> R.drawable.oligon
            "ison" -> R.drawable.ison
            "slight_apostrophos" -> R.drawable.slight_apostrophe
            "fraction" -> R.drawable.fraction
            "gorgo" -> R.drawable.gorgo
            "antikeno" -> R.drawable.vaccum
            "apli" -> R.drawable.vaccum_simple
            else -> android.R.drawable.ic_menu_help
        }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        return runCatching {
            val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
            ensureSoftwareBitmap(decoded)
        }.getOrNull()
    }

    private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap {
        val isHardware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE
        if (isHardware) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, false)
        }
        return bitmap
    }

    private fun downscaleForPreview(bitmap: Bitmap): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= MAX_PREVIEW_SIDE) {
            return bitmap
        }
        val scale = MAX_PREVIEW_SIDE.toFloat() / maxSide.toFloat()
        val targetW = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            .copy(Bitmap.Config.ARGB_8888, false)
    }

    private fun showStatus(message: String) {
        analysisStatus.text = message
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun isTemporarilyDisabled(): Boolean = true

    private data class ModeItem(
        val id: String,
        val label: String
    )

    private data class CropPercent(
        val left: Int,
        val right: Int,
        val top: Int,
        val bottom: Int
    )

    private companion object {
        const val MIN_CROP_GAP_PERCENT = 5
        const val ROTATION_SEEK_CENTER = 90
        const val MAX_PREVIEW_SIDE = 1800
    }
}
