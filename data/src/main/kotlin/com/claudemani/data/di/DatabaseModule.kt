package com.claudemani.data.di

import android.content.Context
import androidx.room.Room
import com.claudemani.database.ClaudemaniDatabaseCallback
import com.claudemani.database.ClaudemaniRoomDatabase
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
import com.claudemani.database.migration.ALL_MIGRATIONS
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Builds the single Room instance (Descriptions §16.1) and exposes every
 * DAO from it. [ALL_MIGRATIONS] and [ClaudemaniDatabaseCallback] are
 * defined in `:database` (Phase 3/6) specifically so this module only
 * has to wire them together — see their own KDoc for what each does and
 * the procedure for adding a future schema migration.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRoomDatabase(@ApplicationContext context: Context): ClaudemaniRoomDatabase =
        Room.databaseBuilder(context, ClaudemaniRoomDatabase::class.java, ClaudemaniRoomDatabase.DATABASE_NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .addCallback(ClaudemaniDatabaseCallback)
            .build()

    @Provides
    fun provideConceptDao(db: ClaudemaniRoomDatabase): ConceptDao = db.conceptDao()

    @Provides
    fun provideContentDao(db: ClaudemaniRoomDatabase): ContentDao = db.contentDao()

    @Provides
    fun provideLearningStateDao(db: ClaudemaniRoomDatabase): LearningStateDao = db.learningStateDao()

    @Provides
    fun provideDifficultyStateDao(db: ClaudemaniRoomDatabase): DifficultyStateDao = db.difficultyStateDao()

    @Provides
    fun provideTagDao(db: ClaudemaniRoomDatabase): TagDao = db.tagDao()

    @Provides
    fun provideConceptTagDao(db: ClaudemaniRoomDatabase): ConceptTagDao = db.conceptTagDao()

    @Provides
    fun provideReviewSessionDao(db: ClaudemaniRoomDatabase): ReviewSessionDao = db.reviewSessionDao()

    @Provides
    fun provideReviewHistoryDao(db: ClaudemaniRoomDatabase): ReviewHistoryDao = db.reviewHistoryDao()

    @Provides
    fun provideSettingsDao(db: ClaudemaniRoomDatabase): SettingsDao = db.settingsDao()

    @Provides
    fun provideAchievementDao(db: ClaudemaniRoomDatabase): AchievementDao = db.achievementDao()

    @Provides
    fun provideCategoryDao(db: ClaudemaniRoomDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideLanguageDao(db: ClaudemaniRoomDatabase): LanguageDao = db.languageDao()

    @Provides
    fun provideLanguagePairDao(db: ClaudemaniRoomDatabase): LanguagePairDao = db.languagePairDao()
}
