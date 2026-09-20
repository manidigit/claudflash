package com.claudemani.data.di

import com.claudemani.domain.model.Concept
import com.claudemani.domain.model.Content
import com.claudemani.domain.usecase.ConceptMigrations
import com.claudemani.domain.usecase.ContentMigrations
import com.claudemani.domain.usecase.CurrentConceptVersion
import com.claudemani.domain.usecase.CurrentContentVersion
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Supplies [com.claudemani.domain.usecase.RefreshDataUseCase]'s four
 * qualified parameters. Both versions are 0 and both migration maps are
 * empty because no Concept/Content data-model change has shipped yet
 * (Phase 19 decision log) — when a future app version needs one, add an
 * entry to the relevant map here and bump the matching version, without
 * touching RefreshDataUseCase itself.
 */
@Module
@InstallIn(SingletonComponent::class)
object RefreshDataModule {

    @Provides
    @CurrentConceptVersion
    fun provideCurrentConceptVersion(): Int = 0

    @Provides
    @CurrentContentVersion
    fun provideCurrentContentVersion(): Int = 0

    @Provides
    @ConceptMigrations
    fun provideConceptMigrations(): Map<Int, (Concept) -> Concept> = emptyMap()

    @Provides
    @ContentMigrations
    fun provideContentMigrations(): Map<Int, (Content) -> Content> = emptyMap()
}
