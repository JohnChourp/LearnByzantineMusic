package com.johnchourp.learnbyzantinemusic.modes

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.johnchourp.learnbyzantinemusic.R

object TheoryTopicLinks {
    data class LinkPattern(val regex: Regex, val topicKey: String)
    data class TopicLink(val start: Int, val endExclusive: Int, val topicKey: String)

    val linkPatterns: List<LinkPattern> = listOf(
        LinkPattern(Regex("""\(2\)"""), TheoryTopicCatalog.TROCHOS),
        LinkPattern(Regex("""\(6\)"""), TheoryTopicCatalog.MIDDLE_ACUTE_GRAVE_DIAPASON),
        LinkPattern(Regex("""\(4\)"""), TheoryTopicCatalog.SCALE),
        LinkPattern(Regex("""\(9\)"""), TheoryTopicCatalog.TESTIMONIES),
        LinkPattern(Regex("""\(44\)"""), TheoryTopicCatalog.PENTACHORD_TROCHOS),
        LinkPattern(Regex("""Σχήμα\s+XVI""", RegexOption.IGNORE_CASE), TheoryTopicCatalog.FIGURE_XVI),
        LinkPattern(Regex("""Figure\s+XVI""", RegexOption.IGNORE_CASE), TheoryTopicCatalog.FIGURE_XVI),
        LinkPattern(
            Regex("""οκτάχορδ\p{L}*(?:\s+ή\s+διαπασών)?|διαπασών""", RegexOption.IGNORE_CASE),
            TheoryTopicCatalog.OCTAVE_DIAPASON
        ),
        LinkPattern(
            Regex("""octave(?:\s+or\s+diapason)?|diapason""", RegexOption.IGNORE_CASE),
            TheoryTopicCatalog.OCTAVE_DIAPASON
        ),
        LinkPattern(
            Regex("""πεντάχορδ\p{L}*(?:\s+ή\s+(?:τον\s+)?τροχ\p{L}*)?""", RegexOption.IGNORE_CASE),
            TheoryTopicCatalog.PENTACHORD_TROCHOS
        ),
        LinkPattern(
            Regex("""pentachord\p{L}*(?:\s+or\s+trochos)?""", RegexOption.IGNORE_CASE),
            TheoryTopicCatalog.PENTACHORD_TROCHOS
        )
    )

    fun open(
        context: Context,
        topicKey: String,
        currentPathTopicKeys: List<String> = emptyList()
    ) {
        context.startActivity(
            EightModesNavigation.topicIntent(
                context = context,
                topicKey = topicKey,
                pathTopicKeys = EightModesNavigation.pathForLinkedTopic(
                    currentPathTopicKeys = currentPathTopicKeys,
                    targetTopicKey = topicKey
                )
            )
        )
    }

    fun setLinkedText(
        context: Context,
        textView: TextView,
        textValue: String,
        excludedTopicKey: String? = null,
        currentPathTopicKeys: List<String> = emptyList()
    ) {
        val linkedText = createLinkedText(context, textValue, excludedTopicKey, currentPathTopicKeys)
        textView.text = linkedText
        val hasLinks = linkedText.getSpans(0, linkedText.length, ClickableSpan::class.java).isNotEmpty()
        if (hasLinks) {
            textView.movementMethod = LinkMovementMethod.getInstance()
            textView.highlightColor = Color.TRANSPARENT
            textView.linksClickable = true
        } else {
            textView.movementMethod = null
        }
    }

    fun createLinkedText(
        context: Context,
        textValue: String,
        excludedTopicKey: String? = null,
        currentPathTopicKeys: List<String> = emptyList()
    ): SpannableString {
        val spannable = SpannableString(textValue)
        findTopicLinks(textValue, excludedTopicKey).forEach { link ->
            applyTopicButtonSpan(
                context = context,
                spannable = spannable,
                start = link.start,
                end = link.endExclusive,
                topicKey = link.topicKey,
                currentPathTopicKeys = currentPathTopicKeys
            )
        }
        return spannable
    }

    fun findTopicLinks(textValue: String, excludedTopicKey: String? = null): List<TopicLink> {
        val links = mutableListOf<TopicLink>()
        val acceptedRanges = mutableListOf<IntRange>()
        linkPatterns.forEach { pattern ->
            if (pattern.topicKey == excludedTopicKey) {
                return@forEach
            }
            pattern.regex.findAll(textValue).forEach { match ->
                val range = match.range
                if (range.first <= range.last && acceptedRanges.none { it.overlaps(range) }) {
                    acceptedRanges.add(range)
                    links.add(
                        TopicLink(
                            start = range.first,
                            endExclusive = range.last + 1,
                            topicKey = pattern.topicKey
                        )
                    )
                }
            }
        }
        return links
    }

    private fun applyTopicButtonSpan(
        context: Context,
        spannable: SpannableString,
        start: Int,
        end: Int,
        topicKey: String,
        currentPathTopicKeys: List<String>
    ) {
        val accentColor = ContextCompat.getColor(context, R.color.first_mode_theory_accent)
        val calloutColor = ContextCompat.getColor(context, R.color.first_mode_theory_callout_bg)
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    open(widget.context, topicKey, currentPathTopicKeys)
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = accentColor
                    ds.isUnderlineText = false
                    ds.bgColor = calloutColor
                }
            },
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            ForegroundColorSpan(accentColor),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            BackgroundColorSpan(calloutColor),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun IntRange.overlaps(other: IntRange): Boolean =
        first <= other.last && other.first <= last
}
