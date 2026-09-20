package com.claudemani.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Descriptions §12.1 — general-purpose UI roles, Light and Dark. */
object AppColors {
    val LightPrimary = Color(0xFF4F46E5)
    val LightSecondary = Color(0xFF14B8A6)
    val LightBackground = Color(0xFFFAFAFA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSuccess = Color(0xFF22C55E)
    val LightError = Color(0xFFEF4444)
    val LightWarning = Color(0xFFF59E0B)

    val DarkPrimary = Color(0xFF818CF8)
    val DarkSecondary = Color(0xFF2DD4BF)
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkSuccess = Color(0xFF4ADE80)
    val DarkError = Color(0xFFF87171)
    val DarkWarning = Color(0xFFFBBF24)
}

/**
 * Descriptions §12.2 — Difficulty/Stage colors are a *supplementary*
 * layer on top of [AppColors], used specifically to show a word's
 * status (not general Chrome). Same values in both Light and Dark per
 * the doc (only the general-purpose roles above have separate Dark
 * values).
 */
object DifficultyColors {
    val Easy = Color(0xFF10B981)
    val Medium = Color(0xFFF59E0B)
    val Hard = Color(0xFFEF4444)
    val VeryHard = Color(0xFF8B5CF6)
}

object StageColors {
    val Daily = Color(0xFF3B82F6)
    val Weekly = Color(0xFF8B5CF6)
    val Monthly = Color(0xFFEC4899)
    val Learned = Color(0xFF10B981)
}
