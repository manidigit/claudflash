package com.flashlearn.domain.model

import java.util.UUID

/** A user-defined grouping for Concepts (e.g. "Animals", "Travel"). */
data class Category(
    val id: UUID,
    val name: String
)

/** A user-defined label. Many-to-many with Concept via [ConceptTag]. */
data class Tag(
    val id: UUID,
    val name: String
)

/**
 * Many-to-many relation table between Concept and Tag (Tag Data Model
 * Finalization, Appendix N — FROZEN). Primary key is (conceptId, tagId);
 * inserts must be idempotent.
 */
data class ConceptTag(
    val conceptId: UUID,
    val tagId: UUID
)
