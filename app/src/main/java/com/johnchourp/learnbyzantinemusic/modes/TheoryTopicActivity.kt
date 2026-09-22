package com.johnchourp.learnbyzantinemusic.modes

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import androidx.core.graphics.ColorUtils

/**
 * A single theory page under «8 Ήχοι» — one entry of [TheoryTopicCatalog], rendered generically.
 *
 * **Why generic.** Every navigable theory page goes through the catalog, and the pages menu is
 * *generated* from it. That is what keeps the menu, the navigation and the page set from drifting: a
 * new page is a catalog entry, never a new activity and never a hand-written menu row.
 *
 * **Inputs:**
 * - `TheoryTopicCatalog.EXTRA_TOPIC_KEY` — which topic to show; an unknown key falls back to a valid one.
 * - `EightModesNavigation.EXTRA_NAV_PATH_TOPIC_KEYS` — the trail walked to get here, used for the
 *   breadcrumb. [EightModesNavigation.resolveTopicPath] repairs a missing or inconsistent trail, so a
 *   deep link with no path still renders a sensible breadcrumb.
 *
 * **Body may be empty by design:** a topic whose `bodyRes` is 0 is a navigation hub, and the body card
 * is hidden rather than shown blank.
 *
 * **Stores nothing.**
 */
class TheoryTopicActivity : BaseActivity() {
    private lateinit var navigationMenuButton: Button
    private lateinit var favoriteButton: Button
    private lateinit var backButton: Button
    private lateinit var breadcrumbText: TextView
    private lateinit var titleText: TextView
    private lateinit var bodyCard: LinearLayout
    private lateinit var bodyText: TextView
    private lateinit var currentTopic: TheoryTopic
    private var currentPathTopicKeys: List<String> = emptyList()
    private var highlightQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_theory_topic)

        navigationMenuButton = findViewById(R.id.theory_topic_navigation_menu_button)
        favoriteButton = findViewById(R.id.theory_topic_favorite_button)
        backButton = findViewById(R.id.theory_topic_back_button)
        breadcrumbText = findViewById(R.id.theory_topic_breadcrumb)
        titleText = findViewById(R.id.theory_topic_title)
        bodyCard = findViewById(R.id.theory_topic_body_card)
        bodyText = findViewById(R.id.theory_topic_body)

        currentTopic = TheoryTopicCatalog.byKey(
            intent.getStringExtra(TheoryTopicCatalog.EXTRA_TOPIC_KEY)
        )
        highlightQuery = intent.getStringExtra(EightModesNavigation.EXTRA_HIGHLIGHT_QUERY).orEmpty()
        currentPathTopicKeys = EightModesNavigation.resolveTopicPath(
            pathTopicKeys = intent.getStringArrayListExtra(
                EightModesNavigation.EXTRA_NAV_PATH_TOPIC_KEYS
            ).orEmpty(),
            currentTopicKey = currentTopic.key
        )
        bindTopic(currentTopic)
        renderFavorite(TheoryTopicFavorites.isFavorite(this, currentTopic.key))
        favoriteButton.setOnClickListener {
            renderFavorite(TheoryTopicFavorites.toggle(this, currentTopic.key))
        }
        navigationMenuButton.setOnClickListener {
            EightModesNavigation.showMenu(this, selectedTopicKey = currentTopic.key)
        }
        backButton.setOnClickListener { finish() }
    }

    /**
     * Lights up the search term that led here (ClickUp `869f4tph0`).
     *
     * Applied **after** [TheoryTopicLinks.setLinkedText] has run, and with `SPAN_EXCLUSIVE_EXCLUSIVE`,
     * so the existing tappable cross-links keep their own spans and behaviour — a highlight must not
     * cost the reader a working link.
     *
     * Ranges come from [TheorySearch.matchRanges], the same matcher the menu used to decide this page
     * was a result. Anything else would sometimes light nothing on a page the search had just found,
     * which reads as a broken page rather than a near-miss.
     */
    private fun applyHighlight(textView: TextView) {
        if (highlightQuery.isBlank()) return
        val text = textView.text ?: return
        val ranges = TheorySearch.matchRanges(text.toString(), highlightQuery)
        if (ranges.isEmpty()) return
        val spannable = SpannableString(text)
        val color = ContextCompat.getColor(this, R.color.first_mode_theory_accent)
        ranges.forEach { range ->
            spannable.setSpan(
                BackgroundColorSpan(ColorUtils.setAlphaComponent(color, HIGHLIGHT_ALPHA)),
                range.first,
                range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        textView.text = spannable
    }

    /** Star state is rendered from the value the store returns, never from a local toggle, so the
     *  button cannot end up showing a state that was refused (e.g. an unknown topic key). */
    private fun renderFavorite(isFavorite: Boolean) {
        favoriteButton.setText(
            if (isFavorite) R.string.theory_topic_favorite_on_symbol
            else R.string.theory_topic_favorite_off_symbol
        )
        favoriteButton.contentDescription = getString(
            if (isFavorite) R.string.theory_topic_favorite_remove
            else R.string.theory_topic_favorite_add
        )
    }

    private fun bindTopic(topic: TheoryTopic) {
        title = getString(topic.titleRes)
        breadcrumbText.text = EightModesNavigation.breadcrumbText(this, currentPathTopicKeys)
        titleText.setText(topic.titleRes)
        applyHighlight(titleText)
        if (topic.bodyRes == 0) {
            bodyCard.visibility = View.GONE
            bodyText.text = ""
        } else {
            bodyCard.visibility = View.VISIBLE
            TheoryTopicLinks.setLinkedText(
                context = this,
                textView = bodyText,
                textValue = getString(topic.bodyRes),
                excludedTopicKey = topic.key,
                currentPathTopicKeys = currentPathTopicKeys
            )
            bodyText.setTextColor(
                ContextCompat.getColor(this, R.color.first_mode_theory_text_primary)
            )
            applyHighlight(bodyText)
        }
    }
    private companion object {
        /** Enough to find the word at a glance, light enough to keep the text readable. */
        const val HIGHLIGHT_ALPHA = 64
    }

}
