package com.flashlearn.domain.usecase

import com.flashlearn.domain.model.BackupType
import com.flashlearn.domain.model.Concept
import com.flashlearn.domain.model.ConceptTag
import com.flashlearn.domain.model.Content
import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.model.EntryType
import com.flashlearn.domain.model.ExportData
import com.flashlearn.domain.model.LearningState
import com.flashlearn.domain.model.ReviewHistory
import com.flashlearn.domain.model.ReviewSession
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.model.Stage
import com.flashlearn.domain.model.Tag
import com.flashlearn.domain.model.VocabularyDifficulty
import com.flashlearn.domain.model.computeCanonicalKey
import com.flashlearn.domain.repository.ConceptRepository
import com.flashlearn.domain.repository.FakeAchievementRepository
import com.flashlearn.domain.repository.FakeCategoryRepository
import com.flashlearn.domain.repository.FakeConceptRepository
import com.flashlearn.domain.repository.FakeConceptTagRepository
import com.flashlearn.domain.repository.FakeContentRepository
import com.flashlearn.domain.repository.FakeDifficultyStateRepository
import com.flashlearn.domain.repository.FakeFlashLearnDatabase
import com.flashlearn.domain.repository.FakeLanguagePairRepository
import com.flashlearn.domain.repository.FakeLanguageRepository
import com.flashlearn.domain.repository.FakeLearningStateRepository
import com.flashlearn.domain.repository.FakeReviewHistoryRepository
import com.flashlearn.domain.repository.FakeReviewSessionRepository
import com.flashlearn.domain.repository.FakeSettingsRepository
import com.flashlearn.domain.repository.FakeTagRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RestoreBackupUseCaseTest {

    private val now = Instant.parse("2026-09-14T12:00:00Z")
    private val alwaysPersist: suspend (ExportData) -> Boolean = { true }
    private val alwaysConfirm: suspend () -> Boolean = { true }
    private val neverConfirm: suspend () -> Boolean = { false }

    private class Fixture(conceptRepositoryOverride: ConceptRepository? = null) {
        val languages = FakeLanguageRepository()
        val categories = FakeCategoryRepository()
        val tags = FakeTagRepository()
        val languagePairs = FakeLanguagePairRepository()
        val concepts = conceptRepositoryOverride ?: FakeConceptRepository()
        val contents = FakeContentRepository()
        val conceptTags = FakeConceptTagRepository()
        val reviewSessions = FakeReviewSessionRepository()
        val reviewHistories = FakeReviewHistoryRepository()
        val learningStates = FakeLearningStateRepository()
        val difficultyStates = FakeDifficultyStateRepository()
        val settings = FakeSettingsRepository()
        val achievements = FakeAchievementRepository()
        val database = FakeFlashLearnDatabase()

        val createBackup = CreateBackupUseCase(
            languages, categories, tags, languagePairs, concepts, contents, conceptTags,
            reviewSessions, reviewHistories, learningStates, difficultyStates, settings, achievements
        )

        val restoreBackup = RestoreBackupUseCase(
            createBackup, database, languages, categories, tags, languagePairs, concepts, contents,
            conceptTags, reviewSessions, reviewHistories, learningStates, difficultyStates, settings, achievements
        )
    }

    private fun concept(id: UUID = UUID.randomUUID()) = Concept(
        id = id, entryType = EntryType.WORD, categoryId = null, favorite = false,
        active = true, createdAt = now, updatedAt = now
    )

    private fun content(conceptId: UUID, id: UUID = UUID.randomUUID(), languageCode: String = "es", text: String = "hola") =
        Content(id = id, conceptId = conceptId, languageCode = languageCode, text = text, canonicalKey = computeCanonicalKey(text))

    private fun reviewSession(id: UUID = UUID.randomUUID()) = ReviewSession(id, now, null, ReviewType.DAILY)

    private fun reviewHistory(conceptId: UUID, sessionId: UUID, id: UUID = UUID.randomUUID()) = ReviewHistory(
        id = id, sessionId = sessionId, reviewAttemptId = UUID.randomUUID(),
        conceptId = conceptId, reviewedAt = now, isCorrect = true, reviewType = ReviewType.DAILY
    )

    private fun learningState(conceptId: UUID) = LearningState(
        id = UUID.randomUUID(), conceptId = conceptId, stage = Stage.DAILY, nextReviewAt = null,
        monthlyWrongCount = 0, hasPathFailure = false, totalCorrect = 0, totalWrong = 0, lastReviewedAt = null
    )

    private fun difficultyState(conceptId: UUID) = DifficultyState(
        id = UUID.randomUUID(), conceptId = conceptId, current = VocabularyDifficulty.EASY,
        consecutiveCorrect = 0, consecutiveWrong = 0, hasReachedVeryHard = false
    )

    private fun fullExport(
        concepts: List<Concept> = emptyList(),
        contents: List<Content> = emptyList(),
        tags: List<Tag> = emptyList(),
        conceptTags: List<ConceptTag> = emptyList(),
        reviewSessions: List<ReviewSession> = emptyList(),
        reviewHistory: List<ReviewHistory> = emptyList(),
        learningStates: List<LearningState> = emptyList(),
        difficultyStates: List<DifficultyState> = emptyList(),
        schemaVersion: Int = 1
    ) = ExportData(
        schemaVersion = schemaVersion, exportedAt = now, backupType = BackupType.FULL,
        concepts = concepts, contents = contents, tags = tags, conceptTags = conceptTags,
        reviewSessions = reviewSessions, reviewHistory = reviewHistory,
        learningStates = learningStates, difficultyStates = difficultyStates
    )

    @Test
    fun `invalid backup returns Error and makes no changes`() = runTest {
        val fx = Fixture()
        val invalidData = fullExport(schemaVersion = 99)

        val result = fx.restoreBackup(invalidData, 1, now, alwaysPersist, alwaysConfirm)

        assertTrue(result is RestoreResult.Error)
        assertTrue(fx.concepts.getAll().isEmpty())
    }

    @Test
    fun `fresh FULL backup counts every record as new`() = runTest {
        val fx = Fixture()
        val c = concept()
        val session = reviewSession()
        val data = fullExport(
            concepts = listOf(c),
            contents = listOf(content(c.id)),
            reviewSessions = listOf(session),
            reviewHistory = listOf(reviewHistory(c.id, session.id)),
            learningStates = listOf(learningState(c.id)),
            difficultyStates = listOf(difficultyState(c.id))
        )

        val result = fx.restoreBackup(data, 1, now, alwaysPersist, alwaysConfirm)

        assertTrue(result is RestoreResult.Success)
        result as RestoreResult.Success
        assertEquals(6, result.newCount) // concept, content, session, history, learningState, difficultyState
        assertEquals(0, result.mergedCount)
    }

    @Test
    fun `restoring the same FULL backup twice counts the second pass as fully merged`() = runTest {
        val fx = Fixture()
        val c = concept()
        val session = reviewSession()
        val data = fullExport(
            concepts = listOf(c),
            contents = listOf(content(c.id)),
            reviewSessions = listOf(session),
            reviewHistory = listOf(reviewHistory(c.id, session.id)),
            learningStates = listOf(learningState(c.id)),
            difficultyStates = listOf(difficultyState(c.id))
        )

        fx.restoreBackup(data, 1, now, alwaysPersist, alwaysConfirm)
        val second = fx.restoreBackup(data, 1, now, alwaysPersist, alwaysConfirm)
            as RestoreResult.Success

        // ReviewHistory is never duplicated either way, so it doesn't add to mergedCount on replay.
        assertEquals(0, second.newCount)
        assertEquals(5, second.mergedCount) // concept, content, session, learningState, difficultyState
    }

    @Test
    fun `AbortedByUser when the safety backup cannot be persisted and the user declines`() = runTest {
        val fx = Fixture()
        val data = fullExport(concepts = listOf(concept()))

        val result = fx.restoreBackup(data, 1, now, { false }, neverConfirm)

        assertEquals(RestoreResult.AbortedByUser, result)
        assertTrue(fx.concepts.getAll().isEmpty())
    }

    @Test
    fun `proceeds when the safety backup fails but the user confirms`() = runTest {
        val fx = Fixture()
        val data = fullExport(concepts = listOf(concept()))

        val result = fx.restoreBackup(data, 1, now, { false }, alwaysConfirm)

        assertTrue(result is RestoreResult.Success)
        assertEquals(1, fx.concepts.getAll().size)
    }

    @Test
    fun `PROGRESS rows referencing a concept missing from the target are skipped, not errored`() = runTest {
        val fx = Fixture()
        val externalConceptId = UUID.randomUUID() // never inserted into fx.concepts
        val session = reviewSession()
        val data = ExportData(
            schemaVersion = 1, exportedAt = now, backupType = BackupType.PROGRESS,
            reviewSessions = listOf(session),
            reviewHistory = listOf(reviewHistory(externalConceptId, session.id)),
            learningStates = listOf(learningState(externalConceptId)),
            difficultyStates = listOf(difficultyState(externalConceptId))
        )

        val result = fx.restoreBackup(data, 1, now, alwaysPersist, alwaysConfirm)
            as RestoreResult.Success

        assertEquals(1, result.newCount) // only the session, which isn't concept-gated
        assertEquals(0, result.mergedCount)
        assertNull(fx.learningStates.get(externalConceptId))
        assertNull(fx.difficultyStates.get(externalConceptId))
        assertTrue(fx.reviewHistories.getByConceptId(externalConceptId).isEmpty())
    }

    @Test
    fun `PROGRESS rows are restored once their concept already exists in the target`() = runTest {
        val fx = Fixture()
        val c = concept()
        fx.concepts.insert(c) // simulates a prior VOCABULARY restore having already happened
        val session = reviewSession()
        val data = ExportData(
            schemaVersion = 1, exportedAt = now, backupType = BackupType.PROGRESS,
            reviewSessions = listOf(session),
            reviewHistory = listOf(reviewHistory(c.id, session.id)),
            learningStates = listOf(learningState(c.id)),
            difficultyStates = listOf(difficultyState(c.id))
        )

        val result = fx.restoreBackup(data, 1, now, alwaysPersist, alwaysConfirm)
            as RestoreResult.Success

        assertEquals(4, result.newCount)
        assertTrue(fx.learningStates.get(c.id) != null)
        assertTrue(fx.difficultyStates.get(c.id) != null)
    }

    @Test
    fun `Content with identical text at an existing (conceptId,languageCode) is skipped, not duplicated`() = runTest {
        val fx = Fixture()
        val c = concept()
        val existingContent = content(c.id, text = "hola")
        fx.concepts.insert(c)
        fx.contents.upsert(existingContent)

        // Same concept + language + text, but a brand-new backup-side Content id.
        val incoming = content(c.id, id = UUID.randomUUID(), text = "hola")
        val data = fullExport(concepts = listOf(c), contents = listOf(incoming))

        fx.restoreBackup(data, 1, now, alwaysPersist, alwaysConfirm)

        val stored = fx.contents.getAllByConceptId(c.id)
        assertEquals(1, stored.size)
        assertEquals(existingContent.id, stored.single().id) // original id preserved, no duplicate row
    }

    @Test
    fun `Content with different text at an existing (conceptId,languageCode) updates the record in place`() = runTest {
        val fx = Fixture()
        val c = concept()
        val existingContent = content(c.id, text = "hola")
        fx.concepts.insert(c)
        fx.contents.upsert(existingContent)

        val incoming = content(c.id, id = UUID.randomUUID(), text = "adiós")
        val data = fullExport(concepts = listOf(c), contents = listOf(incoming))

        fx.restoreBackup(data, 1, now, alwaysPersist, alwaysConfirm)

        val stored = fx.contents.getByConceptIdAndLanguage(c.id, "es")
        assertEquals(existingContent.id, stored?.id) // existing id preserved
        assertEquals("adiós", stored?.text)
    }

    @Test
    fun `ConceptTag is counted as new only the first time it is restored`() = runTest {
        val fx = Fixture()
        val c = concept()
        val tag = Tag(UUID.randomUUID(), "Important")
        fx.tags.insert(tag)
        // The Tag must also be part of THIS backup, not just already
        // present in the target DB — validateBackup() checks internal
        // referential consistency of the export itself (a ConceptTag
        // whose tagId isn't in the backup's own `tags` list is an invalid
        // backup, regardless of what the target happens to already have).
        // Bug found via a real CI test failure: this test previously
        // omitted `tags` entirely, so validateBackup() rejected the backup
        // and `as RestoreResult.Success` below threw ClassCastException —
        // the fix was here, not in RestoreBackupUseCase's ConceptTag logic.
        val data = fullExport(concepts = listOf(c), tags = listOf(tag), conceptTags = listOf(ConceptTag(c.id, tag.id)))

        val first = fx.restoreBackup(data, 1, now, alwaysPersist, alwaysConfirm)
            as RestoreResult.Success
        val second = fx.restoreBackup(data, 1, now, alwaysPersist, alwaysConfirm)
            as RestoreResult.Success

        assertTrue(first.newCount > second.newCount)
        assertEquals(1, fx.conceptTags.getTagIdsForConcept(c.id).size)
    }

    @Test
    fun `an exception during the transaction surfaces as Error instead of propagating`() = runTest {
        val throwingConcepts = object : ConceptRepository by FakeConceptRepository() {
            override suspend fun insert(concept: Concept) {
                throw IllegalStateException("boom")
            }
        }
        val fx = Fixture(conceptRepositoryOverride = throwingConcepts)
        val data = fullExport(concepts = listOf(concept()))

        val result = fx.restoreBackup(data, 1, now, alwaysPersist, alwaysConfirm)

        assertTrue(result is RestoreResult.Error)
    }
}
