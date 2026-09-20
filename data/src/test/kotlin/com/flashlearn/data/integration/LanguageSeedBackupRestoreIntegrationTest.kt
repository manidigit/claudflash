package com.flashlearn.data.integration

import com.flashlearn.database.FLASHLEARN_SCHEMA_VERSION
import com.flashlearn.domain.model.BackupType
import com.flashlearn.domain.model.LanguagePair
import com.flashlearn.domain.usecase.CreateBackupUseCase
import com.flashlearn.domain.usecase.CreateConceptCommand
import com.flashlearn.domain.usecase.CreateConceptUseCase
import com.flashlearn.domain.usecase.EnsureDefaultLanguagePairUseCase
import com.flashlearn.domain.usecase.EnsureDefaultLanguagesUseCase
import com.flashlearn.domain.usecase.RestoreBackupUseCase
import com.flashlearn.domain.usecase.RestoreResult
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
 * Real-Room regression test for the Phase 41 fix: a Backup taken by the
 * app itself (seeded default Language + LanguagePair) must validate and
 * restore — before Phase 41 no Language row was ever written, so
 * `validateBackup()` rejected every VOCABULARY/FULL backup the app made.
 *
 * Also covers the two real-SQLite constraints Restore must never trip:
 * the UNIQUE index on `languages.code` and the partial UNIQUE index
 * "at most one active language_pair" — neither can be exercised by a
 * Fake repository.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LanguageSeedBackupRestoreIntegrationTest {

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

    private suspend fun seedLanguages(h: RealRoomTestHarness) {
        EnsureDefaultLanguagesUseCase(h.languageRepository)()
    }

    private suspend fun seedLanguagesAndPair(h: RealRoomTestHarness) {
        seedLanguages(h)
        EnsureDefaultLanguagePairUseCase(h.languagePairRepository)()
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

    private fun createConceptUseCase(h: RealRoomTestHarness) = CreateConceptUseCase(
        conceptRepository = h.conceptRepository,
        contentRepository = h.contentRepository,
        learningStateRepository = h.learningStateRepository,
        difficultyStateRepository = h.difficultyStateRepository,
        conceptTagRepository = h.conceptTagRepository,
        database = h.database
    )

    @Test
    fun `a backup taken from a seeded app validates and restores into another seeded app`() = runTest {
        seedLanguagesAndPair(source)
        seedLanguagesAndPair(target)
        val conceptId = createConceptUseCase(source)(CreateConceptCommand(sourceText = "hola", targetText = "سلام"))

        val backup = createBackupUseCase(source)(BackupType.FULL, FLASHLEARN_SCHEMA_VERSION, Instant.now())
        assertEquals(3, backup.languages.size)
        assertEquals(1, backup.languagePairs.size)

        val result = restoreBackupUseCase(target)(
            data = backup,
            currentSchemaVersion = FLASHLEARN_SCHEMA_VERSION,
            now = Instant.now(),
            persistSafetyBackup = { true },
            confirmProceedWithoutSafetyBackup = { true }
        )

        assertTrue("expected Success, got $result", result is RestoreResult.Success)
        // Same deterministic ids on both sides: languages/pair are merged, never duplicated.
        assertEquals(3, target.roomDb.languageDao().getAll().size)
        assertEquals(1, target.roomDb.languagePairDao().getAll().size)
        assertNotNull(target.conceptRepository.getById(conceptId))
    }

    @Test
    fun `restoring into a database with no seeded languages brings languages and the active pair over`() = runTest {
        seedLanguagesAndPair(source)
        val backup = createBackupUseCase(source)(BackupType.FULL, FLASHLEARN_SCHEMA_VERSION, Instant.now())

        val result = restoreBackupUseCase(target)(
            data = backup,
            currentSchemaVersion = FLASHLEARN_SCHEMA_VERSION,
            now = Instant.now(),
            persistSafetyBackup = { true },
            confirmProceedWithoutSafetyBackup = { true }
        )

        assertTrue("expected Success, got $result", result is RestoreResult.Success)
        assertEquals(3, target.roomDb.languageDao().getAll().size)
        val active = target.languagePairRepository.getActive()
        assertNotNull(active)
        assertEquals("es", active?.sourceLanguage)
        assertEquals("fa", active?.targetLanguage)
    }

    @Test
    fun `restoring over a legacy random-id active pair does not violate the active-pair index`() = runTest {
        seedLanguagesAndPair(source)
        // An install from before ids were deterministic: same languages, random pair id, already active.
        seedLanguages(target)
        val legacyPair = LanguagePair(UUID.randomUUID(), "es", "fa", isActive = true)
        target.languagePairRepository.insert(legacyPair)

        val backup = createBackupUseCase(source)(BackupType.FULL, FLASHLEARN_SCHEMA_VERSION, Instant.now())
        val result = restoreBackupUseCase(target)(
            data = backup,
            currentSchemaVersion = FLASHLEARN_SCHEMA_VERSION,
            now = Instant.now(),
            persistSafetyBackup = { true },
            confirmProceedWithoutSafetyBackup = { true }
        )

        assertTrue("expected Success, got $result", result is RestoreResult.Success)
        val pairs = target.roomDb.languagePairDao().getAll()
        assertEquals(1, pairs.size)
        assertEquals(legacyPair.id, pairs.single().id)
    }
}
