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
 * V1's single hardcoded default pair. Nothing in the project seeds or
 * activates a real [LanguagePair] row yet (Phase 25 decision —
 * `LanguagePairRepository.getActive()` returns null on a fresh install),
 * so callers that need a concrete [LanguagePair] value object (e.g.
 * [com.flashlearn.domain.usecase.GenerateQuizQuestionUseCase]) use this
 * instead of querying the repository. [id] is never persisted or looked
 * up — it only exists to satisfy the shape of [LanguagePair].
 */
fun defaultV1LanguagePair(): LanguagePair = LanguagePair(
    id = UUID.randomUUID(),
    sourceLanguage = "es",
    targetLanguage = "fa",
    isActive = true
)
