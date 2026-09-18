package com.flashlearn.domain.repository

import com.flashlearn.domain.model.Content
import java.util.UUID

interface ContentRepository {
    /** Insert-or-update by [Content.id] — canonicalKey is recomputed by the caller before calling this. */
    suspend fun upsert(content: Content)

    suspend fun getById(id: UUID): Content?

    /** Unique per (conceptId, languageCode) — the Restore/Merge lookup key (Appendix M, FROZEN). */
    suspend fun getByConceptIdAndLanguage(conceptId: UUID, languageCode: String): Content?

    suspend fun getAllByConceptId(conceptId: UUID): List<Content>

    /** Used by canonicalKey-based Concept-Matching during Import (Appendix M.3). */
    suspend fun findByCanonicalKey(languageCode: String, canonicalKey: String): List<Content>

    /**
     * All Contents in a given language, across every Concept. Used by
     * GenerateQuizQuestion (Algorithms v4.20 §12.4) to build the
     * Distractor candidate pool — the caller filters out the current
     * Concept's own Contents and applies Difficulty/active filtering.
     */
    suspend fun getAllByLanguageCode(languageCode: String): List<Content>

    /** Every Content row — used by CreateBackup (Phase 17). */
    suspend fun getAll(): List<Content>
}
