package com.flashlearn.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnumsTest {

    @Test
    fun `VocabularyDifficulty ordinal order is EASY lt MEDIUM lt HARD lt VERY_HARD`() {
        // Difficulty Calculation Algorithm compares ordinals directly
        // (e.g. "at least MEDIUM" forced update). Reordering this enum
        // silently breaks that logic, so pin it down with a test.
        assertEquals(0, VocabularyDifficulty.EASY.ordinal)
        assertEquals(1, VocabularyDifficulty.MEDIUM.ordinal)
        assertEquals(2, VocabularyDifficulty.HARD.ordinal)
        assertEquals(3, VocabularyDifficulty.VERY_HARD.ordinal)
    }

    @Test
    fun `Stage has exactly four values with no NEW stage`() {
        assertEquals(4, Stage.entries.size)
        assertEquals(
            listOf(Stage.DAILY, Stage.WEEKLY, Stage.MONTHLY, Stage.LEARNED),
            Stage.entries
        )
    }
}
