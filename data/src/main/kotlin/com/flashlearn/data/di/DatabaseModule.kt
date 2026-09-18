package com.flashlearn.data.di

import android.content.Context
import androidx.room.Room
import com.flashlearn.database.FlashLearnDatabaseCallback
import com.flashlearn.database.FlashLearnRoomDatabase
import com.flashlearn.database.dao.AchievementDao
import com.flashlearn.database.dao.CategoryDao
import com.flashlearn.database.dao.ConceptDao
import com.flashlearn.database.dao.ConceptTagDao
import com.flashlearn.database.dao.ContentDao
import com.flashlearn.database.dao.DifficultyStateDao
import com.flashlearn.database.dao.LanguageDao
import com.flashlearn.database.dao.LanguagePairDao
import com.flashlearn.database.dao.LearningStateDao
import com.flashlearn.database.dao.ReviewHistoryDao
import com.flashlearn.database.dao.ReviewSessionDao
import com.flashlearn.database.dao.SettingsDao
import com.flashlearn.database.dao.TagDao
import com.flashlearn.database.migration.ALL_MIGRATIONS
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Builds the single Room instance (Descriptions §16.1) and exposes every
 * DAO from it. [ALL_MIGRATIONS] and [FlashLearnDatabaseCallback] are
 * defined in `:database` (Phase 3/6) specifically so this module only
 * has to wire them together — see their own KDoc for what each does and
 * the procedure for adding a future schema migration.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRoomDatabase(@ApplicationContext context: Context): FlashLearnRoomDatabase =
        Room.databaseBuilder(context, FlashLearnRoomDatabase::class.java, FlashLearnRoomDatabase.DATABASE_NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .addCallback(FlashLearnDatabaseCallback)
            .build()

    @Provides
    fun provideConceptDao(db: FlashLearnRoomDatabase): ConceptDao = db.conceptDao()

    @Provides
    fun provideContentDao(db: FlashLearnRoomDatabase): ContentDao = db.contentDao()

    @Provides
    fun provideLearningStateDao(db: FlashLearnRoomDatabase): LearningStateDao = db.learningStateDao()

    @Provides
    fun provideDifficultyStateDao(db: FlashLearnRoomDatabase): DifficultyStateDao = db.difficultyStateDao()

    @Provides
    fun provideTagDao(db: FlashLearnRoomDatabase): TagDao = db.tagDao()

    @Provides
    fun provideConceptTagDao(db: FlashLearnRoomDatabase): ConceptTagDao = db.conceptTagDao()

    @Provides
    fun provideReviewSessionDao(db: FlashLearnRoomDatabase): ReviewSessionDao = db.reviewSessionDao()

    @Provides
    fun provideReviewHistoryDao(db: FlashLearnRoomDatabase): ReviewHistoryDao = db.reviewHistoryDao()

    @Provides
    fun provideSettingsDao(db: FlashLearnRoomDatabase): SettingsDao = db.settingsDao()

    @Provides
    fun provideAchievementDao(db: FlashLearnRoomDatabase): AchievementDao = db.achievementDao()

    @Provides
    fun provideCategoryDao(db: FlashLearnRoomDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideLanguageDao(db: FlashLearnRoomDatabase): LanguageDao = db.languageDao()

    @Provides
    fun provideLanguagePairDao(db: FlashLearnRoomDatabase): LanguagePairDao = db.languagePairDao()
}
