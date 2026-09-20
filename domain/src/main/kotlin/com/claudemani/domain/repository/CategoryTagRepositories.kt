package com.claudemani.domain.repository

import com.claudemani.domain.model.Category
import com.claudemani.domain.model.ConceptTag
import com.claudemani.domain.model.Tag
import java.util.UUID

interface CategoryRepository {
    suspend fun insert(category: Category)
    suspend fun update(category: Category)
    suspend fun getById(id: UUID): Category?
    suspend fun getAll(): List<Category>
}

interface TagRepository {
    suspend fun insert(tag: Tag)
    suspend fun update(tag: Tag)
    suspend fun getById(id: UUID): Tag?
    suspend fun getAll(): List<Tag>
}

/**
 * Many-to-many relation (Appendix N, FROZEN). Inserts must be idempotent
 * — inserting an existing (conceptId, tagId) pair is a silent no-op, not
 * an error.
 */
interface ConceptTagRepository {
    suspend fun insert(conceptTag: ConceptTag)
    suspend fun delete(conceptTag: ConceptTag)
    suspend fun getTagIdsForConcept(conceptId: UUID): List<UUID>
    suspend fun getConceptIdsForTag(tagId: UUID): List<UUID>

    /** Every (conceptId, tagId) pair — used by CreateBackup (Phase 17). */
    suspend fun getAll(): List<ConceptTag>
}
