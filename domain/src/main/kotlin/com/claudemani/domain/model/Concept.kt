package com.claudemani.domain.model

import java.time.Instant
import java.util.UUID

/**
 * The language-independent semantic core of a vocabulary entry. A Concept
 * owns exactly one [LearningState] and exactly one [DifficultyState]
 * (Data Model Canonical v1.1, FROZEN). Translations live in [Content].
 *
 * [dataVersion] supports RefreshDataUseCase (Descriptions §10) — it is
 * independent from Room schema version and is never touched by anything
 * except RefreshDataUseCase migrations.
 *
 * Soft-delete only: [active] = false. Never physically deleted, so
 * ReviewHistory referencing this Concept remains valid forever.
 */
data class Concept(
    val id: UUID,
    val entryType: EntryType,
    val categoryId: UUID?,
    val favorite: Boolean,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val dataVersion: Int = 0
)
