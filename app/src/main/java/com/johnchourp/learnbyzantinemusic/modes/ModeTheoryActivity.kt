package com.johnchourp.learnbyzantinemusic.modes

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R

class ModeTheoryActivity : BaseActivity() {
    private lateinit var backButton: Button
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var heroSummaryText: TextView
    private lateinit var signImage: ImageView
    private lateinit var quickFactsText: TextView
    private lateinit var styleRowsContainer: LinearLayout
    private lateinit var notesContainer: LinearLayout
    private lateinit var sourceNoteText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_mode_theory)

        backButton = findViewById(R.id.mode_theory_back_button)
        titleText = findViewById(R.id.mode_theory_title)
        subtitleText = findViewById(R.id.mode_theory_subtitle)
        heroSummaryText = findViewById(R.id.mode_theory_hero_summary)
        signImage = findViewById(R.id.mode_theory_sign)
        quickFactsText = findViewById(R.id.mode_theory_quick_facts)
        styleRowsContainer = findViewById(R.id.mode_theory_style_rows_container)
        notesContainer = findViewById(R.id.mode_theory_notes_container)
        sourceNoteText = findViewById(R.id.mode_theory_source_note)

        val theory = ModeTheoryCatalog.byKey(
            intent.getStringExtra(ModeTheoryCatalog.EXTRA_MODE_KEY)
        )
        bindTheory(theory)
        backButton.setOnClickListener { finish() }
    }

    private fun bindTheory(theory: ModeTheory) {
        title = getString(theory.titleRes)
        titleText.setText(theory.titleRes)
        subtitleText.setText(theory.subtitleRes)
        heroSummaryText.setText(theory.heroSummaryRes)
        signImage.setImageResource(theory.signRes)
        signImage.contentDescription = getString(theory.signDescriptionRes)
        quickFactsText.text = getString(
            R.string.mode_theory_quick_facts_template,
            getString(theory.apichimaRes),
            getString(theory.ethosRes)
        )

        styleRowsContainer.removeAllViews()
        styleRowsContainer.addView(createTitleText(R.string.mode_theory_table_title).apply {
            textSize = 20f
        })
        theory.styleRows.forEach { styleRowsContainer.addView(createStyleRowView(it)) }

        notesContainer.removeAllViews()
        notesContainer.addView(
            createTheorySection(
                R.string.mode_theory_section_modulations,
                theory.modulationsRes
            )
        )
        notesContainer.addView(
            createTheorySection(
                R.string.mode_theory_section_attractions,
                theory.attractionsRes
            )
        )
        notesContainer.addView(
            createTheorySection(
                R.string.mode_theory_section_phthores,
                theory.phthoresRes
            )
        )
        notesContainer.addView(
            createTheorySection(
                R.string.mode_theory_section_ethos,
                theory.ethosRes
            )
        )
        sourceNoteText.setText(R.string.mode_theory_source_note)
    }

    private fun createStyleRowView(row: ModeTheoryStyleRow): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(
                this@ModeTheoryActivity,
                R.drawable.first_mode_theory_callout_bg
            )
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
            }
        }
        card.addView(createTitleText(row.styleNameRes))
        card.addView(createFieldText(R.string.mode_theory_field_system, row.systemRes))
        card.addView(createFieldText(R.string.mode_theory_field_scale, row.scaleRes))
        card.addView(createFieldText(R.string.mode_theory_field_base, row.baseRes))
        card.addView(
            createFieldText(
                R.string.mode_theory_field_dominants_cadences,
                row.dominantsCadencesRes
            )
        )
        return card
    }

    private fun createTheorySection(@StringRes titleRes: Int, @StringRes bodyRes: Int): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(
                this@ModeTheoryActivity,
                R.drawable.first_mode_theory_card_bg
            )
            setPadding(dp(18), dp(18), dp(18), dp(18))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        }
        card.addView(createTitleText(titleRes))
        card.addView(createBodyText(bodyRes).apply { setPadding(0, dp(8), 0, 0) })
        return card
    }

    private fun createFieldText(@StringRes labelRes: Int, @StringRes valueRes: Int): TextView =
        createBodyText(
            getString(R.string.mode_theory_field_template, getString(labelRes), getString(valueRes))
        ).apply {
            setPadding(0, dp(8), 0, 0)
        }

    private fun createTitleText(@StringRes textRes: Int): TextView =
        TextView(this).apply {
            setText(textRes)
            setTextColor(ContextCompat.getColor(this@ModeTheoryActivity, R.color.first_mode_theory_accent))
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
        }

    private fun createBodyText(@StringRes textRes: Int): TextView =
        createBodyText(getString(textRes))

    private fun createBodyText(textValue: String): TextView =
        TextView(this).apply {
            text = textValue
            setTextColor(
                ContextCompat.getColor(
                    this@ModeTheoryActivity,
                    R.color.first_mode_theory_text_primary
                )
            )
            textSize = 15f
            setLineSpacing(dp(2).toFloat(), 1.0f)
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
