package com.claudemani.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: UUID,
    val name: String
)

/**
 * Many-to-many relation table (Appendix N, FROZEN — not a JSON/Text
 * column on Concept). PRIMARY KEY(conceptId, tagId); inserts must be
 * idempotent at the DAO level (OnConflictStrategy.IGNORE).
 * INDEX(tagId, conceptId) supports "find all Concepts with this Tag".
 */
@Entity(
    tableName = "concept_tags",
    primaryKeys = ["conceptId", "tagId"],
    indices = [Index(value = ["tagId", "conceptId"])]
)
data class ConceptTagEntity(
    val conceptId: UUID,
    val tagId: UUID
)
