package com.flashlearn.domain.model

import java.util.UUID

/** One supported language (V1: fa, en, es). */
data class Language(
    val id: UUID,
    val code: String,
    val name: String
)

/**
 * A source→target language pairing. V1 supports exactly one active pair
 * at a time ([isActive]); multi-active-pair support is a documented
 * Future Extension (Descriptions §19.1) and is NOT implemented in V1 —
 * do not add a languagePairId to LearningState yet.
 */
data class LanguagePair(
    val id: UUID,
    val sourceLanguage: String,
    val targetLanguage: String,
    val isActive: Boolean
)

/**
 * Stable, deterministic id for a Language, derived only from its [code].
 * The same code therefore gets the SAME id on every install — this is
 * what lets Backup/Restore between two devices match "es" with "es" by
 * id instead of colliding on the UNIQUE `code` index (Phase 41).
 */
fun deterministicLanguageId(code: String): UUID =
    UUID.nameUUIDFromBytes("flashlearn:language:$code".toByteArray(Charsets.UTF_8))

/** Same idea as [deterministicLanguageId], for the default source→target pair. */
fun deterministicLanguagePairId(sourceLanguage: String, targetLanguage: String): UUID =
    UUID.nameUUIDFromBytes(
        "flashlearn:language_pair:$sourceLanguage:$targetLanguage".toByteArray(Charsets.UTF_8)
    )

/**
 * The languages V1 supports (Descriptions §1: Persian, English, Spanish).
 * Single source of truth for seeding — adding a supported language later
 * means adding one entry here; [com.flashlearn.domain.usecase.EnsureDefaultLanguagesUseCase]
 * inserts whatever is missing (matched by `code`) on the next app start.
 */
val V1_LANGUAGES: List<Language> = listOf(
    Language(deterministicLanguageId("fa"), "fa", "فارسی"),
    Language(deterministicLanguageId("en"), "en", "English"),
    Language(deterministicLanguageId("es"), "es", "Español")
)

/**
 * V1's single hardcoded default pair. Used as a value object by callers
 * that need a concrete [LanguagePair] when the repository has none
 * (see [com.flashlearn.domain.usecase.GetActiveLanguagePairUseCase]).
 * Its id is deterministic and identical to the one
 * [com.flashlearn.domain.usecase.EnsureDefaultLanguagePairUseCase] persists.
 */
fun defaultV1LanguagePair(): LanguagePair = LanguagePair(
    id = deterministicLanguagePairId("es", "fa"),
    sourceLanguage = "es",
    targetLanguage = "fa",
    isActive = true
)
