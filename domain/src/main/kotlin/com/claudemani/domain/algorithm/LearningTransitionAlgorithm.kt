package com.claudemani.domain.algorithm

import com.claudemani.domain.model.LearningState
import com.claudemani.domain.model.Stage
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Learning Transition Algorithm — Version 1.1, Status: FROZEN
 * (Algorithms v4.20).
 *
 * Pure function. Does NOT touch the database and does NOT read the system
 * clock — [reviewedAt] is the single source of "now" for this call, so
 * the result is fully deterministic for a given input.
 *
 * This algorithm owns exactly: [Stage], nextReviewAt, hasPathFailure and
 * monthlyWrongCount. It has no opinion about Difficulty — that is
 * [calculateDifficulty]'s responsibility (added in Phase 4), and the two
 * must never both mutate the same field.
 *
 * Rules (§5, FROZEN):
 * - LEARNED is terminal for the normal review flow: stays LEARNED,
 *   nextReviewAt stays null, flags/counters pass through unchanged.
 * - DAILY + Correct  → WEEKLY,  nextReviewAt = reviewedAt + 7 days
 * - WEEKLY + Correct → MONTHLY, nextReviewAt = reviewedAt + 30 days
 * - MONTHLY + Correct→ LEARNED, nextReviewAt = null
 * - DAILY + Wrong    → DAILY,   nextReviewAt = start of next calendar day
 * - WEEKLY + Wrong   → DAILY,   nextReviewAt = start of next calendar day,
 *                                hasPathFailure = true
 * - MONTHLY + Wrong  → DAILY,   nextReviewAt = start of next calendar day,
 *                                hasPathFailure = true,
 *                                monthlyWrongCount = monthlyWrongCount + 1
 * - monthlyWrongCount is cumulative and NEVER reset by a successful transition.
 * - hasPathFailure, once true, is NEVER reset back to false by a normal transition.
 */
fun calculateLearningTransition(
    learningState: LearningState,
    isCorrect: Boolean,
    reviewedAt: Instant,
    zoneId: ZoneId = ZoneId.systemDefault()
): TransitionResult {
    val currentStage = learningState.stage
    val currentMonthlyWrong = learningState.monthlyWrongCount
    val currentHasPathFailure = learningState.hasPathFailure

    // LEARNED is terminal for the normal review flow.
    if (currentStage == Stage.LEARNED) {
        return TransitionResult(
            newStage = Stage.LEARNED,
            nextReviewAt = null,
            hasPathFailure = currentHasPathFailure,
            monthlyWrongCount = currentMonthlyWrong
        )
    }

    return if (isCorrect) {
        when (currentStage) {
            Stage.DAILY -> TransitionResult(
                newStage = Stage.WEEKLY,
                nextReviewAt = reviewedAt.plus(7, ChronoUnit.DAYS),
                hasPathFailure = currentHasPathFailure,
                monthlyWrongCount = currentMonthlyWrong
            )
            Stage.WEEKLY -> TransitionResult(
                newStage = Stage.MONTHLY,
                nextReviewAt = reviewedAt.plus(30, ChronoUnit.DAYS),
                hasPathFailure = currentHasPathFailure,
                monthlyWrongCount = currentMonthlyWrong
            )
            Stage.MONTHLY -> TransitionResult(
                newStage = Stage.LEARNED,
                nextReviewAt = null,
                hasPathFailure = currentHasPathFailure,
                monthlyWrongCount = currentMonthlyWrong
            )
            Stage.LEARNED -> error("unreachable: handled above")
        }
    } else {
        when (currentStage) {
            Stage.DAILY -> TransitionResult(
                newStage = Stage.DAILY,
                nextReviewAt = startOfNextCalendarDay(reviewedAt, zoneId),
                // DAILY wrong does NOT set path failure.
                hasPathFailure = currentHasPathFailure,
                monthlyWrongCount = currentMonthlyWrong
            )
            Stage.WEEKLY -> TransitionResult(
                newStage = Stage.DAILY,
                nextReviewAt = startOfNextCalendarDay(reviewedAt, zoneId),
                hasPathFailure = true,
                monthlyWrongCount = currentMonthlyWrong
            )
            Stage.MONTHLY -> TransitionResult(
                newStage = Stage.DAILY,
                nextReviewAt = startOfNextCalendarDay(reviewedAt, zoneId),
                hasPathFailure = true,
                monthlyWrongCount = currentMonthlyWrong + 1
            )
            Stage.LEARNED -> error("unreachable: handled above")
        }
    }
}

/**
 * Returns the Instant of 00:00:00 of the calendar day *after* [instant],
 * in [zoneId] (device local timezone). Used for the "next day" DAILY
 * wrong-answer rule (Algorithms v4.20 §5.1) — a wrong answer today makes
 * the word reappear tomorrow, not immediately.
 */
fun startOfNextCalendarDay(instant: Instant, zoneId: ZoneId): Instant {
    val localDate = instant.atZone(zoneId).toLocalDate()
    return localDate.plusDays(1).atStartOfDay(zoneId).toInstant()
}
