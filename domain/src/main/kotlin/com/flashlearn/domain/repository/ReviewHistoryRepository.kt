package com.flashlearn.domain.repository

import com.flashlearn.domain.model.ReviewHistory
import java.util.UUID

/**
 * ReviewHistory is append-only — this interface intentionally has no
 * update/delete method. Additional narrow aggregate-query methods needed
 * by specific UseCases (Statistics in Phase 15, Backup/Restore in Phase
 * 16/17) are added to this interface when those UseCases are built,
 * rather than invented speculatively here.
 */
interface ReviewHistoryRepository {
    suspend fun insert(history: ReviewHistory)

    suspend fun getById(id: UUID): ReviewHistory?

    suspend fun getByConceptId(conceptId: UUID): List<ReviewHistory>

    suspend fun getBySessionId(sessionId: UUID): List<ReviewHistory>

    /** Duplicate-attempt check — unique on (sessionId, reviewAttemptId). */
    suspend fun existsByAttemptId(sessionId: UUID, attemptId: UUID): Boolean

    /** Used by Statistics' practicedWords count. */
    suspend fun getDistinctConceptIds(): List<UUID>

    /** Full scan — used by Streak calculation and the LONG_TERM_MEMORY Achievement (Phase 15). */
    suspend fun getAll(): List<ReviewHistory>
}
