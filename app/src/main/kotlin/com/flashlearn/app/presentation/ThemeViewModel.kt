package com.flashlearn.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashlearn.domain.model.AppTheme
import com.flashlearn.domain.usecase.GetThemeUseCase
import com.flashlearn.domain.usecase.SetThemeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Single app-wide source of truth for the user's theme choice
 * (Descriptions §6/Backlog F: Theme/Color Scheme belongs in Settings).
 *
 * This is deliberately created once at the Activity level (in
 * `MainActivity`, before [com.flashlearn.app.navigation.FlashLearnNavGraph]
 * is entered) rather than per-screen like [com.flashlearn.app.presentation.home.HomeViewModel]
 * — the Settings screen changes the theme, but `FlashLearnTheme` at the
 * root of the Compose tree is what actually has to react to it, and a
 * NavBackStackEntry-scoped ViewModel created fresh for the Settings
 * destination would not be visible up there. [currentTheme]/[setTheme]
 * are threaded down through `FlashLearnNavGraph` to the Settings screen
 * as plain parameters instead of a second, independent read of
 * [GetThemeUseCase] — so there is exactly one in-memory value for the
 * current theme, never two that could drift out of sync until a reload.
 *
 * Defaults to [AppTheme.SYSTEM] before the initial async read completes,
 * same default [GetThemeUseCase] itself falls back to — so there is no
 * incorrect flash while loading, only a possible one-frame delay before
 * an explicit LIGHT/DARK user choice takes effect.
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val getTheme: GetThemeUseCase,
    private val setThemeUseCase: SetThemeUseCase
) : ViewModel() {

    private val _currentTheme = MutableStateFlow(AppTheme.SYSTEM)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    init {
        viewModelScope.launch {
            _currentTheme.value = getTheme()
        }
    }

    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
        viewModelScope.launch {
            setThemeUseCase(theme, Instant.now())
        }
    }
}
