package com.flashlearn.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

/**
 * entryType is stored as its enum `.name` (EntryType), not the legacy
 * "contentType" naming from earlier drafts (v4 Audit correction #7).
 * Soft-delete only — `active` is flipped to false, row is never removed.
 */
@Entity(
    tableName = "concepts",
    indices = [Index(value = ["categoryId"]), Index(value = ["active"])]
)
data class ConceptEntity(
    @PrimaryKey val id: UUID,
    val entryType: String,
    val categoryId: UUID?,
    val favorite: Boolean,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val dataVersion: Int
)
