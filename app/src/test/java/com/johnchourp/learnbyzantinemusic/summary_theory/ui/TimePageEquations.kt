package com.johnchourp.learnbyzantinemusic.summary_theory.ui

/**
 * Test-only: every worked equation of the «Χαρακτήρες Χρόνου» page, by the name [TimeCharacters]
 * declares it under (`trigorgo`, `presentedGorgo[1]` …). Read by reflection, so an equation added
 * to the page later is swept by the tests that use this without anyone remembering to list it.
 */
internal object TimePageEquations {

    val all: Map<String, TimeEquation> by lazy {
        TimeCharacters::class.java.declaredFields.flatMap { field ->
            field.isAccessible = true
            val value: Any? = field.get(TimeCharacters)
            if (value is TimeEquation) {
                listOf(field.name to value)
            } else {
                (value as? List<*>).orEmpty().filterIsInstance<TimeEquation>()
                    .mapIndexed { i, eq -> "${field.name}[$i]" to eq }
            }
        }.toMap()
    }
}
