package com.claudemani.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/** May remain with endedAt = null if the session was exited mid-way (Session Exit decision, V1 FROZEN). */
@Entity(tableName = "review_sessions")
data class ReviewSessionEntity(
    @PrimaryKey val id: UUID,
    val startedAt: Instant,
    val endedAt: Instant?,
    val reviewType: String
)

/**
 * Append-only — no update()/delete() DAO method will ever be added for
 * this entity. UNIQUE(sessionId, reviewAttemptId) is how a duplicate
 * submit (double-tap) is rejected.
 */
@Entity(
    tableName = "review_history",
    indices = [
        Index(value = ["sessionId", "reviewAttemptId"], unique = true),
        Index(value = ["conceptId"])
    ]
)
data class ReviewHistoryEntity(
    @PrimaryKey val id: UUID,
    val sessionId: UUID,
    val reviewAttemptId: UUID,
    val conceptId: UUID,
    val reviewedAt: Instant,
    val isCorrect: Boolean,
    val reviewType: String
)
