package com.flashlearn.domain.repository

import com.flashlearn.domain.algorithm.calculateDifficulty
import com.flashlearn.domain.algorithm.calculateLearningTransition
import com.flashlearn.domain.model.Concept
import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.model.EntryType
import com.flashlearn.domain.model.LearningState
import com.flashlearn.domain.model.ReviewHistory
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.model.Stage
import com.flashlearn.domain.model.VocabularyDifficulty
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Not a UseCase test (UseCases arrive in Phase 10/11) — this is a
 * rehearsal proving the repository interface shapes defined in this
 * phase are actually sufficient to implement CreateConcept +
 * SubmitReviewAnswer against, before any Room code exists.
 */
class RepositoryContractRehearsalTest {

    @Test
    fun `create concept then submit one correct DAILY answer, end to end via fakes`() = runTest {
        val db = FakeFlashLearnDatabase()
        val concepts = FakeConceptRepository()
        val learningStates = FakeLearningStateRepository()
        val difficultyStates = FakeDifficultyStateRepository()
        val history = FakeReviewHistoryRepository()

        val now = Instant.parse("2026-09-13T08:00:00Z")
        val conceptId = UUID.randomUUID()

        // ---- CreateConcept rehearsal (atomic init: DAILY + EASY) ----
        db.withTransaction {
            concepts.insert(
                Concept(
                    id = conceptId,
                    entryType = EntryType.WORD,
                    categoryId = null,
                    favorite = false,
                    active = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
            learningStates.upsert(
                LearningState(
                    id = UUID.randomUUID(),
                    conceptId = conceptId,
                    stage = Stage.DAILY,
                    nextReviewAt = null,
                    monthlyWrongCount = 0,
                    hasPathFailure = false,
                    totalCorrect = 0,
                    totalWrong = 0,
                    lastReviewedAt = null
                )
            )
            difficultyStates.upsert(
                DifficultyState(
                    id = UUID.randomUUID(),
                    conceptId = conceptId,
                    current = VocabularyDifficulty.EASY,
                    consecutiveCorrect = 0,
                    consecutiveWrong = 0,
                    hasReachedVeryHard = false
                )
            )
        }

        assertNotNull(concepts.getById(conceptId))
        assertEquals(Stage.DAILY, learningStates.get(conceptId)?.stage)

        // ---- SubmitReviewAnswer rehearsal (one correct DAILY answer) ----
        val sessionId = UUID.randomUUID()
        val attemptId = UUID.randomUUID()

        db.withTransaction {
            val learning = learningStates.get(conceptId) ?: error("DATA_INTEGRITY_ERROR")
            val difficulty = difficultyStates.get(conceptId) ?: error("DATA_INTEGRITY_ERROR")

            assertTrue(!history.existsByAttemptId(sessionId, attemptId))

            val transition = calculateLearningTransition(learning, isCorrect = true, reviewedAt = now)
            val newDifficulty = calculateDifficulty(
                difficulty, isCorrect = true, reviewType = ReviewType.DAILY,
                monthlyWrongCountBefore = learning.monthlyWrongCount
            )

            learningStates.upsert(
                learning.copy(
                    stage = transition.newStage,
                    nextReviewAt = transition.nextReviewAt,
                    hasPathFailure = transition.hasPathFailure,
                    monthlyWrongCount = transition.monthlyWrongCount,
                    totalCorrect = learning.totalCorrect + 1,
                    lastReviewedAt = now
                )
            )
            difficultyStates.upsert(newDifficulty)
            history.insert(
                ReviewHistory(
                    id = UUID.randomUUID(),
                    sessionId = sessionId,
                    reviewAttemptId = attemptId,
                    conceptId = conceptId,
                    reviewedAt = now,
                    isCorrect = true,
                    reviewType = ReviewType.DAILY
                )
            )
        }

        val updatedLearning = learningStates.get(conceptId)
        assertEquals(Stage.WEEKLY, updatedLearning?.stage)
        assertEquals(1, updatedLearning?.totalCorrect)
        assertTrue(history.existsByAttemptId(sessionId, attemptId))
        assertEquals(listOf(conceptId), history.getDistinctConceptIds())
    }
}
