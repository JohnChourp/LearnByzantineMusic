package com.johnchourp.learnbyzantinemusic.calendar

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import java.time.LocalDate

class ReadingTextActivity : BaseActivity() {
    private lateinit var readingReferenceText: TextView
    private lateinit var readingContentText: TextView
    private lateinit var ancientButton: Button
    private lateinit var modernButton: Button
    private lateinit var repository: CalendarCelebrationsRepository

    private var reading: CalendarReadingText? = null
    private var currentMode: ReadingTextMode = ReadingTextMode.ANCIENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_reading_text)

        readingReferenceText = findViewById(R.id.reading_text_reference)
        readingContentText = findViewById(R.id.reading_text_content)
        ancientButton = findViewById(R.id.reading_text_mode_ancient_button)
        modernButton = findViewById(R.id.reading_text_mode_modern_button)
        repository = CalendarCelebrationsRepository(this)

        reading = loadReadingFromIntent()

        ancientButton.setOnClickListener {
            currentMode = ReadingTextMode.ANCIENT
            renderReadingContent()
        }
        modernButton.setOnClickListener {
            currentMode = ReadingTextMode.MODERN
            renderReadingContent()
        }

        renderReadingHeader()
        renderReadingContent()
    }

    private fun loadReadingFromIntent(): CalendarReadingText? {
        val readingId = intent.getStringExtra(EXTRA_READING_ID)?.trim().orEmpty()
        val rawDate = intent.getStringExtra(EXTRA_DATE)?.trim().orEmpty()
        if (readingId.isEmpty() || rawDate.isEmpty()) {
            return null
        }

        val date = try {
            LocalDate.parse(rawDate)
        } catch (_: Exception) {
            return null
        }

        return repository.getReadingById(date, readingId)
    }

    private fun renderReadingHeader() {
        val currentReading = reading
        if (currentReading == null) {
            readingReferenceText.text = getString(R.string.reading_text_missing_reference)
            return
        }
        readingReferenceText.text = currentReading.reference
    }

    private fun renderReadingContent() {
        val currentReading = reading
        if (currentReading == null) {
            readingContentText.text = getString(R.string.reading_text_missing_content)
            updateModeButtons()
            return
        }

        readingContentText.text = when (currentMode) {
            ReadingTextMode.ANCIENT -> currentReading.textAncient
            ReadingTextMode.MODERN -> currentReading.textModern
        }
        updateModeButtons()
    }

    private fun updateModeButtons() {
        val isAncient = currentMode == ReadingTextMode.ANCIENT
        ancientButton.isEnabled = !isAncient
        modernButton.isEnabled = isAncient
        ancientButton.alpha = if (isAncient) 0.65f else 1f
        modernButton.alpha = if (isAncient) 1f else 0.65f
    }

    private enum class ReadingTextMode {
        ANCIENT,
        MODERN,
    }

    companion object {
        const val EXTRA_READING_ID = "reading_id"
        const val EXTRA_DATE = "reading_date"
    }
}
