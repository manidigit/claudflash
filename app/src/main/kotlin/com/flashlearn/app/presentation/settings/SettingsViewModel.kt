package com.flashlearn.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashlearn.domain.usecase.GetMaxReviewCardsUseCase
import com.flashlearn.domain.usecase.GetThresholdDifficultyUseCase
import com.flashlearn.domain.usecase.SetMaxReviewCardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns Settings screen state for Max Review Cards + the read-only
 * threshold display — all through UseCases, same rule as every other
 * screen in this app. Theme is deliberately NOT owned here: it is
 * app-wide state that must be visible above the NavGraph (to
 * `FlashLearnTheme`), so it lives in the Activity-scoped `ThemeViewModel`
 * instead and is passed into `SettingsScreen` as plain parameters —
 * see that class's KDoc for the full reasoning.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getThresholdDifficulty: GetThresholdDifficultyUseCase,
    private val getMaxReviewCards: GetMaxReviewCardsUseCase,
    private val setMaxReviewCards: SetMaxReviewCardsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val threshold = getThresholdDifficulty()
            val maxCards = getMaxReviewCards()
            _uiState.value = SettingsUiState(
                isLoading = false,
                thresholdDifficulty = threshold,
                maxReviewCardsText = maxCards?.toString() ?: "",
                maxReviewCardsError = null
            )
        }
    }

    /** Called on every keystroke — validates locally, only persists on [saveMaxReviewCards]. */
    fun onMaxReviewCardsTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(maxReviewCardsText = text, maxReviewCardsError = null)
    }

    fun saveMaxReviewCards() {
        when (val result = parseMaxReviewCards(_uiState.value.maxReviewCardsText)) {
            is MaxReviewCardsParseResult.Invalid -> {
                _uiState.value = _uiState.value.copy(maxReviewCardsError = result.message)
            }
            is MaxReviewCardsParseResult.Valid -> {
                viewModelScope.launch {
                    setMaxReviewCards(result.value, Instant.now())
                    _uiState.value = _uiState.value.copy(maxReviewCardsError = null)
                }
            }
        }
    }
}

/** Pure result of validating the Max Review Cards text field — no ViewModel/coroutine needed to test it. */
internal sealed class MaxReviewCardsParseResult {
    /** [value] is null for "no limit" (empty field), a positive Int otherwise. */
    data class Valid(val value: Int?) : MaxReviewCardsParseResult()
    data class Invalid(val message: String) : MaxReviewCardsParseResult()
}

/**
 * Empty text means "no limit" (Descriptions §6/Backlog F never mandates
 * a cap). Anything else must parse as a positive integer.
 */
internal fun parseMaxReviewCards(rawText: String): MaxReviewCardsParseResult {
    val text = rawText.trim()
    if (text.isEmpty()) {
        return MaxReviewCardsParseResult.Valid(null)
    }
    val asInt = text.toIntOrNull()
    if (asInt == null || asInt <= 0) {
        return MaxReviewCardsParseResult.Invalid("باید یک عدد مثبت باشد یا خالی بماند")
    }
    return MaxReviewCardsParseResult.Valid(asInt)
}
