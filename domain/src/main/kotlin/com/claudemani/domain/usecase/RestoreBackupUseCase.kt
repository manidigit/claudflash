package com.claudemani.domain.usecase

import com.claudemani.domain.algorithm.BackupValidationResult
import com.claudemani.domain.algorithm.validateBackup
import com.claudemani.domain.model.BackupType
import com.claudemani.domain.model.ExportData
import com.claudemani.domain.model.Stage
import com.claudemani.domain.model.computeCanonicalKey
import com.claudemani.domain.repository.AchievementRepository
import com.claudemani.domain.repository.CategoryRepository
import com.claudemani.domain.repository.ConceptRepository
import com.claudemani.domain.repository.ConceptTagRepository
import com.claudemani.domain.repository.ContentRepository
import com.claudemani.domain.repository.DifficultyStateRepository
import com.claudemani.domain.repository.ClaudemaniDatabase
import com.claudemani.domain.repository.LanguagePairRepository
import com.claudemani.domain.repository.LanguageRepository
import com.claudemani.domain.repository.LearningStateRepository
import com.claudemani.domain.repository.ReviewHistoryRepository
import com.claudemani.domain.repository.ReviewSessionRepository
import com.claudemani.domain.repository.SettingsRepository
import com.claudemani.domain.repository.TagRepository
import java.time.Instant
import javax.inject.Inject

/** Result of [RestoreBackupUseCase]. */
sealed interface RestoreResult {
    data class Success(val newCount: Int, val mergedCount: Int) : RestoreResult
    data object AbortedByUser : RestoreResult
    data class Error(val message: String) : RestoreResult
}

/**
 * RestoreBackup (Algorithms v4.20, Backup & Restore §9-ب). Validates
 * [data], attempts a safety Full Backup of the current state before
 * touching anything, then merges every table inside a single
 * [ClaudemaniDatabase.withTransaction] in the mandated dependency order:
 * Languages → Categories → Tags → LanguagePairs → Concepts → Contents →
 * ConceptTags → ReviewSessions → ReviewHistory → LearningStates →
 * DifficultyStates → Settings → Achievements.
 *
 * [persistSafetyBackup] and [confirmProceedWithoutSafetyBackup] exist
 * because "take a backup and save it somewhere safe" is inherently an
 * I/O + user-interaction concern that a pure domain UseCase cannot own
 * itself (domain has no file system access, and asking the user requires
 * a UI) — see the Phase 17 decision log. This UseCase still owns the
 * *decision* the Algorithm describes: build the safety snapshot, try to
 * persist it, and if that persistence fails, ask before proceeding
 * without it.
 */
class RestoreBackupUseCase @Inject constructor(
    private val createBackup: CreateBackupUseCase,
    private val database: ClaudemaniDatabase,
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
    suspend operator fun invoke(
        data: ExportData,
        currentSchemaVersion: Int,
        now: Instant,
        persistSafetyBackup: suspend (ExportData) -> Boolean,
        confirmProceedWithoutSafetyBackup: suspend () -> Boolean
    ): RestoreResult {
        // مرحله ۱ — اعتبارسنجی Backup
        when (val validation = validateBackup(data, currentSchemaVersion)) {
            is BackupValidationResult.Invalid ->
                return RestoreResult.Error(validation.errors.joinToString("; "))
            BackupValidationResult.Valid -> Unit
        }

        // مرحله ۲ — Backup ایمنی خودکار از وضعیت فعلی
        val safetyBackup = createBackup(BackupType.FULL, currentSchemaVersion, now)
        val safetyBackupPersisted = try {
            persistSafetyBackup(safetyBackup)
        } catch (e: Exception) {
            false
        }
        if (!safetyBackupPersisted) {
            val proceed = confirmProceedWithoutSafetyBackup()
            if (!proceed) return RestoreResult.AbortedByUser
        }

        // مرحله ۳ تا ۵ — اجرای کل Restore داخل یک Transaction واحد
        return try {
            database.withTransaction { runRestore(data, now) }
        } catch (e: Exception) {
            RestoreResult.Error(e.message ?: "Restore failed")
        }
    }

    private suspend fun runRestore(data: ExportData, now: Instant): RestoreResult.Success {
        var newCount = 0
        var mergedCount = 0

        // ۳.۱ Languages
        data.languages.forEach { language ->
            when {
                languageRepository.getById(language.id) != null -> {
                    languageRepository.update(language)
                    mergedCount++
                }
                // `code` is UNIQUE. Same code under a different id is the same language (e.g. a
                // row created before ids were deterministic) — keep the local row, never insert
                // a second one. LanguagePairs reference languages by code, so nothing dangles.
                languageRepository.getByCode(language.code) != null -> mergedCount++
                else -> {
                    languageRepository.insert(language)
                    newCount++
                }
            }
        }

        // ۳.۲ Categories
        data.categories.forEach { category ->
            if (categoryRepository.getById(category.id) != null) {
                categoryRepository.update(category)
                mergedCount++
            } else {
                categoryRepository.insert(category)
                newCount++
            }
        }

        // ۳.۳ Tags
        data.tags.forEach { tag ->
            if (tagRepository.getById(tag.id) != null) {
                tagRepository.update(tag)
                mergedCount++
            } else {
                tagRepository.insert(tag)
                newCount++
            }
        }

        // ۳.۴ LanguagePairs (sourceLanguage/targetLanguage are codes, not FKs to Language.id —
        // no id-remapping needed, see the Phase 16/17 decision log)
        //
        // "At most one active pair" is enforced by a partial UNIQUE index, so Restore must never
        // be the thing that creates a second one: the device's own active choice always wins.
        // An existing pair is matched by id first, then by the same (source,target) codes under
        // a different id (installs from before ids were deterministic); either way the local row
        // and its isActive flag are kept.
        data.languagePairs.forEach { pair ->
            val existing = languagePairRepository.getById(pair.id)
                ?: languagePairRepository.getAll().find {
                    it.sourceLanguage == pair.sourceLanguage && it.targetLanguage == pair.targetLanguage
                }
            if (existing != null) {
                languagePairRepository.update(pair.copy(id = existing.id, isActive = existing.isActive))
                mergedCount++
            } else {
                val canBeActive = pair.isActive && languagePairRepository.getActive() == null
                languagePairRepository.insert(pair.copy(isActive = canBeActive))
                newCount++
            }
        }

        // ۳.۵ Concepts (findAnyById so a soft-deleted Concept is merged in place, not duplicated)
        data.concepts.forEach { concept ->
            if (conceptRepository.findAnyById(concept.id) != null) {
                conceptRepository.update(concept)
                mergedCount++
            } else {
                conceptRepository.insert(concept)
                newCount++
            }
        }

        // ۳.۶ Contents — Appendix M merge order: UUID first, then (conceptId, languageCode)
        data.contents.forEach { content ->
            val existingByUuid = contentRepository.getById(content.id)
            when {
                existingByUuid != null -> {
                    contentRepository.upsert(content.copy(canonicalKey = computeCanonicalKey(content.text)))
                    mergedCount++
                }
                else -> {
                    val existingByConceptLanguage =
                        contentRepository.getByConceptIdAndLanguage(content.conceptId, content.languageCode)
                    when {
                        existingByConceptLanguage == null -> {
                            contentRepository.upsert(content.copy(canonicalKey = computeCanonicalKey(content.text)))
                            newCount++
                        }
                        existingByConceptLanguage.text == content.text -> {
                            // skip — identical content already present, not counted as new or merged
                        }
                        else -> {
                            contentRepository.upsert(
                                content.copy(
                                    id = existingByConceptLanguage.id,
                                    canonicalKey = computeCanonicalKey(content.text)
                                )
                            )
                            mergedCount++
                        }
                    }
                }
            }
        }

        // ۳.۱۱ ConceptTags (Appendix N — idempotent insert; count new vs. already-linked ourselves
        // since the repository contract intentionally hides which branch it took)
        data.conceptTags.forEach { conceptTag ->
            val alreadyLinked = conceptTag.tagId in conceptTagRepository.getTagIdsForConcept(conceptTag.conceptId)
            conceptTagRepository.insert(conceptTag)
            if (alreadyLinked) mergedCount++ else newCount++
        }

        // ۳.۷ ReviewSessions
        data.reviewSessions.forEach { session ->
            if (reviewSessionRepository.getById(session.id) != null) {
                reviewSessionRepository.update(session)
                mergedCount++
            } else {
                reviewSessionRepository.insert(session)
                newCount++
            }
        }

        // ۳.۸ ReviewHistory — append-only (Descriptions §2): a record already found by id IS the
        // same accepted answer already recorded (sessionId/reviewAttemptId/isCorrect/... never
        // legitimately change after the fact), so a duplicate is a pure no-op skip — like the
        // Content "same text" case above, it doesn't count as either new or merged. There is
        // deliberately no update path on ReviewHistoryRepository (see its own KDoc) and this
        // restore step honors that. Also subject to قانون ۵ (Progress Backup): a missing Concept
        // means this row is skipped, not an error.
        data.reviewHistory.forEach { history ->
            if (conceptRepository.findAnyById(history.conceptId) == null) return@forEach
            if (reviewHistoryRepository.getById(history.id) == null) {
                reviewHistoryRepository.insert(history)
                newCount++
            }
        }

        // ۳.۹/۳.۱۰ LearningStates / DifficultyStates — قانون ۵ (Progress Backup): اگر Concept
        // موردنیاز در دیتابیس مقصد وجود نداشته باشد (یک PROGRESS backup می‌تواند به Conceptهای
        // خارجی ارجاع بدهد)، رکورد وابسته skip می‌شود، نه Error.
        data.learningStates.forEach { rawState ->
            if (conceptRepository.findAnyById(rawState.conceptId) == null) return@forEach
            // Backups made by v1.4.0–1.4.4 contain DAILY words with nextReviewAt = null (never
            // due). Repair on the way in so a restore can't resurrect that bug.
            val state = if (rawState.stage != Stage.LEARNED && rawState.nextReviewAt == null) {
                rawState.copy(nextReviewAt = now)
            } else {
                rawState
            }
            val existing = learningStateRepository.get(state.conceptId)
            if (existing != null) {
                learningStateRepository.upsert(state.copy(id = existing.id))
                mergedCount++
            } else {
                learningStateRepository.upsert(state)
                newCount++
            }
        }

        data.difficultyStates.forEach { state ->
            if (conceptRepository.findAnyById(state.conceptId) == null) return@forEach
            val existing = difficultyStateRepository.get(state.conceptId)
            if (existing != null) {
                difficultyStateRepository.upsert(state.copy(id = existing.id))
                mergedCount++
            } else {
                difficultyStateRepository.upsert(state)
                newCount++
            }
        }

        // ۳.۱۲ Settings — keyed by `key`, never by a row id
        data.settings.forEach { setting ->
            val existing = settingsRepository.findByKey(setting.key)
            settingsRepository.put(setting)
            if (existing != null) mergedCount++ else newCount++
        }

        // ۳.۱۳ Achievements — keyed by `type`
        data.achievements.forEach { achievement ->
            val existing = achievementRepository.findByType(achievement.type)
            achievementRepository.upsert(achievement)
            if (existing != null) mergedCount++ else newCount++
        }

        return RestoreResult.Success(newCount = newCount, mergedCount = mergedCount)
    }
}
