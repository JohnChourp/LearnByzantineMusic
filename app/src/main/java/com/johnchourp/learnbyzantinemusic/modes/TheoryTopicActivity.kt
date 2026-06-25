package com.johnchourp.learnbyzantinemusic.modes

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R

class TheoryTopicActivity : BaseActivity() {
    private lateinit var backButton: Button
    private lateinit var titleText: TextView
    private lateinit var bodyCard: LinearLayout
    private lateinit var bodyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_theory_topic)

        backButton = findViewById(R.id.theory_topic_back_button)
        titleText = findViewById(R.id.theory_topic_title)
        bodyCard = findViewById(R.id.theory_topic_body_card)
        bodyText = findViewById(R.id.theory_topic_body)

        bindTopic(
            TheoryTopicCatalog.byKey(
                intent.getStringExtra(TheoryTopicCatalog.EXTRA_TOPIC_KEY)
            )
        )
        backButton.setOnClickListener { finish() }
    }

    private fun bindTopic(topic: TheoryTopic) {
        title = getString(topic.titleRes)
        titleText.setText(topic.titleRes)
        if (topic.bodyRes == 0) {
            bodyCard.visibility = View.GONE
            bodyText.text = ""
        } else {
            bodyCard.visibility = View.VISIBLE
            TheoryTopicLinks.setLinkedText(this, bodyText, getString(topic.bodyRes))
            bodyText.setTextColor(
                ContextCompat.getColor(this, R.color.first_mode_theory_text_primary)
            )
        }
    }
}
