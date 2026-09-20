package com.claudemani.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** Keyed by [type] (its enum `.name`) — never by a row ID, so Restore merges by type without collision. */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val type: String,
    val isUnlocked: Boolean,
    val unlockedAt: Instant?
)
