package com.claudemani.domain.usecase

import com.claudemani.domain.model.AchievementType
import com.claudemani.domain.model.Concept
import com.claudemani.domain.model.DifficultyState
import com.claudemani.domain.model.EntryType
import com.claudemani.domain.model.LearningState
import com.claudemani.domain.model.ReviewHistory
import com.claudemani.domain.model.ReviewType
import com.claudemani.domain.model.Stage
import com.claudemani.domain.model.VocabularyDifficulty
import com.claudemani.domain.repository.FakeAchievementRepository
import com.claudemani.domain.repository.FakeConceptRepository
import com.claudemani.domain.repository.FakeDifficultyStateRepository
import com.claudemani.domain.repository.FakeLearningStateRepository
import com.claudemani.domain.repository.FakeReviewHistoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class CheckAndUnlockAchievementsUseCaseTest {

    private val zone = ZoneOffset.UTC
    private val now = Instant.parse("2026-09-14T12:00:00Z")

    private class Fixture {
        val concepts = FakeConceptRepository()
        val learningStates = FakeLearningStateRepository()
        val difficultyStates = FakeDifficultyStateRepository()
        val reviewHistories = FakeReviewHistoryRepository()
        val achievements = FakeAchievementRepository()

        val useCase = CheckAndUnlockAchievementsUseCase(
            getBasicStatistics = GetBasicStatisticsUseCase(concepts, reviewHistories, learningStates),
            calculateStreak = CalculateStreakUseCase(reviewHistories),
            conceptRepository = concepts,
            learningStateRepository = learningStates,
            difficultyStateRepository = difficultyStates,
            reviewHistoryRepository = reviewHistories,
            achievementRepository = achievements
        )

        suspend fun addConcept(
            stage: Stage = Stage.DAILY,
            difficulty: VocabularyDifficulty = VocabularyDifficulty.EASY,
            hasReachedVeryHard: Boolean = false,
            practiced: Boolean = false
        ): UUID {
            val id = UUID.randomUUID()
            concepts.insert(
                Concept(id = id, entryType = EntryType.WORD, categoryId = null, favorite = false, active = true, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)
            )
            learningStates.upsert(
                LearningState(
                    id = UUID.randomUUID(), conceptId = id, stage = stage, nextReviewAt = null,
                    monthlyWrongCount = 0, hasPathFailure = false, totalCorrect = 0, totalWrong = 0, lastReviewedAt = null
                )
            )
            difficultyStates.upsert(
                DifficultyState(id = UUID.randomUUID(), conceptId = id, current = difficulty, consecutiveCorrect = 0, consecutiveWrong = 0, hasReachedVeryHard = hasReachedVeryHard)
            )
            if (practiced) {
                reviewHistories.insert(
                    ReviewHistory(
                        id = UUID.randomUUID(), sessionId = UUID.randomUUID(), reviewAttemptId = UUID.randomUUID(),
                        conceptId = id, reviewedAt = Instant.EPOCH, isCorrect = true, reviewType = ReviewType.DAILY
                    )
                )
            }
            return id
        }

        suspend fun addMonthlyCorrectReview(conceptId: UUID) {
            reviewHistories.insert(
                ReviewHistory(
                    id = UUID.randomUUID(), sessionId = UUID.randomUUID(), reviewAttemptId = UUID.randomUUID(),
                    conceptId = conceptId, reviewedAt = Instant.EPOCH, isCorrect = true, reviewType = ReviewType.MONTHLY
                )
            )
        }
    }

    @Test
    fun `unlocks FIRST_TEN_WORDS once at least 10 words have been practiced`() = runTest {
        val fx = Fixture()
        repeat(9) { fx.addConcept(practiced = true) }
        assertTrue(fx.useCase(now, zone).none { it.type == AchievementType.FIRST_TEN_WORDS })

        fx.addConcept(practiced = true)
        val unlocked = fx.useCase(now, zone)
        assertTrue(unlocked.any { it.type == AchievementType.FIRST_TEN_WORDS })
    }

    @Test
    fun `already-unlocked achievements are never returned again`() = runTest {
        val fx = Fixture()
        repeat(10) { fx.addConcept(practiced = true) }
        val first = fx.useCase(now, zone)
        assertTrue(first.any { it.type == AchievementType.FIRST_TEN_WORDS })

        val second = fx.useCase(now, zone)
        assertTrue(second.none { it.type == AchievementType.FIRST_TEN_WORDS })
    }

    @Test
    fun `unlocks streak achievements based on CalculateStreak`() = runTest {
        val fx = Fixture()
        repeat(7) { daysAgo ->
            fx.reviewHistories.insert(
                ReviewHistory(
                    id = UUID.randomUUID(), sessionId = UUID.randomUUID(), reviewAttemptId = UUID.randomUUID(),
                    conceptId = UUID.randomUUID(),
                    reviewedAt = now.minusSeconds(daysAgo.toLong() * 86400),
                    isCorrect = true, reviewType = ReviewType.DAILY
                )
            )
        }
        val unlocked = fx.useCase(now, zone)
        assertTrue(unlocked.any { it.type == AchievementType.SEVEN_DAY_STREAK })
        assertFalse(unlocked.any { it.type == AchievementType.THIRTY_DAY_STREAK })
    }

    @Test
    fun `unlocks MEMORY_BUILDER at 100 learned words`() = runTest {
        val fx = Fixture()
        repeat(100) { fx.addConcept(stage = Stage.LEARNED) }
        val unlocked = fx.useCase(now, zone)
        assertTrue(unlocked.any { it.type == AchievementType.MEMORY_BUILDER })
    }

    @Test
    fun `unlocks VOCABULARY_BUILDER at 500 active concepts`() = runTest {
        val fx = Fixture()
        repeat(500) { fx.addConcept() }
        val unlocked = fx.useCase(now, zone)
        assertTrue(unlocked.any { it.type == AchievementType.VOCABULARY_BUILDER })
    }

    @Test
    fun `unlocks HARD_MODE_MASTER at 25 mastered VERY_HARD words`() = runTest {
        val fx = Fixture()
        repeat(24) { fx.addConcept(stage = Stage.LEARNED, difficulty = VocabularyDifficulty.VERY_HARD, hasReachedVeryHard = true) }
        assertFalse(fx.useCase(now, zone).any { it.type == AchievementType.HARD_MODE_MASTER })

        fx.addConcept(stage = Stage.LEARNED, difficulty = VocabularyDifficulty.VERY_HARD, hasReachedVeryHard = true)
        assertTrue(fx.useCase(now, zone).any { it.type == AchievementType.HARD_MODE_MASTER })
    }

    @Test
    fun `HARD_MODE_MASTER requires LEARNED stage, not just hasReachedVeryHard`() = runTest {
        val fx = Fixture()
        repeat(25) { fx.addConcept(stage = Stage.DAILY, difficulty = VocabularyDifficulty.HARD, hasReachedVeryHard = true) }
        assertFalse(fx.useCase(now, zone).any { it.type == AchievementType.HARD_MODE_MASTER })
    }

    @Test
    fun `unlocks LONG_TERM_MEMORY at 50 distinct concepts with a correct MONTHLY review`() = runTest {
        val fx = Fixture()
        val ids = (1..50).map { fx.addConcept() }
        ids.forEach { fx.addMonthlyCorrectReview(it) }
        val unlocked = fx.useCase(now, zone)
        assertTrue(unlocked.any { it.type == AchievementType.LONG_TERM_MEMORY })
    }

    @Test
    fun `LONG_TERM_MEMORY counts distinct concepts, not review count`() = runTest {
        val fx = Fixture()
        val ids = (1..49).map { fx.addConcept() }
        // Same concept reviewed many times must not substitute for distinct concepts.
        repeat(10) { fx.addMonthlyCorrectReview(ids.first()) }
        ids.drop(1).forEach { fx.addMonthlyCorrectReview(it) }
        assertFalse(fx.useCase(now, zone).any { it.type == AchievementType.LONG_TERM_MEMORY })
    }
}
