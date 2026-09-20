package com.claudemani.domain.repository

import com.claudemani.domain.model.Concept
import java.time.Instant
import java.util.UUID

/**
 * Note on identity: [Concept.id] is itself the stable UUID used for both
 * database identity and Backup/Restore identity (Code v4.10 §1) — there
 * is no separate internal Long ID in this schema, so "find by UUID" and
 * "find by id" are the same operation here.
 */
interface ConceptRepository {
    suspend fun insert(concept: Concept)
    suspend fun update(concept: Concept)

    /** Active concepts only (mirrors the DAO's `WHERE active = 1`). */
    suspend fun getById(id: UUID): Concept?

    /**
     * Includes soft-deleted (active = false) concepts. Restore/Merge must
     * use this — an inactive Concept still needs to be found so Restore
     * updates it in place instead of duplicating.
     */
    suspend fun findAnyById(id: UUID): Concept?

    suspend fun getAllActive(): List<Concept>

    /** Every Concept regardless of active flag — used by CreateBackup (Phase 17). */
    suspend fun getAll(): List<Concept>

    /** Soft delete only — never a physical delete (Descriptions §2). */
    suspend fun softDelete(id: UUID, now: Instant)
}
