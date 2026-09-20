package com.claudemani.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Owns the learning-path side of a Concept: [stage], [nextReviewAt],
 * cumulative [monthlyWrongCount] and the historical [hasPathFailure]
 * flag, plus review totals.
 *
 * HARD RULE (Data Model Canonical v1.1, FROZEN — do not violate):
 * LearningState must NEVER contain a `difficulty`, `consecutiveCorrect`
 * or `consecutiveWrong` field. Those belong exclusively to
 * [DifficultyState]. This separation must hold even though both algorithms
 * run inside the same SubmitReviewAnswer transaction.
 *
 * - [monthlyWrongCount] increments only on MONTHLY+Wrong; never reset.
 * - [hasPathFailure] becomes true only after a WEEKLY or MONTHLY failure;
 *   never reset by normal review.
 * - [nextReviewAt] is null exactly when [stage] == LEARNED.
 */
data class LearningState(
    val id: UUID,
    val conceptId: UUID,
    val stage: Stage,
    val nextReviewAt: Instant?,
    val monthlyWrongCount: Int,
    val hasPathFailure: Boolean,
    val totalCorrect: Int,
    val totalWrong: Int,
    val lastReviewedAt: Instant?
)
