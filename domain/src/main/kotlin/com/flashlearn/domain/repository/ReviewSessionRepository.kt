package com.flashlearn.domain.repository

import com.flashlearn.domain.model.ReviewSession
import java.util.UUID

interface ReviewSessionRepository {
    suspend fun insert(session: ReviewSession)
    suspend fun update(session: ReviewSession)
    suspend fun getById(id: UUID): ReviewSession?

    /** Every ReviewSession — used by CreateBackup (Phase 17). */
    suspend fun getAll(): List<ReviewSession>
}
