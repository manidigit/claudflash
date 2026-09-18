package com.flashlearn.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "languages",
    indices = [Index(value = ["code"], unique = true)]
)
data class LanguageEntity(
    @PrimaryKey val id: UUID,
    val code: String,
    val name: String
)

/**
 * V1 supports exactly one active pair (isActive = true) at a time. A
 * *conditional* unique index (WHERE isActive = 1) is not expressible via
 * Room's declarative @Entity indices — it is created as raw SQL in
 * [com.flashlearn.database.FlashLearnDatabaseCallback] instead. The plain
 * (non-unique) index declared here only speeds up the "find the active
 * pair" query.
 *
 * That callback-created index is the *only* enforcement of "at most one
 * active pair" anywhere in this codebase — there is no
 * `SetActiveLanguagePairUseCase` (or any other domain-layer check) for
 * it yet. An earlier version of this comment claimed the opposite
 * (enforcement at the UseCase level); that was never actually true and
 * was corrected in Phase 29 after `LanguagePairActiveIndexTest` was
 * written to verify the real behavior directly against Room.
 */
@Entity(
    tableName = "language_pairs",
    indices = [Index(value = ["isActive"])]
)
data class LanguagePairEntity(
    @PrimaryKey val id: UUID,
    val sourceLanguage: String,
    val targetLanguage: String,
    val isActive: Boolean
)
