package com.flashlearn.app.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Owns only route-selection state (Descriptions §3.4/Phase 8:
 * "AppViewModel is Hilt-compatible and handles route selection only.
 * Review algorithms and persistence remain in domain/data layers.").
 * Each screen's own ViewModel (Phase 23+) is where domain UseCases are
 * actually invoked — this class never calls into `domain` at all.
 */
@HiltViewModel
class AppViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun onRouteChanged(route: String) {
        _uiState.value = _uiState.value.copy(currentRoute = route)
    }
}
