package com.johnchourp.learnbyzantinemusic.trainer

import com.johnchourp.learnbyzantinemusic.music.Beats
import com.johnchourp.learnbyzantinemusic.music.ShownBeats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * The Trainer's «Κανόνες χρόνου» card prints the rules' numbers, not numbers of its own (ClickUp
 * `869f5x29r`, H5).
 *
 * Its sentences used to carry «1», «0,5» and «0,5» as text, a second copy of the rules that nothing
 * checked. Now they carry placeholders, filled from [TimingRulesHelp], which reads them off the rules.
 * The old one-paragraph `melody_trainer_rules_body` is not shown anywhere any more; it is left as it
 * is and not checked here.
 */
class TimingRulesHelpTextTest {

    /** Each sentence of the card, with the number of values it takes. */
    private val sentences = mapOf(
        "melody_trainer_rule_default_body" to 1,
        "melody_trainer_rule_gorgo_body" to 2,
        "melody_trainer_rule_klasma_body" to 1,
    )

    private val placeholder = Regex("""%(\d+)[$]s""")

    @Test
    fun theNumbersAreWhatTheRulesDo() {
        assertEquals(Beats.ONE, TimingRulesHelp.defaultLength)
        assertEquals(Beats.of(1, 2), TimingRulesHelp.gorgonNote)
        assertEquals(Beats.of(1, 2), TimingRulesHelp.gorgonTakes)
        assertEquals(Beats.ONE, TimingRulesHelp.klasmaAdds)
    }

    @Test
    fun noSentenceTypesANumberInAnyLanguage() {
        var checked = 0
        ShownBeats.languages.forEach { (folder, strings) ->
            sentences.forEach { (name, values) ->
                val text = strings[name] ?: return@forEach
                val where = "$folder/$name: «$text»"
                val prose = placeholder.replace(text, "")
                assertTrue("$where types a number", prose.none { it.isDigit() || it in ShownBeats.fractionGlyphs })
                assertEquals(where, (1..values).toList(), placeholder.findAll(text).map { it.groupValues[1].toInt() }.toList())
                checked++
            }
        }
        assertTrue("expected the three sentences in Greek and English, checked $checked", checked >= 2 * sentences.size)
    }

    @Test
    fun theCardReadsAsItAlwaysDidWithTheRulesNumbers() {
        fun card(folder: String, name: String, separator: Char, beats: List<Beats>): String =
            String.format(
                Locale.ROOT,
                ShownBeats.languages.getValue(folder).getValue(name),
                *beats.map { BeatsLabel.of(it, separator) }.toTypedArray(),
            )
        val help = TimingRulesHelp
        assertEquals(
            "Κάθε φθόγγος κρατάει 1 χρόνο.",
            card("values", "melody_trainer_rule_default_body", ',', listOf(help.defaultLength)),
        )
        assertEquals(
            "Ο φθόγγος γίνεται μισός χρόνος (0,5) και «τραβάει» 0,5 από τον προηγούμενο — δύο φθόγγοι σε έναν χρόνο.",
            card("values", "melody_trainer_rule_gorgo_body", ',', listOf(help.gorgonNote, help.gorgonTakes)),
        )
        assertEquals(
            "Προσθέτει 1 ολόκληρο χρόνο στον φθόγγο.",
            card("values", "melody_trainer_rule_klasma_body", ',', listOf(help.klasmaAdds)),
        )
        assertEquals(
            "The phthong becomes half a beat (0.5) and takes 0.5 from the previous one — two phthongi in one beat.",
            card("values-en", "melody_trainer_rule_gorgo_body", '.', listOf(help.gorgonNote, help.gorgonTakes)),
        )
    }

    @Test
    fun lengthsArePrintedAsTheNoteRowsPrintThem() {
        assertEquals("1", BeatsLabel.of(Beats.ONE, ','))
        assertEquals("0,5", BeatsLabel.of(Beats.of(1, 2), ','))
        assertEquals("1.5", BeatsLabel.of(Beats.of(3, 2), '.'))
        assertEquals("4", BeatsLabel.of(Beats.whole(4), ','))
        // A length finer than a half is not rounded to one.
        assertEquals("0,33", BeatsLabel.of(Beats.of(1, 3), ','))
        assertEquals("0,25", BeatsLabel.of(Beats.of(1, 4), ','))
    }
}
