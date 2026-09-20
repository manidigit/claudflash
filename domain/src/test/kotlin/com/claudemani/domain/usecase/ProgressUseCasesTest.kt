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

class ProgressUseCasesTest {

    private val now = Instant.parse("2026-09-14T12:00:00Z")

    private class Fixture {
        val concepts = FakeConceptRepository()
        val learningStates = FakeLearningStateRepository()
        val reviewHistories = FakeReviewHistoryRepository()

        val getSummary = GetProgressSummaryUseCase(concepts, learningStates)
        val getPercentage = CalculateProgressPercentageUseCase(concepts, learningStates, reviewHistories)

        suspend fun addConcept(
            stage: Stage?,
            nextReviewAt: Instant? = null,
            totalCorrect: Int = 0,
            totalWrong: Int = 0,
            active: Boolean = true,
            practiced: Boolean = false
        ): UUID {
            val id = UUID.randomUUID()
            concepts.insert(
                Concept(id = id, entryType = EntryType.WORD, categoryId = null, favorite = false, active = active, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)
            )
            if (stage != null) {
                learningStates.upsert(
                    LearningState(
                        id = UUID.randomUUID(), conceptId = id, stage = stage, nextReviewAt = nextReviewAt,
                        monthlyWrongCount = 0, hasPathFailure = false, totalCorrect = totalCorrect, totalWrong = totalWrong, lastReviewedAt = null
                    )
                )
            }
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
    }

    @Test
    fun `progress summary is all-zero with no active concepts`() = runTest {
        val fx = Fixture()
        val summary = fx.getSummary(now)
        assertEquals(0, summary.activeConceptCount)
        assertEquals(0.0, summary.accuracyPercentage)
    }

    @Test
    fun `progress summary aggregates due, learned, correct, wrong and accuracy`() = runTest {
        val fx = Fixture()
        fx.addConcept(Stage.LEARNED, totalCorrect = 3, totalWrong = 1)
        fx.addConcept(Stage.DAILY, nextReviewAt = now.minusSeconds(60), totalCorrect = 1, totalWrong = 0)
        fx.addConcept(Stage.WEEKLY, nextReviewAt = now.plusSeconds(60), totalCorrect = 0, totalWrong = 0) // not due yet

        val summary = fx.getSummary(now)
        assertEquals(3, summary.activeConceptCount)
        assertEquals(1, summary.learnedCount)
        assertEquals(1, summary.dueCount) // only the DAILY one is due
        assertEquals(4, summary.totalCorrect)
        assertEquals(1, summary.totalWrong)
        assertEquals(80.0, summary.accuracyPercentage)
    }

    @Test
    fun `progress summary ignores soft-deleted concepts`() = runTest {
        val fx = Fixture()
        fx.addConcept(Stage.LEARNED, totalCorrect = 5, totalWrong = 0, active = false)
        fx.addConcept(Stage.DAILY, totalCorrect = 1, totalWrong = 1)

        val summary = fx.getSummary(now)
        assertEquals(1, summary.activeConceptCount)
        assertEquals(0, summary.learnedCount)
        assertEquals(1, summary.totalCorrect)
    }

    @Test
    fun `progress percentage is 0 with no active concepts`() = runTest {
        val fx = Fixture()
        assertEquals(0.0, fx.getPercentage())
    }

    @Test
    fun `progress percentage scores each stage per the table`() = runTest {
        val fx = Fixture()
        fx.addConcept(Stage.LEARNED) // 100
        fx.addConcept(Stage.MONTHLY) // 80
        fx.addConcept(Stage.WEEKLY) // 60
        fx.addConcept(Stage.DAILY, practiced = true) // 15

        val percentage = fx.getPercentage()
        assertEquals((100 + 80 + 60 + 15) / 4.0, percentage)
    }

    @Test
    fun `fresh DAILY words that were never reviewed score 0 percent`() = runTest {
        val fx = Fixture()
        fx.addConcept(Stage.DAILY)
        fx.addConcept(Stage.DAILY)

        assertEquals(0.0, fx.getPercentage())
    }

    @Test
    fun `progress percentage gives 15 for practiced concepts with no learning state and 0 otherwise`() = runTest {
        val fx = Fixture()
        fx.addConcept(stage = null, practiced = true) // 15
        fx.addConcept(stage = null, practiced = false) // 0

        val percentage = fx.getPercentage()
        assertEquals((15 + 0) / 2.0, percentage)
    }
}
