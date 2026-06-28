package com.johnchourp.learnbyzantinemusic.calendar

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.calendar.ui.ReadingTextScreen
import com.johnchourp.learnbyzantinemusic.calendar.ui.ReadingTextUiState
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme
import java.time.LocalDate

/**
 * Host for the redesigned «Κείμενο Αναγνώσματος» screen. Resolves the reading from the launching
 * Intent once, then renders an immutable [ReadingTextUiState] through the Compose [ReadingTextScreen].
 * The Αρχαία / Νεοελληνική toggle is the only mutable state; Ancient is the default mode.
 */
class ReadingTextActivity : BaseActivity() {

    private lateinit var repository: CalendarCelebrationsRepository
    private var readingState: ReadingTextUiState = ReadingTextUiState()
    private var ancientSelected by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = CalendarCelebrationsRepository(this)
        readingState = loadReadingFromIntent()

        setContent {
            LbmTheme {
                ReadingTextScreen(
                    state = readingState,
                    ancientSelected = ancientSelected,
                    onBack = ::finish,
                    onSelectAncient = { ancientSelected = true },
                    onSelectModern = { ancientSelected = false },
                )
            }
        }
    }

    private fun loadReadingFromIntent(): ReadingTextUiState {
        val readingId = intent.getStringExtra(EXTRA_READING_ID)?.trim().orEmpty()
        val rawDate = intent.getStringExtra(EXTRA_DATE)?.trim().orEmpty()
        if (readingId.isEmpty() || rawDate.isEmpty()) {
            return ReadingTextUiState(hasReading = false)
        }

        val date = try {
            LocalDate.parse(rawDate)
        } catch (_: Exception) {
            return ReadingTextUiState(hasReading = false)
        }

        val reading = repository.getReadingById(date, readingId)
            ?: return ReadingTextUiState(hasReading = false)

        return ReadingTextUiState(
            hasReading = true,
            reference = reading.reference,
            textAncient = reading.textAncient,
            textModern = reading.textModern,
        )
    }

    companion object {
        const val EXTRA_READING_ID = "reading_id"
        const val EXTRA_DATE = "reading_date"
    }
}
