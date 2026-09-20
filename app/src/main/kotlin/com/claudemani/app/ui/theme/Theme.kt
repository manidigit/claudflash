package com.claudemani.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * App-wide Compose theme (Descriptions §12: independent design, full
 * Light/Dark support — not a copy of Duolingo or similar apps, and no
 * Android dynamic/wallpaper-based color).
 *
 * This phase only follows the *system* Light/Dark setting via
 * [isSystemInDarkTheme]; wiring the user's explicit choice from
 * [com.claudemani.domain.usecase.GetThemeUseCase] (LIGHT/DARK/SYSTEM) is
 * deferred to the Settings screen (Phase 28), since that needs an
 * async read before the first frame — see the Phase 22 decision log in
 * PROGRESS_TRACKER.md.
 */
@Composable
fun ClaudemaniTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = AppColors.DarkPrimary,
            secondary = AppColors.DarkSecondary,
            background = AppColors.DarkBackground,
            surface = AppColors.DarkSurface,
            error = AppColors.DarkError
        )
    } else {
        lightColorScheme(
            primary = AppColors.LightPrimary,
            secondary = AppColors.LightSecondary,
            background = AppColors.LightBackground,
            surface = AppColors.LightSurface,
            error = AppColors.LightError
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
