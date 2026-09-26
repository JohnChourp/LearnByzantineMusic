package com.johnchourp.learnbyzantinemusic.lectern

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.lectern.ui.LecternReaderActions
import com.johnchourp.learnbyzantinemusic.lectern.ui.LecternReaderScreen
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmTheme

/**
 * The digital lectern's reader (ClickUp `869f5x2e7`): one of the user's PDFs, page by page, with the
 * ison/απήχημα bar that remembers the ήχος of each page. Everything that must outlive a rotation — the
 * open PDF, the page, the map, the sound — is [LecternReaderViewModel]'s; this activity is the window.
 *
 * **The window.** The only screen of the app that turns to landscape ([screenOrientation]: any way the
 * user holds it, unless they locked rotation). The screen stays on while it is in front — a lectern
 * must not go dark mid-hymn.
 *
 * **Keys ([dispatchKeyEvent]).** While a PDF is open, the volume keys and a Bluetooth pedal turn pages
 * ([LecternPageTurns]): the reader takes those keys before the system, so they neither change the volume
 * nor move the focus. A dialog on top has its own window, so there the volume keys are volume again.
 *
 * **Leaving.** onStop hands the ViewModel `isChangingConfigurations`: a rotation silences nothing, while
 * leaving stops the ison — unless «Συνέχισε στο παρασκήνιο» is on, when J5's service plays it on.
 *
 * **Opened with** [intent]: the PDF's URI as data, with a read grant passed along, and its title.
 */
class LecternReaderActivity : BaseActivity() {

    private val viewModel: LecternReaderViewModel by viewModels()

    override val screenOrientation: Int get() = ActivityInfo.SCREEN_ORIENTATION_FULL_USER

    private lateinit var uri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uri = intent.data ?: run {
            finish()
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        viewModel.defaultNight(currentPalette().isDark)
        viewModel.open(uri)
        setContent {
            LbmTheme(palette = currentPalette()) {
                val state by viewModel.state.collectAsState()
                val actions = remember { actions() }
                LecternReaderScreen(state = state, title = title, actions = actions)
            }
        }
    }

    private fun actions() = LecternReaderActions(
        renderPage = viewModel::page,
        prefetchPage = viewModel::prefetch,
        onTurn = viewModel::turn,
        onGoTo = viewModel::goTo,
        onNightChange = viewModel::setNight,
        onIsonChange = viewModel::setIson,
        onChooseMode = viewModel::chooseMode,
        onChooseShift = viewModel::chooseShift,
        onChooseIson = viewModel::chooseIson,
        onClearPage = viewModel::clearPage,
        onUsePageSetting = viewModel::usePageSetting,
        onPlayApichima = viewModel::playApichima,
        onStopApichima = viewModel::stopApichima,
        onRemoveFromLibrary = ::removeFromLibrary,
        onBack = ::finish,
    )

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val ready = viewModel.state.value.phase == LecternReaderViewModel.Phase.READY
        if (!ready || LecternPageTurns.forKey(event.keyCode, event.isShiftPressed) == null) {
            return super.dispatchKeyEvent(event)
        }
        LecternPageTurns.turnsOn(
            keyCode = event.keyCode,
            shiftPressed = event.isShiftPressed,
            isDown = event.action == KeyEvent.ACTION_DOWN,
            repeatCount = event.repeatCount,
        )?.let(viewModel::turn)
        // Down, repeats and up alike: none of them may reach the system's volume or focus handling.
        return true
    }

    /** A PDF that can no longer be read: forget it here, with its grant, and go back to the library. */
    private fun removeFromLibrary() {
        val prefs = LecternPrefs(this)
        prefs.saveLibrary(prefs.library().remove(uri.toString(), ContentResolverGrants(contentResolver)))
        finish()
    }

    override fun onStart() {
        super.onStart()
        viewModel.onScreenStarted()
    }

    override fun onStop() {
        viewModel.onScreenStopped(changingConfigurations = isChangingConfigurations)
        super.onStop()
    }

    companion object {
        private const val EXTRA_TITLE = "com.johnchourp.learnbyzantinemusic.lectern.EXTRA_TITLE"

        fun intent(context: Context, uri: Uri, title: String): Intent =
            Intent(context, LecternReaderActivity::class.java)
                .setData(uri)
                .putExtra(EXTRA_TITLE, title)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
