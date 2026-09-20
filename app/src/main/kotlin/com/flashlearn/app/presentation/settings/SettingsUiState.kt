package com.flashlearn.app.presentation.settings

import com.flashlearn.domain.model.QuizDifficulty

/**
 * Settings screen state. [thresholdDifficulty] is shown read-only
 * (Descriptions §6.3/D — no UI to change it in V1); only
 * [maxReviewCardsText] is user-editable here. Theme lives in the
 * Activity-scoped `ThemeViewModel`, not here — see [SettingsViewModel]'s
 * KDoc.
 */
data class SettingsUiState(
    val isLoading: Boolean = true,
    val thresholdDifficulty: Int = 0,
    /** Empty string means "no limit" (Descriptions §6/Backlog F). */
    val maxReviewCardsText: String = "",
    val maxReviewCardsError: String? = null,
    /** Algorithms O.3 Backlog #1 (Phase 38) — distractor-selection difficulty, independent from Vocabulary Difficulty. */
    val quizDifficulty: QuizDifficulty = QuizDifficulty.MEDIUM,
    /** Descriptions §16.2 (Phase 38) — encrypts the exported Backup file with a user-entered PIN. */
    val backupEncryptionEnabled: Boolean = false
)
