package com.claudemani.app.presentation

import com.claudemani.app.navigation.Routes

/**
 * Top-level app UI state (Descriptions §3.4/Phase 8: "AppUiState contains
 * only the currently selected route; it does not duplicate persisted
 * learning, difficulty, or progress state"). Immutable — a new instance
 * replaces the old one on every change, never mutated in place.
 */
data class AppUiState(
    val currentRoute: String = Routes.HOME
)
