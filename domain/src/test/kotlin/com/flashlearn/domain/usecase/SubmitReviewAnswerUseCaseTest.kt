package com.flashlearn.domain.usecase

import com.flashlearn.domain.exception.DataIntegrityException
import com.flashlearn.domain.exception.DuplicateReviewAttemptException
import com.flashlearn.domain.exception.ReviewNotDueException
import com.flashlearn.domain.model.AppSetting
import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.model.LearningState
import com.flashlearn.domain.model.ReviewHistory
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.model.SettingKeys
import com.flashlearn.domain.model.Stage
import com.flashlearn.domain.model.VocabularyDifficulty
import com.flashlearn.domain.repository.FakeDifficultyStateRepository
import com.flashlearn.domain.repository.FakeFlashLearnDatabase
import com.flashlearn.domain.repository.FakeLearningStateRepository
import com.flashlearn.domain.repository.FakeReviewHistoryRepository
import com.flashlearn.domain.repository.FakeSettingsRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class SubmitReviewAnswerUseCaseTest {

    private val now = Instant.parse("2026-09-13T10:00:00Z")

    private class Fixture {
        val learningStates = FakeLearningStateRepository()
        val difficultyStates = FakeDifficultyStateRepository()
        val history = FakeReviewHistoryRepository()
        val settings = FakeSettingsRepository()

        val useCase = SubmitReviewAnswerUseCase(
            learningStateRepository = learningStates,
            difficultyStateRepository = difficultyStates,
            reviewHistoryRepository = history,
            settingsRepository = settings,
            database = FakeFlashLearnDatabase()
        )

        suspend fun seed(
            conceptId: UUID,
            stage: Stage,
            nextReviewAt: Instant?,
            monthlyWrongCount: Int = 0,
            hasPathFailure: Boolean = false,
            difficulty: VocabularyDifficulty = VocabularyDifficulty.EASY,
            consecutiveCorrect: Int = 0,
            consecutiveWrong: Int = 0,
            hasReachedVeryHard: Boolean = false
        ) {
            learningStates.upsert(
                LearningState(
                    id = UUID.randomUUID(),
                    conceptId = conceptId,
                    stage = stage,
                    nextReviewAt = nextReviewAt,
                    monthlyWrongCount = monthlyWrongCount,
                    hasPathFailure = hasPathFailure,
                    totalCorrect = 0,
                    totalWrong = 0,
                    lastReviewedAt = null
                )
            )
            difficultyStates.upsert(
                DifficultyState(
                    id = UUID.randomUUID(),
                    conceptId = conceptId,
                    current = difficulty,
                    consecutiveCorrect = consecutiveCorrect,
                    consecutiveWrong = consecutiveWrong,
                    hasReachedVeryHard = hasReachedVeryHard
                )
            )
        }
    }

    private val dueNow = { now.minusSeconds(60) }

    @Test
    fun `happy path - DAILY correct moves to WEEKLY and increments totals`() = runTest {
        val f = Fixture()
        val conceptId = UUID.randomUUID()
        f.seed(conceptId, Stage.DAILY, dueNow())

        val result = f.useCase(
            SubmitReviewAnswerRequest(
                conceptId = conceptId,
                sessionId = UUID.randomUUID(),
                reviewAttemptId = UUID.randomUUID(),
                reviewType = ReviewType.DAILY,
                isCorrect = true,
                reviewedAt = now
            )
        )

        assertEquals(Stage.WEEKLY, result.learningState.stage)
        assertEquals(1, result.learningState.totalCorrect)
        assertEquals(0, result.learningState.totalWrong)
        assertEquals(now, result.learningState.lastReviewedAt)
    }

    @Test
    fun `missing LearningState throws DataIntegrityException`() {
        val f = Fixture()
        val conceptId = UUID.randomUUID()
        assertThrows(DataIntegrityException::class.java) {
            runBlocking {
                f.difficultyStates.upsert(
                    DifficultyState(UUID.randomUUID(), conceptId, VocabularyDifficulty.EASY, 0, 0, false)
                )
                f.useCase(
                    SubmitReviewAnswerRequest(
                        conceptId, UUID.randomUUID(), UUID.randomUUID(), ReviewType.DAILY, true, now
                    )
                )
            }
        }
    }

    @Test
    fun `missing DifficultyState throws DataIntegrityException`() {
        val f = Fixture()
        val conceptId = UUID.randomUUID()
        assertThrows(DataIntegrityException::class.java) {
            runBlocking {
                f.learningStates.upsert(
                    LearningState(UUID.randomUUID(), conceptId, Stage.DAILY, dueNow(), 0, false, 0, 0, null)
                )
                f.useCase(
                    SubmitReviewAnswerRequest(
                        conceptId, UUID.randomUUID(), UUID.randomUUID(), ReviewType.DAILY, true, now
                    )
                )
            }
        }
    }

    @Test
    fun `duplicate attempt is rejected even when the concept is not due`() {
        // Proves the v4.10 ordering: duplicate check runs BEFORE due
        // validation, so a duplicate is always reported as a duplicate,
        // never masked by a "not due" error.
        val f = Fixture()
        val conceptId = UUID.randomUUID()
        val sessionId = UUID.randomUUID()
        val attemptId = UUID.randomUUID()

        val ex = assertThrows(DuplicateReviewAttemptException::class.java) {
            runBlocking {
                // nextReviewAt in the future -> would normally fail due validation.
                f.seed(conceptId, Stage.DAILY, now.plusSeconds(3600))
                f.history.insert(
                    ReviewHistory(
                        id = UUID.randomUUID(),
                        sessionId = sessionId,
                        reviewAttemptId = attemptId,
                        conceptId = conceptId,
                        reviewedAt = now.minusSeconds(10),
                        isCorrect = true,
                        reviewType = ReviewType.DAILY
                    )
                )
                f.useCase(
                    SubmitReviewAnswerRequest(conceptId, sessionId, attemptId, ReviewType.DAILY, true, now)
                )
            }
        }
        assertTrue(ex.message?.contains(attemptId.toString()) == true)
    }

    @Test
    fun `not-yet-due concept is rejected when there is no duplicate`() {
        val f = Fixture()
        val conceptId = UUID.randomUUID()
        assertThrows(ReviewNotDueException::class.java) {
            runBlocking {
                f.seed(conceptId, Stage.DAILY, now.plusSeconds(3600))
                f.useCase(
                    SubmitReviewAnswerRequest(
                        conceptId, UUID.randomUUID(), UUID.randomUUID(), ReviewType.DAILY, true, now
                    )
                )
            }
        }
    }

    @Test
    fun `LEARNED review is always allowed regardless of nextReviewAt and never touches Difficulty`() = runTest {
        val f = Fixture()
        val conceptId = UUID.randomUUID()
        f.seed(
            conceptId, Stage.LEARNED, nextReviewAt = null,
            difficulty = VocabularyDifficulty.MEDIUM, consecutiveWrong = 2
        )

        val result = f.useCase(
            SubmitReviewAnswerRequest(
                conceptId, UUID.randomUUID(), UUID.randomUUID(), ReviewType.LEARNED, false, now
            )
        )

        assertEquals(Stage.LEARNED, result.learningState.stage)
        // Difficulty must pass through completely unchanged for LEARNED.
        assertEquals(VocabularyDifficulty.MEDIUM, result.difficultyState.current)
        assertEquals(2, result.difficultyState.consecutiveWrong)
    }

    @Test
    fun `WEEKLY wrong forces Difficulty to at least MEDIUM`() = runTest {
        val f = Fixture()
        val conceptId = UUID.randomUUID()
        f.seed(conceptId, Stage.WEEKLY, dueNow(), difficulty = VocabularyDifficulty.EASY)

        val result = f.useCase(
            SubmitReviewAnswerRequest(
                conceptId, UUID.randomUUID(), UUID.randomUUID(), ReviewType.WEEKLY, false, now
            )
        )

        assertEquals(Stage.DAILY, result.learningState.stage)
        assertTrue(result.learningState.hasPathFailure)
        assertEquals(VocabularyDifficulty.MEDIUM, result.difficultyState.current)
        assertEquals(0, result.difficultyState.consecutiveWrong)
    }

    @Test
    fun `MONTHLY first wrong sets HARD, second sets VERY_HARD`() = runTest {
        val f = Fixture()
        val conceptId = UUID.randomUUID()
        f.seed(conceptId, Stage.MONTHLY, dueNow(), monthlyWrongCount = 0)

        val first = f.useCase(
            SubmitReviewAnswerRequest(
                conceptId, UUID.randomUUID(), UUID.randomUUID(), ReviewType.MONTHLY, false, now
            )
        )
        assertEquals(VocabularyDifficulty.HARD, first.difficultyState.current)
        assertEquals(1, first.learningState.monthlyWrongCount)

        // Climb back to MONTHLY and fail again.
        f.learningStates.upsert(first.learningState.copy(stage = Stage.MONTHLY, nextReviewAt = dueNow()))
        val second = f.useCase(
            SubmitReviewAnswerRequest(
                conceptId, UUID.randomUUID(), UUID.randomUUID(), ReviewType.MONTHLY, false, now
            )
        )
        assertEquals(VocabularyDifficulty.VERY_HARD, second.difficultyState.current)
        assertEquals(2, second.learningState.monthlyWrongCount)
        assertTrue(second.difficultyState.hasReachedVeryHard)
    }

    @Test
    fun `threshold defaults to 3 when Settings has no value configured`() = runTest {
        val f = Fixture()
        val conceptId = UUID.randomUUID()
        f.seed(conceptId, Stage.DAILY, dueNow(), difficulty = VocabularyDifficulty.EASY, consecutiveWrong = 2)

        val result = f.useCase(
            SubmitReviewAnswerRequest(
                conceptId, UUID.randomUUID(), UUID.randomUUID(), ReviewType.DAILY, false, now
            )
        )
        // Third consecutive wrong with default threshold 3 -> escalates.
        assertEquals(VocabularyDifficulty.MEDIUM, result.difficultyState.current)
    }

    @Test
    fun `custom threshold from Settings is honored`() = runTest {
        val f = Fixture()
        f.settings.put(AppSetting(SettingKeys.THRESHOLD_DIFFICULTY, "2", now))
        val conceptId = UUID.randomUUID()
        f.seed(conceptId, Stage.DAILY, dueNow(), difficulty = VocabularyDifficulty.EASY, consecutiveWrong = 1)

        val result = f.useCase(
            SubmitReviewAnswerRequest(
                conceptId, UUID.randomUUID(), UUID.randomUUID(), ReviewType.DAILY, false, now
            )
        )
        // Second consecutive wrong with threshold 2 -> escalates.
        assertEquals(VocabularyDifficulty.MEDIUM, result.difficultyState.current)
    }

    @Test
    fun `ReviewHistory is recorded with the submitted fields`() = runTest {
        val f = Fixture()
        val conceptId = UUID.randomUUID()
        f.seed(conceptId, Stage.DAILY, dueNow())
        val sessionId = UUID.randomUUID()
        val attemptId = UUID.randomUUID()

        f.useCase(
            SubmitReviewAnswerRequest(conceptId, sessionId, attemptId, ReviewType.DAILY, true, now)
        )

        assertTrue(f.history.existsByAttemptId(sessionId, attemptId))
        val recorded = f.history.getByConceptId(conceptId).single()
        assertEquals(ReviewType.DAILY, recorded.reviewType)
        assertTrue(recorded.isCorrect)
        assertEquals(now, recorded.reviewedAt)
    }

    @Test
    fun `wrong answer increments totalWrong not totalCorrect and does not set pathFailure on DAILY`() = runTest {
        val f = Fixture()
        val conceptId = UUID.randomUUID()
        f.seed(conceptId, Stage.DAILY, dueNow())

        val result = f.useCase(
            SubmitReviewAnswerRequest(
                conceptId, UUID.randomUUID(), UUID.randomUUID(), ReviewType.DAILY, false, now
            )
        )
        assertEquals(0, result.learningState.totalCorrect)
        assertEquals(1, result.learningState.totalWrong)
        assertFalse(result.learningState.hasPathFailure)
    }
}
