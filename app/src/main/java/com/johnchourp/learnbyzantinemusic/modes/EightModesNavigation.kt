package com.johnchourp.learnbyzantinemusic.modes

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.johnchourp.learnbyzantinemusic.R
import java.text.Normalizer
import java.util.Locale

object EightModesNavigation {
    const val EXTRA_NAV_PATH_TOPIC_KEYS = "theory_nav_path_topic_keys"
    const val HOME_ENTRY_KEY = "eight_modes_home"

    data class NavigationEntry(
        val key: String,
        val title: String,
        val searchableText: String,
        val topicKey: String? = null
    )

    fun navigationEntryKeys(): List<String> =
        listOf(HOME_ENTRY_KEY) + TheoryTopicCatalog.topics.map { it.key }

    fun resolveTopicPath(pathTopicKeys: List<String>, currentTopicKey: String): List<String> {
        val knownPath = pathTopicKeys.filter(::isKnownTopicKey)
        return if (knownPath.lastOrNull() == currentTopicKey) {
            knownPath
        } else {
            knownPath + currentTopicKey
        }
    }

    fun pathForLinkedTopic(currentPathTopicKeys: List<String>, targetTopicKey: String): ArrayList<String> {
        val knownPath = currentPathTopicKeys.filter(::isKnownTopicKey)
        val nextPath = if (knownPath.lastOrNull() == targetTopicKey) {
            knownPath
        } else {
            knownPath + targetTopicKey
        }
        return ArrayList(nextPath)
    }

    fun pathForMenuTopic(targetTopicKey: String): ArrayList<String> =
        arrayListOf(targetTopicKey)

    fun breadcrumbText(context: Context, topicPathKeys: List<String>): String {
        val labels = listOf(context.getString(R.string.eight_modes_navigation_home_title)) +
            topicPathKeys
                .filter(::isKnownTopicKey)
                .map { key -> context.getString(TheoryTopicCatalog.byKey(key).titleRes) }
        return labels.joinToString(context.getString(R.string.eight_modes_breadcrumb_separator))
    }

    fun topicIntent(context: Context, topicKey: String, pathTopicKeys: ArrayList<String>): Intent =
        Intent(context, TheoryTopicActivity::class.java).apply {
            putExtra(TheoryTopicCatalog.EXTRA_TOPIC_KEY, topicKey)
            putStringArrayListExtra(EXTRA_NAV_PATH_TOPIC_KEYS, pathTopicKeys)
        }

    fun homeIntent(context: Context): Intent =
        Intent(context, EightModesActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

    fun entries(context: Context): List<NavigationEntry> {
        val homeTitle = context.getString(R.string.eight_modes_navigation_home_menu_title)
        val homeSearchText = listOf(
            context.getString(R.string.eight_modes_navigation_home_title),
            context.getString(R.string.eight_modes_page_title),
            context.getString(R.string.eight_modes_page_subtitle),
            context.getString(R.string.eight_modes_current_mode_title)
        ).joinToString(" ")
        return listOf(
            NavigationEntry(
                key = HOME_ENTRY_KEY,
                title = homeTitle,
                searchableText = homeSearchText
            )
        ) + TheoryTopicCatalog.topics.map { topic ->
            val bodyText = if (topic.bodyRes == 0) "" else context.getString(topic.bodyRes)
            val title = context.getString(topic.titleRes)
            NavigationEntry(
                key = topic.key,
                title = title,
                searchableText = "$title $bodyText",
                topicKey = topic.key
            )
        }
    }

    fun filterEntries(entries: List<NavigationEntry>, query: String): List<NavigationEntry> {
        val normalizedQuery = normalizeSearch(query)
        return if (normalizedQuery.isBlank()) {
            entries
        } else {
            entries.filter { entry ->
                normalizeSearch(entry.searchableText).contains(normalizedQuery)
            }
        }
    }

    fun showMenu(
        activity: Activity,
        selectedTopicKey: String?
    ) {
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val allEntries = entries(activity)
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(18), activity.dp(18), activity.dp(18), activity.dp(12))
            background = roundedBackground(
                fillColor = Color.WHITE,
                strokeColor = ContextCompat.getColor(activity, R.color.first_mode_theory_card_border),
                radiusDp = 18,
                context = activity
            )
        }

        root.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.eight_modes_navigation_menu_title)
                setTextColor(ContextCompat.getColor(activity, R.color.first_mode_theory_text_primary))
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
            }
        )

        val searchInput = EditText(activity).apply {
            hint = activity.getString(R.string.eight_modes_navigation_search_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
            setTextColor(ContextCompat.getColor(activity, R.color.first_mode_theory_text_primary))
            setHintTextColor(ContextCompat.getColor(activity, R.color.first_mode_theory_text_secondary))
            textSize = 15f
            setPadding(activity.dp(12), 0, activity.dp(12), 0)
            setBackgroundResource(R.drawable.eight_modes_spinner_bg)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.dp(48)
            ).apply {
                topMargin = activity.dp(12)
            }
        }
        root.addView(searchInput)

        val listContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scrollView = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = activity.dp(12)
            }
            addView(listContainer)
        }
        root.addView(scrollView)

        fun renderRows(query: String) {
            listContainer.removeAllViews()
            val visibleEntries = filterEntries(allEntries, query)
            if (visibleEntries.isEmpty()) {
                listContainer.addView(
                    TextView(activity).apply {
                        text = activity.getString(R.string.eight_modes_navigation_no_results)
                        gravity = Gravity.CENTER
                        setTextColor(ContextCompat.getColor(activity, R.color.first_mode_theory_text_secondary))
                        textSize = 15f
                        setPadding(0, activity.dp(24), 0, activity.dp(24))
                    }
                )
                return
            }
            visibleEntries.forEach { entry ->
                val isSelected = entry.topicKey == selectedTopicKey ||
                    (entry.topicKey == null && selectedTopicKey == null)
                listContainer.addView(
                    createEntryRow(
                        activity = activity,
                        entry = entry,
                        isSelected = isSelected
                    ) {
                        dialog.dismiss()
                        navigateToEntry(activity, entry, selectedTopicKey)
                    }
                )
            }
        }

        renderRows("")
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderRows(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        dialog.setContentView(root)
        dialog.setOnShowListener {
            dialog.window?.setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.94f).toInt(),
                (activity.resources.displayMetrics.heightPixels * 0.82f).toInt()
            )
            searchInput.requestFocus()
        }
        dialog.show()
    }

    private fun createEntryRow(
        activity: Activity,
        entry: NavigationEntry,
        isSelected: Boolean,
        onClick: () -> Unit
    ): TextView =
        TextView(activity).apply {
            text = if (isSelected) {
                activity.getString(R.string.eight_modes_navigation_selected_template, entry.title)
            } else {
                entry.title
            }
            setTextColor(
                ContextCompat.getColor(
                    activity,
                    if (isSelected) {
                        R.color.first_mode_theory_accent
                    } else {
                        R.color.first_mode_theory_text_primary
                    }
                )
            )
            textSize = 16f
            typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            gravity = Gravity.CENTER_VERTICAL
            setPadding(activity.dp(12), 0, activity.dp(12), 0)
            minHeight = activity.dp(48)
            background = roundedBackground(
                fillColor = if (isSelected) {
                    ContextCompat.getColor(activity, R.color.first_mode_theory_callout_bg)
                } else {
                    Color.TRANSPARENT
                },
                strokeColor = if (isSelected) {
                    ContextCompat.getColor(activity, R.color.first_mode_theory_callout_border)
                } else {
                    Color.TRANSPARENT
                },
                radiusDp = 10,
                context = activity
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = activity.dp(6)
            }
            setOnClickListener { onClick() }
        }

    private fun navigateToEntry(
        activity: Activity,
        entry: NavigationEntry,
        selectedTopicKey: String?
    ) {
        val topicKey = entry.topicKey
        if (topicKey == null) {
            if (selectedTopicKey != null) {
                activity.startActivity(homeIntent(activity))
            }
            return
        }
        if (topicKey == selectedTopicKey) {
            return
        }
        activity.startActivity(topicIntent(activity, topicKey, pathForMenuTopic(topicKey)))
    }

    private fun normalizeSearch(value: String): String {
        val lowercaseValue = value.lowercase(Locale.getDefault())
        return Normalizer.normalize(lowercaseValue, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .trim()
    }

    private fun roundedBackground(
        fillColor: Int,
        strokeColor: Int,
        radiusDp: Int,
        context: Context
    ): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fillColor)
            setStroke(context.dp(1), strokeColor)
            cornerRadius = context.dp(radiusDp).toFloat()
        }

    private fun isKnownTopicKey(key: String): Boolean =
        TheoryTopicCatalog.topics.any { it.key == key }

    private fun Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
