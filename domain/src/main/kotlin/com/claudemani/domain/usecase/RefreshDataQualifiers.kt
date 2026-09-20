package com.claudemani.domain.usecase

import javax.inject.Qualifier

/**
 * DI qualifiers for [RefreshDataUseCase]'s constructor parameters. Built
 * on `javax.inject.Qualifier` (not a Dagger-specific annotation) so this
 * file can live in `:domain` without adding a Hilt/Dagger dependency
 * there — Hilt recognizes JSR-330 qualifiers the same way it does its
 * own. Without these, Hilt cannot disambiguate the two `Int` parameters
 * or the two `Map<Int, (X) -> X>` parameters (which erase to the same
 * raw type at the JVM level).
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentConceptVersion

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentContentVersion

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ConceptMigrations

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ContentMigrations
