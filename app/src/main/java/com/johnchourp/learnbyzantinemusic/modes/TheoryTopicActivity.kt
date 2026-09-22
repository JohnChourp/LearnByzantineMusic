package com.johnchourp.learnbyzantinemusic.modes

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R

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
    private lateinit var backButton: Button
    private lateinit var breadcrumbText: TextView
    private lateinit var titleText: TextView
    private lateinit var bodyCard: LinearLayout
    private lateinit var bodyText: TextView
    private lateinit var currentTopic: TheoryTopic
    private var currentPathTopicKeys: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_theory_topic)

        navigationMenuButton = findViewById(R.id.theory_topic_navigation_menu_button)
        backButton = findViewById(R.id.theory_topic_back_button)
        breadcrumbText = findViewById(R.id.theory_topic_breadcrumb)
        titleText = findViewById(R.id.theory_topic_title)
        bodyCard = findViewById(R.id.theory_topic_body_card)
        bodyText = findViewById(R.id.theory_topic_body)

        currentTopic = TheoryTopicCatalog.byKey(
            intent.getStringExtra(TheoryTopicCatalog.EXTRA_TOPIC_KEY)
        )
        currentPathTopicKeys = EightModesNavigation.resolveTopicPath(
            pathTopicKeys = intent.getStringArrayListExtra(
                EightModesNavigation.EXTRA_NAV_PATH_TOPIC_KEYS
            ).orEmpty(),
            currentTopicKey = currentTopic.key
        )
        bindTopic(currentTopic)
        navigationMenuButton.setOnClickListener {
            EightModesNavigation.showMenu(this, selectedTopicKey = currentTopic.key)
        }
        backButton.setOnClickListener { finish() }
    }

    private fun bindTopic(topic: TheoryTopic) {
        title = getString(topic.titleRes)
        breadcrumbText.text = EightModesNavigation.breadcrumbText(this, currentPathTopicKeys)
        titleText.setText(topic.titleRes)
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
        }
    }
}
