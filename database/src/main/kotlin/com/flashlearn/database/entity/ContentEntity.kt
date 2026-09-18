package com.flashlearn.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * UNIQUE(conceptId, languageCode) — FROZEN (Data Model Canonical v1.1).
 * INDEX(languageCode, canonicalKey) supports Concept-Matching on Import
 * and Duplicate Detection (Appendix M.3) without a live normalize(text)
 * call inside a WHERE clause.
 */
@Entity(
    tableName = "contents",
    indices = [
        Index(value = ["conceptId", "languageCode"], unique = true),
        Index(value = ["languageCode", "canonicalKey"])
    ]
)
data class ContentEntity(
    @PrimaryKey val id: UUID,
    val conceptId: UUID,
    val languageCode: String,
    val text: String,
    val canonicalKey: String,
    val notes: String?,
    val pronunciation: String?,
    val example: String?,
    val dataVersion: Int
)
