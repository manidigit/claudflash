package com.claudemani.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** Keyed by [key] (e.g. "threshold_difficulty") — never by a row ID, so Restore merges by key without collision. */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Instant
)
