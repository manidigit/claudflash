package com.claudemani.database

import androidx.room.TypeConverter
import java.time.Instant
import java.util.UUID

/**
 * Room has no native UUID/Instant column type, so both are converted to
 * primitive storage forms. Enums (Stage, VocabularyDifficulty, ReviewType,
 * EntryType, AchievementType) are intentionally NOT converted here — they
 * are stored as their `.name` String directly on each Entity and
 * converted in the Mappers (Phase 9), matching the reference Code v4.10
 * design exactly.
 */
class Converters {
    @TypeConverter
    fun fromUUID(value: UUID?): String? = value?.toString()

    @TypeConverter
    fun toUUID(value: String?): UUID? = value?.let(UUID::fromString)

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
