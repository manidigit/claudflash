package com.flashlearn.data.integration

import com.flashlearn.database.FLASHLEARN_SCHEMA_VERSION
import com.flashlearn.domain.model.BackupType
import com.flashlearn.domain.model.ReviewSession
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.model.Stage
import com.flashlearn.domain.usecase.CreateBackupUseCase
import com.flashlearn.domain.usecase.CreateConceptCommand
import com.flashlearn.domain.usecase.CreateConceptUseCase
import com.flashlearn.domain.usecase.RestoreBackupUseCase
import com.flashlearn.domain.usecase.RestoreResult
import com.flashlearn.domain.usecase.SubmitReviewAnswerRequest
import com.flashlearn.domain.usecase.SubmitReviewAnswerUseCase
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Real-Room, two-database integration test for Backup/Restore
 * (Algorithms v4.20 §9). This is the one area of the whole project where
 * a Fake-repository test genuinely cannot substitute for the real thing:
 * the entire point of the UUID → real-database-ID mapping the Algorithm
 * describes (Restore Rule 1/2) only exists because Room looks up rows by
 * UUID primary key across two *separate physical databases* — a single
 * shared in-memory Fake map has no such distinction to get wrong in the
 * first place.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BackupRestoreIntegrationTest {

    private lateinit var source: RealRoomTestHarness
    private lateinit var target: RealRoomTestHarness

    @Before
    fun setUp() {
        source = RealRoomTestHarness()
        target = RealRoomTestHarness()
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    private fun createBackupUseCase(h: RealRoomTestHarness) = CreateBackupUseCase(
        languageRepository = h.languageRepository,
        categoryRepository = h.categoryRepository,
        tagRepository = h.tagRepository,
        languagePairRepository = h.languagePairRepository,
        conceptRepository = h.conceptRepository,
        contentRepository = h.contentRepository,
        conceptTagRepository = h.conceptTagRepository,
        reviewSessionRepository = h.reviewSessionRepository,
        reviewHistoryRepository = h.reviewHistoryRepository,
        learningStateRepository = h.learningStateRepository,
        difficultyStateRepository = h.difficultyStateRepository,
        settingsRepository = h.settingsRepository,
        achievementRepository = h.achievementRepository
    )

    private fun restoreBackupUseCase(h: RealRoomTestHarness) = RestoreBackupUseCase(
        createBackup = createBackupUseCase(h),
        database = h.database,
        languageRepository = h.languageRepository,
        categoryRepository = h.categoryRepository,
        tagRepository = h.tagRepository,
        languagePairRepository = h.languagePairRepository,
        conceptRepository = h.conceptRepository,
        contentRepository = h.contentRepository,
        conceptTagRepository = h.conceptTagRepository,
        reviewSessionRepository = h.reviewSessionRepository,
        reviewHistoryRepository = h.reviewHistoryRepository,
        learningStateRepository = h.learningStateRepository,
        difficultyStateRepository = h.difficultyStateRepository,
        settingsRepository = h.settingsRepository,
        achievementRepository = h.achievementRepository
    )

    @Test
    fun `full backup from one real database restores completely into an empty one`() = runTest {
        val createConceptInSource = CreateConceptUseCase(
            conceptRepository = source.conceptRepository,
            contentRepository = source.contentRepository,
            learningStateRepository = source.learningStateRepository,
            difficultyStateRepository = source.difficultyStateRepository,
            conceptTagRepository = source.conceptTagRepository,
            database = source.database
        )
        val submitAnswerInSource = SubmitReviewAnswerUseCase(
            learningStateRepository = source.learningStateRepository,
            difficultyStateRepository = source.difficultyStateRepository,
            reviewHistoryRepository = source.reviewHistoryRepository,
            settingsRepository = source.settingsRepository,
            database = source.database
        )

        val conceptId = createConceptInSource(CreateConceptCommand(sourceText = "hola", targetText = "سلام"))
        createConceptInSource(CreateConceptCommand(sourceText = "adiós", targetText = "خداحافظ"))

        // A FULL backup must be internally referentially consistent
        // (validateBackup checks every ReviewHistory.sessionId is in the
        // backup's own reviewSessions) — a real ReviewSession row must
        // exist before submitting an answer under that sessionId, not
        // just a bare UUID. Bug found via a real CI test failure: this
        // test previously skipped creating the session, so the produced
        // backup was invalid and Restore correctly returned Error, which
        // then failed the `result is RestoreResult.Success` assertion below.
        val sessionId = UUID.randomUUID()
        source.reviewSessionRepository.insert(
            ReviewSession(
                id = sessionId, startedAt = Instant.parse("2026-09-18T08:55:00Z"),
                endedAt = null, reviewType = ReviewType.DAILY
            )
        )
        submitAnswerInSource(
            SubmitReviewAnswerRequest(
                conceptId = conceptId, sessionId = sessionId, reviewAttemptId = UUID.randomUUID(),
                reviewType = ReviewType.DAILY, isCorrect = true, reviewedAt = Instant.ofEpochMilli(System.currentTimeMillis() + 60_000)
            )
        )

        val backup = createBackupUseCase(source)(BackupType.FULL, FLASHLEARN_SCHEMA_VERSION, Instant.now())
        assertEquals(2, backup.concepts.size)
        assertEquals(4, backup.contents.size)
        assertEquals(1, backup.reviewSessions.size)
        assertEquals(1, backup.reviewHistory.size)

        val result = restoreBackupUseCase(target)(
            data = backup,
            currentSchemaVersion = FLASHLEARN_SCHEMA_VERSION,
            now = Instant.now(),
            persistSafetyBackup = { true },
            confirmProceedWithoutSafetyBackup = { true }
        )

        assertTrue("expected Success, got $result", result is RestoreResult.Success)
        result as RestoreResult.Success
        assertEquals(0, result.mergedCount)

        val restoredConcept = target.conceptRepository.getById(conceptId)
        assertNotNull("restored Concept must exist with the same UUID as the source", restoredConcept)

        val restoredSourceContent = target.roomDb.contentDao().getAllByConceptId(conceptId)
            .first { it.languageCode == "es" }
        assertEquals("hola", restoredSourceContent.text)

        val restoredLearning = target.learningStateRepository.get(conceptId)
        assertEquals(Stage.WEEKLY, restoredLearning?.stage)

        val restoredHistory = target.roomDb.reviewHistoryDao().getByConceptId(conceptId)
        assertEquals(1, restoredHistory.size)
    }

    @Test
    fun `restoring the same backup twice is idempotent - second pass merges instead of duplicating`() = runTest {
        val createConceptInSource = CreateConceptUseCase(
            conceptRepository = source.conceptRepository,
            contentRepository = source.contentRepository,
            learningStateRepository = source.learningStateRepository,
            difficultyStateRepository = source.difficultyStateRepository,
            conceptTagRepository = source.conceptTagRepository,
            database = source.database
        )
        createConceptInSource(CreateConceptCommand(sourceText = "hola", targetText = "سلام"))
        val backup = createBackupUseCase(source)(BackupType.FULL, FLASHLEARN_SCHEMA_VERSION, Instant.now())
        val restore = restoreBackupUseCase(target)

        val first = restore(backup, FLASHLEARN_SCHEMA_VERSION, Instant.now(), { true }, { true }) as RestoreResult.Success
        val second = restore(backup, FLASHLEARN_SCHEMA_VERSION, Instant.now(), { true }, { true }) as RestoreResult.Success

        assertEquals(first.newCount, second.mergedCount)
        assertEquals(0, second.newCount)
        assertEquals(1, target.roomDb.conceptDao().getAllActive().size)
    }
}
