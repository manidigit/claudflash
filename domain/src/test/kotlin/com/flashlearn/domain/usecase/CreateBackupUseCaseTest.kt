package com.flashlearn.domain.usecase

import com.flashlearn.domain.model.AppSetting
import com.flashlearn.domain.model.BackupType
import com.flashlearn.domain.model.Concept
import com.flashlearn.domain.model.EntryType
import com.flashlearn.domain.repository.FakeAchievementRepository
import com.flashlearn.domain.repository.FakeCategoryRepository
import com.flashlearn.domain.repository.FakeConceptRepository
import com.flashlearn.domain.repository.FakeConceptTagRepository
import com.flashlearn.domain.repository.FakeContentRepository
import com.flashlearn.domain.repository.FakeDifficultyStateRepository
import com.flashlearn.domain.repository.FakeLanguagePairRepository
import com.flashlearn.domain.repository.FakeLanguageRepository
import com.flashlearn.domain.repository.FakeLearningStateRepository
import com.flashlearn.domain.repository.FakeReviewHistoryRepository
import com.flashlearn.domain.repository.FakeReviewSessionRepository
import com.flashlearn.domain.repository.FakeSettingsRepository
import com.flashlearn.domain.repository.FakeTagRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class CreateBackupUseCaseTest {

    private val now = Instant.parse("2026-09-14T12:00:00Z")

    // `inner`: Fixture reads the outer class's `now` — see the identical
    // fix/reasoning in SelectReviewQueueUseCaseTest.
    private inner class Fixture {
        val languages = FakeLanguageRepository()
        val categories = FakeCategoryRepository()
        val tags = FakeTagRepository()
        val languagePairs = FakeLanguagePairRepository()
        val concepts = FakeConceptRepository()
        val contents = FakeContentRepository()
        val conceptTags = FakeConceptTagRepository()
        val reviewSessions = FakeReviewSessionRepository()
        val reviewHistories = FakeReviewHistoryRepository()
        val learningStates = FakeLearningStateRepository()
        val difficultyStates = FakeDifficultyStateRepository()
        val settings = FakeSettingsRepository()
        val achievements = FakeAchievementRepository()

        val useCase = CreateBackupUseCase(
            languageRepository = languages, categoryRepository = categories, tagRepository = tags,
            languagePairRepository = languagePairs, conceptRepository = concepts, contentRepository = contents,
            conceptTagRepository = conceptTags, reviewSessionRepository = reviewSessions,
            reviewHistoryRepository = reviewHistories, learningStateRepository = learningStates,
            difficultyStateRepository = difficultyStates, settingsRepository = settings,
            achievementRepository = achievements
        )

        suspend fun addActiveConcept(): UUID {
            val id = UUID.randomUUID()
            concepts.insert(
                Concept(id = id, entryType = EntryType.WORD, categoryId = null, favorite = false, active = true, createdAt = now, updatedAt = now)
            )
            return id
        }

        suspend fun addSoftDeletedConcept(): UUID {
            val id = addActiveConcept()
            concepts.softDelete(id, now)
            return id
        }
    }

    @Test
    fun `VOCABULARY backup populates only vocabulary tables`() = runTest {
        val fx = Fixture()
        fx.addActiveConcept()
        fx.settings.put(AppSetting("threshold_difficulty", "3", now))

        val export = fx.useCase(BackupType.VOCABULARY, currentSchemaVersion = 1, now = now)

        assertEquals(BackupType.VOCABULARY, export.backupType)
        assertEquals(1, export.concepts.size)
        assertTrue(export.settings.isEmpty())
        assertTrue(export.reviewSessions.isEmpty())
        assertTrue(export.learningStates.isEmpty())
        assertTrue(export.achievements.isEmpty())
    }

    @Test
    fun `PROGRESS backup populates only progress tables`() = runTest {
        val fx = Fixture()
        fx.addActiveConcept()
        fx.settings.put(AppSetting("threshold_difficulty", "3", now))

        val export = fx.useCase(BackupType.PROGRESS, currentSchemaVersion = 1, now = now)

        assertEquals(BackupType.PROGRESS, export.backupType)
        assertTrue(export.concepts.isEmpty())
        assertTrue(export.contents.isEmpty())
        assertEquals(1, export.settings.size)
    }

    @Test
    fun `FULL backup includes soft-deleted concepts`() = runTest {
        val fx = Fixture()
        fx.addActiveConcept()
        fx.addSoftDeletedConcept()

        val export = fx.useCase(BackupType.FULL, currentSchemaVersion = 1, now = now)

        assertEquals(2, export.concepts.size)
        assertTrue(export.concepts.any { !it.active })
    }

    @Test
    fun `schemaVersion and exportedAt are set from the caller's parameters`() = runTest {
        val fx = Fixture()
        val export = fx.useCase(BackupType.FULL, currentSchemaVersion = 7, now = now)
        assertEquals(7, export.schemaVersion)
        assertEquals(now, export.exportedAt)
    }
}
