package com.flashlearn.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * UNIQUE(conceptId) — exactly one LearningState per Concept.
 * INDEX(stage, nextReviewAt) — the Scheduler's primary due-query index
 * (Descriptions §16.1).
 *
 * HARD RULE: no `difficulty`/`consecutiveCorrect`/`consecutiveWrong`
 * columns here — those live only in [DifficultyStateEntity].
 */
@Entity(
    tableName = "learning_states",
    indices = [
        Index(value = ["conceptId"], unique = true),
        Index(value = ["stage", "nextReviewAt"])
    ]
)
data class LearningStateEntity(
    @PrimaryKey val id: UUID,
    val conceptId: UUID,
    val stage: String,
    val nextReviewAt: Instant?,
    val monthlyWrongCount: Int,
    val hasPathFailure: Boolean,
    val totalCorrect: Int,
    val totalWrong: Int,
    val lastReviewedAt: Instant?
)
