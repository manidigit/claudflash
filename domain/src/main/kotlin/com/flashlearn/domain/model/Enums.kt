package com.flashlearn.domain.model

/**
 * Learning-path stage. Owned exclusively by the Learning Transition
 * Algorithm (Algorithms v4.20 §5). NEW stage was removed in v1.9 — do not
 * reintroduce it.
 */
enum class Stage {
    DAILY,
    WEEKLY,
    MONTHLY,
    LEARNED
}

/**
 * Difficulty is fully independent from [Stage] (Descriptions §4, Data
 * Model Canonical v1.1 FROZEN). Ordinal order matters: algorithms compare
 * ordinals to move "one step easier/harder", so this order must never
 * change.
 */
enum class VocabularyDifficulty {
    EASY,
    MEDIUM,
    HARD,
    VERY_HARD
}

/**
 * The kind of review being performed. RANDOM draws from due
 * DAILY+WEEKLY+MONTHLY concepts only (shuffled) — see Algorithms v4.20
 * Appendix O.1. LEARNED is only ever entered voluntarily by the user.
 */
enum class ReviewType {
    DAILY,
    WEEKLY,
    MONTHLY,
    LEARNED,
    RANDOM
}

/**
 * Classification of a vocabulary Concept, as produced by the Parser
 * (Algorithms v4.20 §33) or picked manually in Add Word.
 */
enum class EntryType {
    WORD,
    PHRASE,
    SENTENCE,
    IDIOM,
    COLLOCATION,
    STRUCTURE
}

/**
 * Achievement identifiers with fixed thresholds (Algorithms v4.20 §11.4).
 * No additional achievement is invented beyond what the spec defines.
 */
enum class AchievementType {
    FIRST_TEN_WORDS,
    SEVEN_DAY_STREAK,
    THIRTY_DAY_STREAK,
    MEMORY_BUILDER,
    VOCABULARY_BUILDER,
    HARD_MODE_MASTER,
    LONG_TERM_MEMORY
}

/**
 * Backup scope (Algorithms v4.20, Backup & Restore §9-الف). VOCABULARY
 * exports only the Vocabulary tables; PROGRESS exports only the Progress
 * tables (Concepts/Contents are deliberately excluded — see
 * [com.flashlearn.domain.model.ExportData.conceptReferences]); FULL
 * exports both.
 */
enum class BackupType {
    VOCABULARY,
    PROGRESS,
    FULL
}

/**
 * App-wide theme (Descriptions §6/Backlog F "Theme / Color Scheme", §12
 * full Light+Dark support). SYSTEM (follow the device setting) is this
 * implementation's own default — the doc requires the setting to exist
 * but never specifies which of the three should be default; see the
 * Phase 20 decision log in PROGRESS_TRACKER.md.
 */
enum class AppTheme { LIGHT, DARK, SYSTEM }

/**
 * Distractor-selection difficulty for [com.flashlearn.domain.usecase.GenerateQuizQuestionUseCase]
 * — Algorithms v4.20 O.3 Backlog #1: completely independent from
 * [VocabularyDifficulty] ("Vocabulary Difficulty معیار انتخاب Distractor
 * نیست" for this system). Not persisted per-Concept anywhere; it's a
 * per-quiz generation-time parameter only (Phase 38 decision — see the
 * tracker for the category-preference rule each level maps to, and why
 * MEDIUM is defined as "no special treatment", not a half-strength HARD).
 */
enum class QuizDifficulty { EASY, MEDIUM, HARD }
