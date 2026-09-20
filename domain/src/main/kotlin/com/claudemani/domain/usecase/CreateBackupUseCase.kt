package com.claudemani.domain.usecase

import com.claudemani.domain.model.BackupType
import com.claudemani.domain.model.ExportData
import com.claudemani.domain.repository.AchievementRepository
import com.claudemani.domain.repository.CategoryRepository
import com.claudemani.domain.repository.ConceptRepository
import com.claudemani.domain.repository.ConceptTagRepository
import com.claudemani.domain.repository.ContentRepository
import com.claudemani.domain.repository.DifficultyStateRepository
import com.claudemani.domain.repository.LanguagePairRepository
import com.claudemani.domain.repository.LanguageRepository
import com.claudemani.domain.repository.LearningStateRepository
import com.claudemani.domain.repository.ReviewHistoryRepository
import com.claudemani.domain.repository.ReviewSessionRepository
import com.claudemani.domain.repository.SettingsRepository
import com.claudemani.domain.repository.TagRepository
import java.time.Instant
import javax.inject.Inject

/**
 * CreateBackup (Algorithms v4.20, Backup & Restore §9-الف). Produces an
 * in-memory [ExportData] snapshot — this UseCase only reads repositories
 * and does not perform any file/JSON I/O. Turning the result into an
 * actual persisted file is a presentation/data-layer concern for a later
 * phase (not yet on the numbered Phase list — see the Phase 17 decision
 * log in PROGRESS_TRACKER.md); this stays symmetric with [ExportData]
 * being consumed directly as a Kotlin object by
 * [com.claudemani.domain.algorithm.validateBackup] and
 * [RestoreBackupUseCase].
 *
 * A backup includes soft-deleted Concepts too (VOCABULARY/FULL) — a Full
 * Backup is meant to be a complete, faithful snapshot, and the Descriptions
 * §2 invariant that deletion is always soft (never physical) would be
 * defeated if a "full" backup silently dropped deleted records.
 */
class CreateBackupUseCase @Inject constructor(
    private val languageRepository: LanguageRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val languagePairRepository: LanguagePairRepository,
    private val conceptRepository: ConceptRepository,
    private val contentRepository: ContentRepository,
    private val conceptTagRepository: ConceptTagRepository,
    private val reviewSessionRepository: ReviewSessionRepository,
    private val reviewHistoryRepository: ReviewHistoryRepository,
    private val learningStateRepository: LearningStateRepository,
    private val difficultyStateRepository: DifficultyStateRepository,
    private val settingsRepository: SettingsRepository,
    private val achievementRepository: AchievementRepository
) {
    suspend operator fun invoke(backupType: BackupType, currentSchemaVersion: Int, now: Instant): ExportData {
        val includeVocabulary = backupType == BackupType.VOCABULARY || backupType == BackupType.FULL
        val includeProgress = backupType == BackupType.PROGRESS || backupType == BackupType.FULL

        return ExportData(
            schemaVersion = currentSchemaVersion,
            exportedAt = now,
            backupType = backupType,

            languages = if (includeVocabulary) languageRepository.getAll() else emptyList(),
            categories = if (includeVocabulary) categoryRepository.getAll() else emptyList(),
            tags = if (includeVocabulary) tagRepository.getAll() else emptyList(),
            languagePairs = if (includeVocabulary) languagePairRepository.getAll() else emptyList(),
            concepts = if (includeVocabulary) conceptRepository.getAll() else emptyList(),
            contents = if (includeVocabulary) contentRepository.getAll() else emptyList(),
            conceptTags = if (includeVocabulary) conceptTagRepository.getAll() else emptyList(),

            reviewSessions = if (includeProgress) reviewSessionRepository.getAll() else emptyList(),
            reviewHistory = if (includeProgress) reviewHistoryRepository.getAll() else emptyList(),
            learningStates = if (includeProgress) learningStateRepository.getAll() else emptyList(),
            difficultyStates = if (includeProgress) difficultyStateRepository.getAll() else emptyList(),
            settings = if (includeProgress) settingsRepository.getAll() else emptyList(),
            achievements = if (includeProgress) achievementRepository.getAll() else emptyList()
        )
    }
}
