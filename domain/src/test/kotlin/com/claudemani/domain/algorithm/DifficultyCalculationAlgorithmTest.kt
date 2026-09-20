package com.claudemani.domain.algorithm

import com.claudemani.domain.model.DifficultyState
import com.claudemani.domain.model.ReviewType
import com.claudemani.domain.model.VocabularyDifficulty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class DifficultyCalculationAlgorithmTest {

    private fun state(
        current: VocabularyDifficulty = VocabularyDifficulty.EASY,
        cc: Int = 0,
        cw: Int = 0,
        reached: Boolean = false
    ) = DifficultyState(
        id = UUID.randomUUID(),
        conceptId = UUID.randomUUID(),
        current = current,
        consecutiveCorrect = cc,
        consecutiveWrong = cw,
        hasReachedVeryHard = reached
    )

    // ---------- Forced Updates ----------

    @Test
    fun `WEEKLY wrong forces at least MEDIUM and resets counters`() {
        val result = calculateDifficulty(
            state(VocabularyDifficulty.EASY, cc = 2), false, ReviewType.WEEKLY, monthlyWrongCountBefore = 0
        )
        assertEquals(VocabularyDifficulty.MEDIUM, result.current)
        assertEquals(0, result.consecutiveCorrect)
        assertEquals(0, result.consecutiveWrong)
    }

    @Test
    fun `WEEKLY wrong keeps existing HARD (never downgrades)`() {
        val result = calculateDifficulty(
            state(VocabularyDifficulty.HARD), false, ReviewType.WEEKLY, monthlyWrongCountBefore = 0
        )
        assertEquals(VocabularyDifficulty.HARD, result.current)
    }

    @Test
    fun `MONTHLY first wrong (countBefore=0) sets HARD`() {
        val result = calculateDifficulty(state(), false, ReviewType.MONTHLY, monthlyWrongCountBefore = 0)
        assertEquals(VocabularyDifficulty.HARD, result.current)
        assertEquals(0, result.consecutiveCorrect)
        assertEquals(0, result.consecutiveWrong)
        assertFalse(result.hasReachedVeryHard)
    }

    @Test
    fun `MONTHLY subsequent wrong (countBefore greater than 0) sets VERY_HARD and flag`() {
        val result = calculateDifficulty(state(), false, ReviewType.MONTHLY, monthlyWrongCountBefore = 1)
        assertEquals(VocabularyDifficulty.VERY_HARD, result.current)
        assertTrue(result.hasReachedVeryHard)
    }

    @Test
    fun `forced updates do not apply to RANDOM review type`() {
        // Even though the answer is wrong, RANDOM must use the normal
        // consecutive-counter path, not the WEEKLY/MONTHLY forced jump.
        val result = calculateDifficulty(state(cw = 1), false, ReviewType.RANDOM, monthlyWrongCountBefore = 0)
        assertEquals(VocabularyDifficulty.EASY, result.current)
        assertEquals(2, result.consecutiveWrong)
    }

    @Test
    fun `forced updates do not apply to LEARNED review type`() {
        val result = calculateDifficulty(state(cw = 1), false, ReviewType.LEARNED, monthlyWrongCountBefore = 5)
        assertEquals(VocabularyDifficulty.EASY, result.current)
        assertEquals(2, result.consecutiveWrong)
    }

    // ---------- Normal consecutive-counter logic ----------

    @Test
    fun `three consecutive wrongs from EASY go to MEDIUM and reset counter`() {
        var s = state()
        repeat(2) {
            s = calculateDifficulty(s, false, ReviewType.DAILY, 0)
            assertEquals(VocabularyDifficulty.EASY, s.current)
        }
        s = calculateDifficulty(s, false, ReviewType.DAILY, 0)
        assertEquals(VocabularyDifficulty.MEDIUM, s.current)
        assertEquals(0, s.consecutiveWrong)
    }

    @Test
    fun `three consecutive corrects from MEDIUM go to EASY`() {
        var s = state(current = VocabularyDifficulty.MEDIUM)
        repeat(2) { s = calculateDifficulty(s, true, ReviewType.DAILY, 0) }
        s = calculateDifficulty(s, true, ReviewType.DAILY, 0)
        assertEquals(VocabularyDifficulty.EASY, s.current)
        assertEquals(0, s.consecutiveCorrect)
    }

    @Test
    fun `opposite answer zeros the previous counter (broken correct streak)`() {
        var s = state(cc = 2)
        s = calculateDifficulty(s, false, ReviewType.DAILY, 0)
        assertEquals(0, s.consecutiveCorrect)
        assertEquals(1, s.consecutiveWrong)
        assertEquals(VocabularyDifficulty.EASY, s.current)
    }

    @Test
    fun `opposite answer zeros the previous counter (broken wrong streak)`() {
        var s = state(cw = 2)
        s = calculateDifficulty(s, true, ReviewType.DAILY, 0)
        assertEquals(0, s.consecutiveWrong)
        assertEquals(1, s.consecutiveCorrect)
        assertEquals(VocabularyDifficulty.EASY, s.current)
    }

    @Test
    fun `threshold not reached keeps level and increments counter`() {
        val s = calculateDifficulty(state(cw = 1), false, ReviewType.DAILY, 0)
        assertEquals(VocabularyDifficulty.EASY, s.current)
        assertEquals(2, s.consecutiveWrong)
    }

    @Test
    fun `custom threshold of 2 changes level after two consecutive wrongs`() {
        var s = state()
        s = calculateDifficulty(s, false, ReviewType.DAILY, 0, threshold = 2)
        assertEquals(VocabularyDifficulty.EASY, s.current)
        s = calculateDifficulty(s, false, ReviewType.DAILY, 0, threshold = 2)
        assertEquals(VocabularyDifficulty.MEDIUM, s.current)
    }

    // ---------- Floor / ceiling ----------

    @Test
    fun `EASY floor - three consecutive corrects at EASY stay EASY`() {
        var s = state(current = VocabularyDifficulty.EASY)
        repeat(3) { s = calculateDifficulty(s, true, ReviewType.DAILY, 0) }
        assertEquals(VocabularyDifficulty.EASY, s.current)
        assertEquals(0, s.consecutiveCorrect)
    }

    @Test
    fun `VERY_HARD ceiling - three consecutive wrongs at VERY_HARD stay VERY_HARD`() {
        var s = state(current = VocabularyDifficulty.VERY_HARD, reached = true)
        repeat(3) { s = calculateDifficulty(s, false, ReviewType.DAILY, 0) }
        assertEquals(VocabularyDifficulty.VERY_HARD, s.current)
    }

    // ---------- hasReachedVeryHard invariant ----------

    @Test
    fun `hasReachedVeryHard is monotonic - stays true after later decrease`() {
        var s = state(current = VocabularyDifficulty.HARD, cw = 2)
        s = calculateDifficulty(s, false, ReviewType.DAILY, 0) // -> VERY_HARD
        assertEquals(VocabularyDifficulty.VERY_HARD, s.current)
        assertTrue(s.hasReachedVeryHard)

        s = calculateDifficulty(s.copy(consecutiveCorrect = 2), true, ReviewType.DAILY, 0) // -> HARD
        assertEquals(VocabularyDifficulty.HARD, s.current)
        assertTrue(s.hasReachedVeryHard, "flag must remain true even after Difficulty decreases")
    }

    // ---------- General invariants ----------

    @Test
    fun `consecutiveCorrect and consecutiveWrong are never both greater than zero`() {
        var s = state()
        val sequence = listOf(true, true, false, true, false, false, false, true)
        for (answer in sequence) {
            s = calculateDifficulty(s, answer, ReviewType.DAILY, 0)
            assertFalse(s.consecutiveCorrect > 0 && s.consecutiveWrong > 0)
        }
    }

    @Test
    fun `deterministic - same inputs always produce the same output`() {
        val input = state(current = VocabularyDifficulty.MEDIUM, cw = 1)
        val a = calculateDifficulty(input, false, ReviewType.WEEKLY, monthlyWrongCountBefore = 0)
        val b = calculateDifficulty(input, false, ReviewType.WEEKLY, monthlyWrongCountBefore = 0)
        assertEquals(a, b)
    }
}
