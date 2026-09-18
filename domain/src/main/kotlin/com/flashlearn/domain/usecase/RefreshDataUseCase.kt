package com.flashlearn.domain.usecase

import com.flashlearn.domain.model.Concept
import com.flashlearn.domain.model.Content
import com.flashlearn.domain.repository.ConceptRepository
import com.flashlearn.domain.repository.ContentRepository
import com.flashlearn.domain.repository.FlashLearnDatabase
import javax.inject.Inject

/** Result of [RefreshDataUseCase]. */
sealed interface RefreshDataResult {
    data class Success(val updatedCount: Int) : RefreshDataResult
    data object NoChange : RefreshDataResult
    data class Error(val message: String) : RefreshDataResult
}

/** Thrown internally when a required migration step isn't registered — turns into rollback + [RefreshDataResult.Error]. */
private class MissingMigrationException(message: String) : Exception(message)

/**
 * RefreshDataUseCase (Algorithms v4.20 §7.X — "Refresh / Data Migration
 * Algorithm"). Walks every active Concept and its Contents, applying
 * registered per-version migrations one step at a time while
 * `dataVersion < currentVersion`, entirely inside one transaction so a
 * missing/failing migration rolls back everything from this run rather
 * than leaving some records upgraded and others not.
 *
 * Deliberately independent of:
 * - Room Schema Migration (§12) — that changes table *structure* and
 *   always runs first, before this UseCase, and is never invoked here.
 * - LearningState / ReviewHistory — this UseCase's writes are scoped to
 *   Concept and Content only (§9); migrations themselves are also
 *   forbidden from touching either.
 *
 * [currentConceptVersion]/[currentContentVersion] and the two migration
 * maps are constructor parameters rather than hardcoded constants so a
 * future app version can supply new values without changing this
 * class — see the Phase 19 decision log in PROGRESS_TRACKER.md for why
 * they default to 0/empty (no data-model change has shipped yet) and
 * for the Hilt-binding note.
 */
class RefreshDataUseCase @Inject constructor(
    private val conceptRepository: ConceptRepository,
    private val contentRepository: ContentRepository,
    private val database: FlashLearnDatabase,
    @CurrentConceptVersion private val currentConceptVersion: Int = 0,
    @ConceptMigrations private val conceptMigrations: Map<Int, (Concept) -> Concept> = emptyMap(),
    @CurrentContentVersion private val currentContentVersion: Int = 0,
    @ContentMigrations private val contentMigrations: Map<Int, (Content) -> Content> = emptyMap()
) {
    suspend operator fun invoke(): RefreshDataResult {
        return try {
            val updatedCount = database.withTransaction { runRefresh() }
            if (updatedCount > 0) RefreshDataResult.Success(updatedCount) else RefreshDataResult.NoChange
        } catch (e: Exception) {
            RefreshDataResult.Error(e.message ?: "Refresh failed")
        }
    }

    private suspend fun runRefresh(): Int {
        var updatedCount = 0

        for (concept in conceptRepository.getAllActive()) {
            val migratedConcept = migrateConcept(concept)
            val conceptChanged = migratedConcept.dataVersion != concept.dataVersion
            if (conceptChanged) {
                conceptRepository.update(migratedConcept)
            }

            var contentChangedForConcept = false
            for (content in contentRepository.getAllByConceptId(concept.id)) {
                val migratedContent = migrateContent(content)
                if (migratedContent.dataVersion != content.dataVersion) {
                    contentRepository.upsert(migratedContent)
                    contentChangedForConcept = true
                }
            }

            if (conceptChanged || contentChangedForConcept) updatedCount++
        }

        return updatedCount
    }

    /** migrateConceptToVersionN applied in order, N-1 → N, until [currentConceptVersion] is reached (§2/§5.1). */
    private fun migrateConcept(concept: Concept): Concept {
        var current = concept
        while (current.dataVersion < currentConceptVersion) {
            val nextVersion = current.dataVersion + 1
            val migration = conceptMigrations[nextVersion]
                ?: throw MissingMigrationException("Missing Concept Migration for version $nextVersion")
            current = migration(current).copy(dataVersion = nextVersion)
        }
        return current
    }

    /** migrateContentToVersionN applied in order, N-1 → N, until [currentContentVersion] is reached (§2/§5.2). */
    private fun migrateContent(content: Content): Content {
        var current = content
        while (current.dataVersion < currentContentVersion) {
            val nextVersion = current.dataVersion + 1
            val migration = contentMigrations[nextVersion]
                ?: throw MissingMigrationException("Missing Content Migration for version $nextVersion")
            current = migration(current).copy(dataVersion = nextVersion)
        }
        return current
    }
}
