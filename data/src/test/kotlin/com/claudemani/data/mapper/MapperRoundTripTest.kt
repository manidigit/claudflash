package com.claudemani.data.mapper

import com.claudemani.domain.model.Achievement
import com.claudemani.domain.model.AchievementType
import com.claudemani.domain.model.Concept
import com.claudemani.domain.model.DifficultyState
import com.claudemani.domain.model.EntryType
import com.claudemani.domain.model.LearningState
import com.claudemani.domain.model.ReviewHistory
import com.claudemani.domain.model.ReviewSession
import com.claudemani.domain.model.ReviewType
import com.claudemani.domain.model.Stage
import com.claudemani.domain.model.VocabularyDifficulty
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.UUID

/**
 * Every enum used on an Entity is stored as a raw String (`.name`) and
 * parsed back with `.valueOf(...)`. These tests exist specifically to
 * catch a typo or mismatch between the two directions, for every enum in
 * the schema — cheap insurance against a runtime IllegalArgumentException
 * from Room reading back an old row.
 */
class MapperRoundTripTest {

    @Test
    fun `Concept round-trips through every EntryType`() {
        for (entryType in EntryType.entries) {
            val concept = Concept(
                id = UUID.randomUUID(),
                entryType = entryType,
                categoryId = UUID.randomUUID(),
                favorite = true,
                active = true,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                dataVersion = 2
            )
            val roundTripped = ConceptMapper.toDomain(ConceptMapper.toEntity(concept))
            assertEquals(concept, roundTripped)
        }
    }

    @Test
    fun `LearningState round-trips through every Stage`() {
        for (stage in Stage.entries) {
            val state = LearningState(
                id = UUID.randomUUID(),
                conceptId = UUID.randomUUID(),
                stage = stage,
                nextReviewAt = if (stage == Stage.LEARNED) null else Instant.now(),
                monthlyWrongCount = 3,
                hasPathFailure = true,
                totalCorrect = 5,
                totalWrong = 2,
                lastReviewedAt = Instant.now()
            )
            val roundTripped = LearningStateMapper.toDomain(LearningStateMapper.toEntity(state))
            assertEquals(state, roundTripped)
        }
    }

    @Test
    fun `DifficultyState round-trips through every VocabularyDifficulty`() {
        for (difficulty in VocabularyDifficulty.entries) {
            val state = DifficultyState(
                id = UUID.randomUUID(),
                conceptId = UUID.randomUUID(),
                current = difficulty,
                consecutiveCorrect = 1,
                consecutiveWrong = 0,
                hasReachedVeryHard = difficulty == VocabularyDifficulty.VERY_HARD
            )
            val roundTripped = DifficultyStateMapper.toDomain(DifficultyStateMapper.toEntity(state))
            assertEquals(state, roundTripped)
        }
    }

    @Test
    fun `ReviewHistory and ReviewSession round-trip through every ReviewType`() {
        for (reviewType in ReviewType.entries) {
            val history = ReviewHistory(
                id = UUID.randomUUID(),
                sessionId = UUID.randomUUID(),
                reviewAttemptId = UUID.randomUUID(),
                conceptId = UUID.randomUUID(),
                reviewedAt = Instant.now(),
                isCorrect = true,
                reviewType = reviewType
            )
            assertEquals(history, ReviewHistoryMapper.toDomain(ReviewHistoryMapper.toEntity(history)))

            val session = ReviewSession(
                id = UUID.randomUUID(),
                startedAt = Instant.now(),
                endedAt = null,
                reviewType = reviewType
            )
            assertEquals(session, ReviewSessionMapper.toDomain(ReviewSessionMapper.toEntity(session)))
        }
    }

    @Test
    fun `Achievement round-trips through every AchievementType`() {
        for (type in AchievementType.entries) {
            val achievement = Achievement(type = type, isUnlocked = true, unlockedAt = Instant.now())
            assertEquals(achievement, AchievementMapper.toDomain(AchievementMapper.toEntity(achievement)))
        }
    }
}
