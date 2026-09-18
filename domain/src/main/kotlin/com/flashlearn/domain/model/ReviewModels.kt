package com.flashlearn.domain.model

import java.time.Instant
import java.util.UUID

/**
 * A single review sitting. May remain with [endedAt] == null if the user
 * exits mid-session — accepted answers are still preserved (Session Exit
 * decision, Descriptions §C, V1 FROZEN). Resuming an exact queue position
 * is explicitly not required in V1.
 */
data class ReviewSession(
    val id: UUID,
    val startedAt: Instant,
    val endedAt: Instant?,
    val reviewType: ReviewType
)

/**
 * Append-only record of one accepted answer. Never updated or deleted.
 * Unique on (sessionId, reviewAttemptId) — this is how duplicate submits
 * (double-tap) are rejected.
 */
data class ReviewHistory(
    val id: UUID,
    val sessionId: UUID,
    val reviewAttemptId: UUID,
    val conceptId: UUID,
    val reviewedAt: Instant,
    val isCorrect: Boolean,
    val reviewType: ReviewType
)
