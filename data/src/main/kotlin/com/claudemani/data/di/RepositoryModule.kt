package com.claudemani.data.di

import com.claudemani.data.repository.AchievementRepositoryImpl
import com.claudemani.data.repository.CategoryRepositoryImpl
import com.claudemani.data.repository.ConceptRepositoryImpl
import com.claudemani.data.repository.ConceptTagRepositoryImpl
import com.claudemani.data.repository.ContentRepositoryImpl
import com.claudemani.data.repository.DifficultyStateRepositoryImpl
import com.claudemani.data.repository.ClaudemaniDatabaseImpl
import com.claudemani.data.repository.LanguagePairRepositoryImpl
import com.claudemani.data.repository.LanguageRepositoryImpl
import com.claudemani.data.repository.LearningStateRepositoryImpl
import com.claudemani.data.repository.ReviewHistoryRepositoryImpl
import com.claudemani.data.repository.ReviewSessionRepositoryImpl
import com.claudemani.data.repository.SettingsRepositoryImpl
import com.claudemani.data.repository.TagRepositoryImpl
import com.claudemani.domain.repository.AchievementRepository
import com.claudemani.domain.repository.CategoryRepository
import com.claudemani.domain.repository.ConceptRepository
import com.claudemani.domain.repository.ConceptTagRepository
import com.claudemani.domain.repository.ContentRepository
import com.claudemani.domain.repository.DifficultyStateRepository
import com.claudemani.domain.repository.ClaudemaniDatabase
import com.claudemani.domain.repository.LanguagePairRepository
import com.claudemani.domain.repository.LanguageRepository
import com.claudemani.domain.repository.LearningStateRepository
import com.claudemani.domain.repository.ReviewHistoryRepository
import com.claudemani.domain.repository.ReviewSessionRepository
import com.claudemani.domain.repository.SettingsRepository
import com.claudemani.domain.repository.TagRepository
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
    abstract fun bindClaudemaniDatabase(impl: ClaudemaniDatabaseImpl): ClaudemaniDatabase

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
