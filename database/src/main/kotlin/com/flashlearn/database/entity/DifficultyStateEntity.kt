package com.flashlearn.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** UNIQUE(conceptId) — exactly one DifficultyState per Concept, independent of LearningState. */
@Entity(
    tableName = "difficulty_states",
    indices = [Index(value = ["conceptId"], unique = true)]
)
data class DifficultyStateEntity(
    @PrimaryKey val id: UUID,
    val conceptId: UUID,
    val current: String,
    val consecutiveCorrect: Int,
    val consecutiveWrong: Int,
    val hasReachedVeryHard: Boolean
)
