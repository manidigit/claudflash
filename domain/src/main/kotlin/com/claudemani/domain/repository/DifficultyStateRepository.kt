package com.claudemani.domain.repository

import com.claudemani.domain.model.DifficultyState
import java.util.UUID

interface DifficultyStateRepository {
    /** Exactly one DifficultyState per Concept (UNIQUE conceptId). */
    suspend fun get(conceptId: UUID): DifficultyState?

    suspend fun upsert(state: DifficultyState)

    suspend fun delete(conceptId: UUID)

    /** Every DifficultyState — used by CreateBackup (Phase 17). */
    suspend fun getAll(): List<DifficultyState>
}
