package com.flashlearn.domain.algorithm

import com.flashlearn.domain.model.BackupType
import com.flashlearn.domain.model.ExportData

/** Result of [validateBackup] — either Restore may proceed, or it must not. */
sealed interface BackupValidationResult {
    data object Valid : BackupValidationResult
    data class Invalid(val errors: List<String>) : BackupValidationResult
}

/**
 * ValidateBackup (Algorithms v4.20, Backup & Restore §9, "مرحله ۱ —
 * اعتبارسنجی Backup"). Pure and read-only: inspects [data] for internal
 * consistency only. It never touches a live database — cross-checking a
 * PROGRESS backup's [ExportData.conceptReferences] against what the
 * *target* database actually has is Restore's job (Phase 17), not this
 * validator's; per Restore's own contract, a missing Concept there means
 * "skip that row", not a validation failure.
 *
 * Covers the Algorithm's 8 checks as far as they are meaningful at this
 * layer:
 * - (2) schemaVersion is within the supported range.
 * - (5) no duplicate identity keys within any single table of this backup.
 * - (6) required references *between tables inside this same backup* are
 *   valid. Concept-referencing Progress rows (LearningState/
 *   DifficultyState/ReviewHistory.conceptId) are only checked this way
 *   for a FULL backup, where Concepts are actually included alongside
 *   them — for a PROGRESS backup those references are legitimately
 *   external (§ Progress Backup, قانون ۵) and are not flagged here.
 * - (7) required text fields are not blank.
 * - (8) the populated table set matches what [ExportData.backupType] allows.
 *
 * Checks (1), (3) and (4) — "data exists", "ExportData structure is
 * complete/readable", "UUIDs are syntactically valid" — are already
 * guaranteed by Kotlin's type system for any [ExportData] instance that
 * exists at all (a non-nullable, fully-constructed data class with typed
 * `UUID` fields), so there is nothing further to check for them here;
 * see the Phase 16 decision log in PROGRESS_TRACKER.md.
 */
fun validateBackup(data: ExportData, currentSchemaVersion: Int): BackupValidationResult {
    val errors = mutableListOf<String>()

    if (data.schemaVersion < 1 || data.schemaVersion > currentSchemaVersion) {
        errors += "schemaVersion ${data.schemaVersion} خارج از بازه پشتیبانی‌شده (1..$currentSchemaVersion) است."
    }

    errors += validateBackupTypePartitioning(data)
    errors += validateNoDuplicateKeys(data)
    errors += validateInternalReferences(data)
    errors += validateRequiredFields(data)

    return if (errors.isEmpty()) BackupValidationResult.Valid else BackupValidationResult.Invalid(errors)
}

private fun validateBackupTypePartitioning(data: ExportData): List<String> {
    val errors = mutableListOf<String>()
    val vocabularyPopulated = data.languages.isNotEmpty() || data.categories.isNotEmpty() ||
        data.tags.isNotEmpty() || data.languagePairs.isNotEmpty() ||
        data.concepts.isNotEmpty() || data.contents.isNotEmpty() || data.conceptTags.isNotEmpty()
    val progressPopulated = data.reviewSessions.isNotEmpty() || data.reviewHistory.isNotEmpty() ||
        data.learningStates.isNotEmpty() || data.difficultyStates.isNotEmpty() ||
        data.settings.isNotEmpty() || data.achievements.isNotEmpty()

    when (data.backupType) {
        BackupType.VOCABULARY -> if (progressPopulated) {
            errors += "Backup از نوع VOCABULARY است اما حداقل یکی از جداول Progress هم پر شده است."
        }
        BackupType.PROGRESS -> if (vocabularyPopulated) {
            errors += "Backup از نوع PROGRESS است اما حداقل یکی از جداول Vocabulary هم پر شده است."
        }
        BackupType.FULL -> Unit
    }
    return errors
}

private fun validateNoDuplicateKeys(data: ExportData): List<String> {
    val errors = mutableListOf<String>()

    fun <K> checkUnique(label: String, keys: List<K>) {
        val duplicates = keys.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) errors += "$label شناسه(های) تکراری دارد: $duplicates"
    }

    checkUnique("Languages.id", data.languages.map { it.id })
    checkUnique("Categories.id", data.categories.map { it.id })
    checkUnique("Tags.id", data.tags.map { it.id })
    checkUnique("LanguagePairs.id", data.languagePairs.map { it.id })
    checkUnique("Concepts.id", data.concepts.map { it.id })
    checkUnique("Contents.id", data.contents.map { it.id })
    checkUnique("Contents.(conceptId,languageCode)", data.contents.map { it.conceptId to it.languageCode })
    checkUnique("ConceptTags.(conceptId,tagId)", data.conceptTags.map { it.conceptId to it.tagId })
    checkUnique("ReviewSessions.id", data.reviewSessions.map { it.id })
    checkUnique("ReviewHistory.id", data.reviewHistory.map { it.id })
    checkUnique(
        "ReviewHistory.(sessionId,reviewAttemptId)",
        data.reviewHistory.map { it.sessionId to it.reviewAttemptId }
    )
    checkUnique("LearningStates.id", data.learningStates.map { it.id })
    checkUnique("LearningStates.conceptId", data.learningStates.map { it.conceptId })
    checkUnique("DifficultyStates.id", data.difficultyStates.map { it.id })
    checkUnique("DifficultyStates.conceptId", data.difficultyStates.map { it.conceptId })
    checkUnique("Settings.key", data.settings.map { it.key })
    checkUnique("Achievements.type", data.achievements.map { it.type })

    return errors
}

private fun validateInternalReferences(data: ExportData): List<String> {
    val errors = mutableListOf<String>()
    val languageCodes = data.languages.map { it.code }.toSet()
    val conceptIds = data.concepts.map { it.id }.toSet()
    val tagIds = data.tags.map { it.id }.toSet()
    val sessionIds = data.reviewSessions.map { it.id }.toSet()

    data.languagePairs.forEach { pair ->
        if (pair.sourceLanguage !in languageCodes) {
            errors += "LanguagePair ${pair.id}: sourceLanguage '${pair.sourceLanguage}' در Languages نیست."
        }
        if (pair.targetLanguage !in languageCodes) {
            errors += "LanguagePair ${pair.id}: targetLanguage '${pair.targetLanguage}' در Languages نیست."
        }
    }

    // Content/ConceptTag are Vocabulary-only tables — they only ever appear
    // alongside Concepts (VOCABULARY/FULL), so no backupType guard is needed:
    // for a well-formed PROGRESS backup both lists are empty and these loops
    // simply do nothing.
    data.contents.forEach { content ->
        if (content.conceptId !in conceptIds) {
            errors += "Content ${content.id}: conceptId ${content.conceptId} در Concepts این Backup نیست."
        }
    }
    data.conceptTags.forEach { conceptTag ->
        if (conceptTag.conceptId !in conceptIds) {
            errors += "ConceptTag(${conceptTag.conceptId}, ${conceptTag.tagId}): conceptId در Concepts این Backup نیست."
        }
        if (conceptTag.tagId !in tagIds) {
            errors += "ConceptTag(${conceptTag.conceptId}, ${conceptTag.tagId}): tagId در Tags این Backup نیست."
        }
    }

    data.reviewHistory.forEach { history ->
        if (history.sessionId !in sessionIds) {
            errors += "ReviewHistory ${history.id}: sessionId ${history.sessionId} در ReviewSessions این Backup نیست."
        }
    }

    // Progress rows reference Concepts by UUID too, but ONLY a FULL backup
    // actually carries Concepts alongside them — for a PROGRESS backup this
    // reference is intentionally external (ExportData.conceptReferences) and
    // must NOT be flagged here.
    if (data.backupType == BackupType.FULL) {
        data.learningStates.forEach { state ->
            if (state.conceptId !in conceptIds) {
                errors += "LearningState ${state.id}: conceptId ${state.conceptId} در Concepts این Backup نیست."
            }
        }
        data.difficultyStates.forEach { state ->
            if (state.conceptId !in conceptIds) {
                errors += "DifficultyState ${state.id}: conceptId ${state.conceptId} در Concepts این Backup نیست."
            }
        }
        data.reviewHistory.forEach { history ->
            if (history.conceptId !in conceptIds) {
                errors += "ReviewHistory ${history.id}: conceptId ${history.conceptId} در Concepts این Backup نیست."
            }
        }
    }

    return errors
}

private fun validateRequiredFields(data: ExportData): List<String> {
    val errors = mutableListOf<String>()

    data.languages.forEach {
        if (it.code.isBlank()) errors += "Language ${it.id}: code خالی است."
        if (it.name.isBlank()) errors += "Language ${it.id}: name خالی است."
    }
    data.categories.forEach { if (it.name.isBlank()) errors += "Category ${it.id}: name خالی است." }
    data.tags.forEach { if (it.name.isBlank()) errors += "Tag ${it.id}: name خالی است." }
    data.contents.forEach {
        if (it.text.isBlank()) errors += "Content ${it.id}: text خالی است."
        if (it.languageCode.isBlank()) errors += "Content ${it.id}: languageCode خالی است."
    }
    data.settings.forEach { if (it.key.isBlank()) errors += "Setting با key خالی وجود دارد." }

    return errors
}
