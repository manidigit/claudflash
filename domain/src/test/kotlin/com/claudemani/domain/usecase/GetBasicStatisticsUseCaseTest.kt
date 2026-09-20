package com.claudemani.domain.usecase

import com.claudemani.domain.model.Concept
import com.claudemani.domain.model.EntryType
import com.claudemani.domain.model.LearningState
import com.claudemani.domain.model.ReviewHistory
import com.claudemani.domain.model.ReviewType
import com.claudemani.domain.model.Stage
import com.claudemani.domain.repository.FakeConceptRepository
import com.claudemani.domain.repository.FakeLearningStateRepository
import com.claudemani.domain.repository.FakeReviewHistoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class GetBasicStatisticsUseCaseTest {

    private val now = Instant.parse("2026-09-14T10:00:00Z")

    private class Fixture {
        val concepts = FakeConceptRepository()
        val reviewHistories = FakeReviewHistoryRepository()
        val learningStates = FakeLearningStateRepository()

        val useCase = GetBasicStatisticsUseCase(concepts, reviewHistories, learningStates)

        suspend fun addConcept(stage: Stage, active: Boolean = true): UUID {
            val id = UUID.randomUUID()
            concepts.insert(
                Concept(id = id, entryType = EntryType.WORD, categoryId = null, favorite = false, active = active, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)
            )
            learningStates.upsert(
                LearningState(
                    id = UUID.randomUUID(), conceptId = id, stage = stage, nextReviewAt = null,
                    monthlyWrongCount = 0, hasPathFailure = false, totalCorrect = 0, totalWrong = 0, lastReviewedAt = null
                )
            )
            return id
        }

        suspend fun addReview(conceptId: UUID) {
            reviewHistories.insert(
                ReviewHistory(
                    id = UUID.randomUUID(), sessionId = UUID.randomUUID(), reviewAttemptId = UUID.randomUUID(),
                    conceptId = conceptId, reviewedAt = Instant.EPOCH, isCorrect = true, reviewType = ReviewType.DAILY
                )
            )
        }
    }

    @Test
    fun `all zero when there are no active concepts`() = runTest {
        val fx = Fixture()
        val stats = fx.useCase()
        assertEquals(BasicStatistics(0, 0, 0, 0), stats)
    }

    @Test
    fun `counts totals, practiced, unpracticed and learned correctly`() = runTest {
        val fx = Fixture()
        val learned = fx.addConcept(Stage.LEARNED)
        val daily = fx.addConcept(Stage.DAILY)
        val weekly = fx.addConcept(Stage.WEEKLY)
        fx.addReview(learned)
        fx.addReview(daily)
        // weekly has never been reviewed -> unpracticed

        val stats = fx.useCase()
        assertEquals(3, stats.totalActiveWords)
        assertEquals(2, stats.practicedWords)
        assertEquals(1, stats.unpracticedWords)
        assertEquals(1, stats.learnedWords)
    }

    @Test
    fun `soft-deleted concepts are excluded from every metric`() = runTest {
        val fx = Fixture()
        val active = fx.addConcept(Stage.LEARNED)
        val deleted = fx.addConcept(Stage.LEARNED, active = false)
        fx.addReview(active)
        fx.addReview(deleted)

        val stats = fx.useCase()
        assertEquals(1, stats.totalActiveWords)
        assertEquals(1, stats.practicedWords)
        assertEquals(0, stats.unpracticedWords)
        assertEquals(1, stats.learnedWords)
    }
}
