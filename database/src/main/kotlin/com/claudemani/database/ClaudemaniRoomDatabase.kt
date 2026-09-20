package com.claudemani.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.claudemani.database.dao.AchievementDao
import com.claudemani.database.dao.CategoryDao
import com.claudemani.database.dao.ConceptDao
import com.claudemani.database.dao.ConceptTagDao
import com.claudemani.database.dao.ContentDao
import com.claudemani.database.dao.DifficultyStateDao
import com.claudemani.database.dao.LanguageDao
import com.claudemani.database.dao.LanguagePairDao
import com.claudemani.database.dao.LearningStateDao
import com.claudemani.database.dao.ReviewHistoryDao
import com.claudemani.database.dao.ReviewSessionDao
import com.claudemani.database.dao.SettingsDao
import com.claudemani.database.dao.TagDao
import com.claudemani.database.entity.AchievementEntity
import com.claudemani.database.entity.CategoryEntity
import com.claudemani.database.entity.ConceptEntity
import com.claudemani.database.entity.ConceptTagEntity
import com.claudemani.database.entity.ContentEntity
import com.claudemani.database.entity.DifficultyStateEntity
import com.claudemani.database.entity.LanguageEntity
import com.claudemani.database.entity.LanguagePairEntity
import com.claudemani.database.entity.LearningStateEntity
import com.claudemani.database.entity.ReviewHistoryEntity
import com.claudemani.database.entity.ReviewSessionEntity
import com.claudemani.database.entity.SettingsEntity
import com.claudemani.database.entity.TagEntity

/**
 * Current Room schema version. Version 1 was the first release of this
 * from-scratch rewrite; version 2 (v1.4.5) only repairs data — no table changed
 * (see MIGRATION_1_2). Bump this and add a Migration whenever the table structure
 * changes (see the KDoc on ALL_MIGRATIONS for the exact procedure).
 *
 * This is entirely independent from Concept/Content.dataVersion, which is
 * owned by RefreshDataUseCase (Phase 19) and migrates row *content*, not
 * table *structure*.
 */
const val CLAUDEMANI_SCHEMA_VERSION = 2

/**
 * The single Room database for the app (Descriptions §16.1: one Room
 * instance, all operations on Dispatchers.IO). Building the actual
 * instance (Room.databaseBuilder + migrations + callback) happens in the
 * Hilt DatabaseModule (Phase 21) in the `data` module — this class only
 * declares the schema.
 */
@Database(
    entities = [
        ConceptEntity::class,
        ContentEntity::class,
        LearningStateEntity::class,
        DifficultyStateEntity::class,
        TagEntity::class,
        ConceptTagEntity::class,
        ReviewSessionEntity::class,
        ReviewHistoryEntity::class,
        SettingsEntity::class,
        AchievementEntity::class,
        CategoryEntity::class,
        LanguageEntity::class,
        LanguagePairEntity::class
    ],
    version = CLAUDEMANI_SCHEMA_VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ClaudemaniRoomDatabase : RoomDatabase() {
    abstract fun conceptDao(): ConceptDao
    abstract fun contentDao(): ContentDao
    abstract fun learningStateDao(): LearningStateDao
    abstract fun difficultyStateDao(): DifficultyStateDao
    abstract fun tagDao(): TagDao
    abstract fun conceptTagDao(): ConceptTagDao
    abstract fun reviewSessionDao(): ReviewSessionDao
    abstract fun reviewHistoryDao(): ReviewHistoryDao
    abstract fun settingsDao(): SettingsDao
    abstract fun achievementDao(): AchievementDao
    abstract fun categoryDao(): CategoryDao
    abstract fun languageDao(): LanguageDao
    abstract fun languagePairDao(): LanguagePairDao

    companion object {
        const val DATABASE_NAME = "claudemani.db"
    }
}
