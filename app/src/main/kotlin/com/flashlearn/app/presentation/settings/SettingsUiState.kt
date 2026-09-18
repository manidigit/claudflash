package com.flashlearn.app.presentation.settings

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
    val maxReviewCardsError: String? = null
)
