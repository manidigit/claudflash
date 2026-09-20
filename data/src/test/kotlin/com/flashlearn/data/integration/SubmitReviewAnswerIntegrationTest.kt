package com.flashlearn.data.integration

import androidx.room.withTransaction
import com.flashlearn.domain.exception.DataIntegrityException
import com.flashlearn.domain.exception.DuplicateReviewAttemptException
import com.flashlearn.domain.model.LearningState
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.model.Stage
import com.flashlearn.domain.model.VocabularyDifficulty
import com.flashlearn.domain.usecase.CreateConceptCommand
import com.flashlearn.domain.usecase.CreateConceptUseCase
import com.flashlearn.domain.usecase.SubmitReviewAnswerRequest
import com.flashlearn.domain.usecase.SubmitReviewAnswerUseCase
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Real-Room integration test for [SubmitReviewAnswerUseCase] — the single
 * most spec-emphasized atomicity contract in the whole project
 * (Descriptions §2/§14, Algorithms v4.10 correction #5, Phase 6 Test
 * Strategy: "برای SubmitReviewAnswer، حداقل باید Commit موفق و Rollback
 * ... تست شود"). Every earlier test of this UseCase
 * ([com.flashlearn.domain.usecase.SubmitReviewAnswerUseCaseTest] etc.)
 * ran against [com.flashlearn.domain.repository.FakeFlashLearnDatabase],
 * whose own KDoc admits "no real rollback semantics" — this is the first
 * test in the project to exercise the actual Room transaction.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SubmitReviewAnswerIntegrationTest {

    private lateinit var harness: RealRoomTestHarness
    private lateinit var createConcept: CreateConceptUseCase
    private lateinit var submitReviewAnswer: SubmitReviewAnswerUseCase

    @Before
    fun setUp() {
        harness = RealRoomTestHarness()
        createConcept = CreateConceptUseCase(
            conceptRepository = harness.conceptRepository,
            contentRepository = harness.contentRepository,
            learningStateRepository = harness.learningStateRepository,
            difficultyStateRepository = harness.difficultyStateRepository,
            conceptTagRepository = harness.conceptTagRepository,
            database = harness.database
        )
        submitReviewAnswer = SubmitReviewAnswerUseCase(
            learningStateRepository = harness.learningStateRepository,
            difficultyStateRepository = harness.difficultyStateRepository,
            reviewHistoryRepository = harness.reviewHistoryRepository,
            settingsRepository = harness.settingsRepository,
            database = harness.database
        )
    }

    @After
    fun tearDown() {
        harness.close()
    }

    @Test
    fun `correct DAILY answer transitions to WEEKLY and appends one ReviewHistory row`() = runTest {
        val conceptId = createConcept(CreateConceptCommand(sourceText = "hola", targetText = "سلام"))
        val now = Instant.now().plusSeconds(60) // a new word is due at creation time, so answer after it
        val sessionId = UUID.randomUUID()
        val attemptId = UUID.randomUUID()

        submitReviewAnswer(
            SubmitReviewAnswerRequest(
                conceptId = conceptId, sessionId = sessionId, reviewAttemptId = attemptId,
                reviewType = ReviewType.DAILY, isCorrect = true, reviewedAt = now
            )
        )

        val learning = harness.learningStateRepository.get(conceptId)
        assertEquals(Stage.WEEKLY, learning?.stage)
        assertEquals(now.plusSeconds(7 * 86400), learning?.nextReviewAt)
        assertEquals(1, learning?.totalCorrect)

        val history = harness.roomDb.reviewHistoryDao().getByConceptId(conceptId)
        assertEquals(1, history.size)
        assertEquals(attemptId, history.single().reviewAttemptId)
    }

    @Test
    fun `replaying the same session and attempt id is rejected and changes nothing`() = runTest {
        val conceptId = createConcept(CreateConceptCommand(sourceText = "hola", targetText = "سلام"))
        val now = Instant.now().plusSeconds(60) // a new word is due at creation time, so answer after it
        val sessionId = UUID.randomUUID()
        val attemptId = UUID.randomUUID()
        val request = SubmitReviewAnswerRequest(
            conceptId = conceptId, sessionId = sessionId, reviewAttemptId = attemptId,
            reviewType = ReviewType.DAILY, isCorrect = true, reviewedAt = now
        )

        submitReviewAnswer(request)
        val learningAfterFirst = harness.learningStateRepository.get(conceptId)
        val difficultyAfterFirst = harness.difficultyStateRepository.get(conceptId)

        assertThrows(DuplicateReviewAttemptException::class.java) {
            runBlocking { submitReviewAnswer(request) }
        }

        assertEquals(learningAfterFirst, harness.learningStateRepository.get(conceptId))
        assertEquals(difficultyAfterFirst, harness.difficultyStateRepository.get(conceptId))
        assertEquals(1, harness.roomDb.reviewHistoryDao().getByConceptId(conceptId).size)
    }

    @Test
    fun `missing LearningState is a DATA_INTEGRITY_ERROR and writes nothing`() = runTest {
        val neverCreatedConceptId = UUID.randomUUID()

        assertThrows(DataIntegrityException::class.java) {
            runBlocking {
                submitReviewAnswer(
                    SubmitReviewAnswerRequest(
                        conceptId = neverCreatedConceptId, sessionId = UUID.randomUUID(),
                        reviewAttemptId = UUID.randomUUID(), reviewType = ReviewType.DAILY,
                        isCorrect = true, reviewedAt = Instant.now()
                    )
                )
            }
        }

        assertEquals(0, harness.roomDb.reviewHistoryDao().getByConceptId(neverCreatedConceptId).size)
        assertNull(harness.learningStateRepository.get(neverCreatedConceptId))
    }

    /**
     * Does not go through [SubmitReviewAnswerUseCase] at all — every path
     * it exposes is already guarded before its second real write would
     * run, so there is no way to force a natural failure *between* the
     * LearningState and DifficultyState writes without modifying the
     * UseCase for testability. This instead verifies the lower-level
     * primitive [SubmitReviewAnswerUseCase] actually depends on for its
     * atomicity claim: that `FlashLearnDatabaseImpl.withTransaction`
     * (backed by real `androidx.room.withTransaction`) truly rolls back
     * a write when the block throws afterward — the exact behavior
     * [com.flashlearn.domain.repository.FakeFlashLearnDatabase] could
     * never verify.
     */
    @Test
    fun `a real Room transaction rolls back a write when the block throws afterward`() = runTest {
        val conceptId = UUID.randomUUID()
        val state = LearningState(
            id = UUID.randomUUID(), conceptId = conceptId, stage = Stage.DAILY, nextReviewAt = null,
            monthlyWrongCount = 0, hasPathFailure = false, totalCorrect = 0, totalWrong = 0, lastReviewedAt = null
        )

        assertThrows(RuntimeException::class.java) {
            runBlocking {
                harness.roomDb.withTransaction {
                    harness.learningStateRepository.upsert(state)
                    throw RuntimeException("simulated failure after a real write")
                }
            }
        }

        assertNull(
            "the LearningState written before the throw must not survive the rollback",
            harness.learningStateRepository.get(conceptId)
        )
    }

    @Test
    fun `WEEKLY wrong forces at least MEDIUM difficulty via the real transaction`() = runTest {
        val conceptId = createConcept(CreateConceptCommand(sourceText = "hola", targetText = "سلام"))
        // Force the concept into WEEKLY first, via a real correct DAILY answer.
        // (A new word is due at creation time, so the first answer must come after it.)
        val firstAnswerAt = Instant.now().plusSeconds(60)
        submitReviewAnswer(
            SubmitReviewAnswerRequest(
                conceptId = conceptId, sessionId = UUID.randomUUID(), reviewAttemptId = UUID.randomUUID(),
                reviewType = ReviewType.DAILY, isCorrect = true, reviewedAt = firstAnswerAt
            )
        )

        submitReviewAnswer(
            SubmitReviewAnswerRequest(
                conceptId = conceptId, sessionId = UUID.randomUUID(), reviewAttemptId = UUID.randomUUID(),
                reviewType = ReviewType.WEEKLY, isCorrect = false, reviewedAt = firstAnswerAt.plusSeconds(7 * 86400)
            )
        )

        val difficulty = harness.difficultyStateRepository.get(conceptId)
        assertEquals(VocabularyDifficulty.MEDIUM, difficulty?.current)
        val learning = harness.learningStateRepository.get(conceptId)
        assertEquals(Stage.DAILY, learning?.stage)
        assertEquals(true, learning?.hasPathFailure)
    }
}
