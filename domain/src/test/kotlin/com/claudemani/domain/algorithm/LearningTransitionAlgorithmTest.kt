package com.claudemani.domain.algorithm

import com.claudemani.domain.model.LearningState
import com.claudemani.domain.model.Stage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class LearningTransitionAlgorithmTest {

    private val now = Instant.parse("2026-09-10T10:00:00Z")
    private val zone = ZoneOffset.UTC

    private fun baseState(
        stage: Stage,
        monthlyWrong: Int = 0,
        pathFailure: Boolean = false
    ) = LearningState(
        id = UUID.randomUUID(),
        conceptId = UUID.randomUUID(),
        stage = stage,
        nextReviewAt = now,
        monthlyWrongCount = monthlyWrong,
        hasPathFailure = pathFailure,
        totalCorrect = 0,
        totalWrong = 0,
        lastReviewedAt = null
    )

    // ---------- Full transition table ----------

    @Test
    fun `DAILY correct goes to WEEKLY with plus 7 days`() {
        val result = calculateLearningTransition(baseState(Stage.DAILY), true, now, zone)
        assertEquals(Stage.WEEKLY, result.newStage)
        assertEquals(now.plusSeconds(7 * 86400), result.nextReviewAt)
        assertFalse(result.hasPathFailure)
        assertEquals(0, result.monthlyWrongCount)
    }

    @Test
    fun `DAILY wrong stays DAILY, next calendar day, pathFailure unchanged`() {
        val result = calculateLearningTransition(baseState(Stage.DAILY), false, now, zone)
        assertEquals(Stage.DAILY, result.newStage)
        assertEquals(startOfNextCalendarDay(now, zone), result.nextReviewAt)
        assertFalse(result.hasPathFailure)
        assertEquals(0, result.monthlyWrongCount)
    }

    @Test
    fun `WEEKLY correct goes to MONTHLY with plus 30 days`() {
        val result = calculateLearningTransition(baseState(Stage.WEEKLY), true, now, zone)
        assertEquals(Stage.MONTHLY, result.newStage)
        assertEquals(now.plusSeconds(30L * 86400), result.nextReviewAt)
        assertFalse(result.hasPathFailure)
    }

    @Test
    fun `WEEKLY wrong goes to DAILY and sets pathFailure true`() {
        val result = calculateLearningTransition(baseState(Stage.WEEKLY), false, now, zone)
        assertEquals(Stage.DAILY, result.newStage)
        assertEquals(startOfNextCalendarDay(now, zone), result.nextReviewAt)
        assertTrue(result.hasPathFailure)
        assertEquals(0, result.monthlyWrongCount)
    }

    @Test
    fun `MONTHLY correct goes to LEARNED with null nextReviewAt`() {
        val result = calculateLearningTransition(baseState(Stage.MONTHLY), true, now, zone)
        assertEquals(Stage.LEARNED, result.newStage)
        assertNull(result.nextReviewAt)
    }

    @Test
    fun `MONTHLY wrong goes to DAILY, sets pathFailure and increments monthlyWrongCount`() {
        val result = calculateLearningTransition(
            baseState(Stage.MONTHLY, monthlyWrong = 2), false, now, zone
        )
        assertEquals(Stage.DAILY, result.newStage)
        assertTrue(result.hasPathFailure)
        assertEquals(3, result.monthlyWrongCount)
    }

    @Test
    fun `LEARNED stays LEARNED regardless of answer, flags and counters pass through`() {
        val state = baseState(Stage.LEARNED, monthlyWrong = 5, pathFailure = true)
        val correct = calculateLearningTransition(state, true, now, zone)
        val wrong = calculateLearningTransition(state, false, now, zone)

        assertEquals(Stage.LEARNED, correct.newStage)
        assertEquals(Stage.LEARNED, wrong.newStage)
        assertNull(correct.nextReviewAt)
        assertNull(wrong.nextReviewAt)
        // LEARNED review must not touch flags/counters at all.
        assertTrue(correct.hasPathFailure)
        assertTrue(wrong.hasPathFailure)
        assertEquals(5, correct.monthlyWrongCount)
        assertEquals(5, wrong.monthlyWrongCount)
    }

    // ---------- Invariants ----------

    @Test
    fun `hasPathFailure once true is never reset by a successful transition`() {
        val afterWeeklyFail = baseState(Stage.DAILY, pathFailure = true)
        // DAILY correct -> WEEKLY should not clear the historical flag.
        val result = calculateLearningTransition(afterWeeklyFail, true, now, zone)
        assertTrue(result.hasPathFailure)
    }

    @Test
    fun `monthlyWrongCount is cumulative across repeated MONTHLY failures`() {
        var state = baseState(Stage.MONTHLY, monthlyWrong = 0)

        val first = calculateLearningTransition(state, false, now, zone)
        assertEquals(1, first.monthlyWrongCount)

        // Simulate climbing back to MONTHLY and failing again.
        state = state.copy(monthlyWrongCount = first.monthlyWrongCount)
        val second = calculateLearningTransition(state, false, now, zone)
        assertEquals(2, second.monthlyWrongCount)
    }

    @Test
    fun `DAILY and MONTHLY correct never change monthlyWrongCount`() {
        val monthlyState = baseState(Stage.MONTHLY, monthlyWrong = 4)
        val result = calculateLearningTransition(monthlyState, true, now, zone)
        assertEquals(4, result.monthlyWrongCount)
    }

    @Test
    fun `startOfNextCalendarDay returns midnight of the following day in the given zone`() {
        val instant = Instant.parse("2026-09-10T23:59:00Z")
        val next = startOfNextCalendarDay(instant, ZoneOffset.UTC)
        assertEquals(Instant.parse("2026-09-11T00:00:00Z"), next)
    }

    @Test
    fun `startOfNextCalendarDay respects a non-UTC zone`() {
        // 23:30 UTC on 2026-09-10 is already 03:00 on 2026-09-11 in +03:30 (Tehran-like offset).
        val tehranZone = ZoneOffset.ofHoursMinutes(3, 30)
        val instant = Instant.parse("2026-09-10T23:30:00Z")
        val next = startOfNextCalendarDay(instant, tehranZone)
        // Next calendar day boundary in +03:30 is 2026-09-12T00:00 local = 2026-09-11T20:30:00Z
        assertEquals(Instant.parse("2026-09-11T20:30:00Z"), next)
    }
}
