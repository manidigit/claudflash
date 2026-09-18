package com.flashlearn.data.di

import com.flashlearn.data.repository.AchievementRepositoryImpl
import com.flashlearn.data.repository.CategoryRepositoryImpl
import com.flashlearn.data.repository.ConceptRepositoryImpl
import com.flashlearn.data.repository.ConceptTagRepositoryImpl
import com.flashlearn.data.repository.ContentRepositoryImpl
import com.flashlearn.data.repository.DifficultyStateRepositoryImpl
import com.flashlearn.data.repository.FlashLearnDatabaseImpl
import com.flashlearn.data.repository.LanguagePairRepositoryImpl
import com.flashlearn.data.repository.LanguageRepositoryImpl
import com.flashlearn.data.repository.LearningStateRepositoryImpl
import com.flashlearn.data.repository.ReviewHistoryRepositoryImpl
import com.flashlearn.data.repository.ReviewSessionRepositoryImpl
import com.flashlearn.data.repository.SettingsRepositoryImpl
import com.flashlearn.data.repository.TagRepositoryImpl
import com.flashlearn.domain.repository.AchievementRepository
import com.flashlearn.domain.repository.CategoryRepository
import com.flashlearn.domain.repository.ConceptRepository
import com.flashlearn.domain.repository.ConceptTagRepository
import com.flashlearn.domain.repository.ContentRepository
import com.flashlearn.domain.repository.DifficultyStateRepository
import com.flashlearn.domain.repository.FlashLearnDatabase
import com.flashlearn.domain.repository.LanguagePairRepository
import com.flashlearn.domain.repository.LanguageRepository
import com.flashlearn.domain.repository.LearningStateRepository
import com.flashlearn.domain.repository.ReviewHistoryRepository
import com.flashlearn.domain.repository.ReviewSessionRepository
import com.flashlearn.domain.repository.SettingsRepository
import com.flashlearn.domain.repository.TagRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds every domain Repository interface (`:domain`) to its Room-backed
 * implementation (`:data`). Every UseCase across all 20 prior phases
 * depends only on the `:domain` interfaces — this module is what makes
 * those interfaces resolvable at runtime; nothing about a UseCase's own
 * code changes because of it.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFlashLearnDatabase(impl: FlashLearnDatabaseImpl): FlashLearnDatabase

    @Binds
    @Singleton
    abstract fun bindConceptRepository(impl: ConceptRepositoryImpl): ConceptRepository

    @Binds
    @Singleton
    abstract fun bindContentRepository(impl: ContentRepositoryImpl): ContentRepository

    @Binds
    @Singleton
    abstract fun bindLearningStateRepository(impl: LearningStateRepositoryImpl): LearningStateRepository

    @Binds
    @Singleton
    abstract fun bindDifficultyStateRepository(impl: DifficultyStateRepositoryImpl): DifficultyStateRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository

    @Binds
    @Singleton
    abstract fun bindConceptTagRepository(impl: ConceptTagRepositoryImpl): ConceptTagRepository

    @Binds
    @Singleton
    abstract fun bindReviewSessionRepository(impl: ReviewSessionRepositoryImpl): ReviewSessionRepository

    @Binds
    @Singleton
    abstract fun bindReviewHistoryRepository(impl: ReviewHistoryRepositoryImpl): ReviewHistoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAchievementRepository(impl: AchievementRepositoryImpl): AchievementRepository

    @Binds
    @Singleton
    abstract fun bindLanguageRepository(impl: LanguageRepositoryImpl): LanguageRepository

    @Binds
    @Singleton
    abstract fun bindLanguagePairRepository(impl: LanguagePairRepositoryImpl): LanguagePairRepository
}
