package com.flashlearn.domain.model

import java.util.UUID
import java.time.Instant

/**
 * Full or partial export snapshot (Algorithms v4.20, "الگوریتم نهایی:
 * Backup & Restore" — بخش الف). Populated by [com.flashlearn.domain.usecase.CreateBackupUseCase]
 * and consumed by [com.flashlearn.domain.usecase.RestoreBackupUseCase]
 * (both Phase 17); this file (Phase 16) only defines the shape and its
 * structural validator ([validateBackup] in
 * `domain/algorithm/BackupValidationAlgorithm.kt`).
 *
 * Every entity's own `id` field throughout this project's domain models
 * (Concept.id, Content.id, ReviewHistory.id, ...) already IS the stable
 * cross-database identity — there is no separate internal
 * Long/auto-increment database ID anywhere in this Room schema (see
 * Phase 3's Entities, all keyed `@PrimaryKey val id: UUID`). The
 * Algorithm document's "UUID = شناسه پایدار بین Backup و Restore" vs.
 * "Database ID = شناسه داخلی" distinction — and the resulting
 * conceptIdMap/languageIdMap/... UUID→DatabaseID remapping it describes —
 * therefore collapses to a no-op in this codebase: Restore (Phase 17)
 * can look a record up by its `id` directly, with no separate ID-mapping
 * table required. This is recorded as a deliberate simplification in
 * PROGRESS_TRACKER.md, not a deviation from the Algorithm's *intent*
 * (stable cross-database identity is still exactly what `id` provides).
 *
 * VOCABULARY backups populate only the Vocabulary-table fields; PROGRESS
 * backups populate only the Progress-table fields (plus
 * [conceptReferences]); FULL backups populate both. [validateBackup]
 * enforces this partitioning.
 */
data class ExportData(
    val schemaVersion: Int,
    val exportedAt: Instant,
    val backupType: BackupType,

    // --- Vocabulary tables ---
    val languages: List<Language> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val languagePairs: List<LanguagePair> = emptyList(),
    val concepts: List<Concept> = emptyList(),
    val contents: List<Content> = emptyList(),
    val conceptTags: List<ConceptTag> = emptyList(),

    // --- Progress tables ---
    val reviewSessions: List<ReviewSession> = emptyList(),
    val reviewHistory: List<ReviewHistory> = emptyList(),
    val learningStates: List<LearningState> = emptyList(),
    val difficultyStates: List<DifficultyState> = emptyList(),
    val settings: List<AppSetting> = emptyList(),
    val achievements: List<Achievement> = emptyList()
) {
    /**
     * Concept UUIDs referenced by Progress rows in this export — derived
     * on demand rather than stored, since it is fully computable from
     * [reviewHistory]/[learningStates]/[difficultyStates] and a stored
     * copy could otherwise drift out of sync. For a PROGRESS backup,
     * Restore (Phase 17) uses this set to confirm the target database
     * already has each Concept before restoring a row that depends on
     * it; a missing Concept means that row is skipped, not an error
     * (Algorithms v4.20, قانون ۵ — Progress Backup).
     *
     * The Algorithm's prose also lists "ReviewSessions" as a source of
     * ConceptReferences, but [ReviewSession] has no `conceptId` field in
     * this project's model at all — a session spans many concepts,
     * linked only via `ReviewHistory.sessionId`. That table is therefore
     * not part of this computation; see the Phase 16 decision log.
     */
    val conceptReferences: Set<UUID>
        get() = (
            reviewHistory.map { it.conceptId } +
                learningStates.map { it.conceptId } +
                difficultyStates.map { it.conceptId }
            ).toSet()
}
