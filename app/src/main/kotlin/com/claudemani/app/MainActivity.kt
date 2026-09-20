package com.claudemani.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.claudemani.app.navigation.ClaudemaniNavGraph
import com.claudemani.app.presentation.StartupViewModel
import com.claudemani.app.presentation.ThemeViewModel
import com.claudemani.app.ui.theme.ClaudemaniTheme
import com.claudemani.domain.model.AppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point. Real navigation was wired in Phase 22 — this class itself
 * has no business logic and never will (Descriptions §3.3): it only
 * hosts the Compose tree, theme, and NavGraph.
 *
 * [ThemeViewModel] is created here, before [ClaudemaniNavGraph] is
 * entered, so it is scoped to the Activity (not to any one destination)
 * and its state/setter can be threaded down to the Settings screen
 * (Phase 28) as plain parameters — see that class's KDoc. [StartupViewModel]
 * is created the same way, purely so its `init` block runs exactly once
 * per app process — see its own KDoc.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            hiltViewModel<StartupViewModel>()
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val currentTheme by themeViewModel.currentTheme.collectAsState()
            val darkTheme = when (currentTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            ClaudemaniTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ClaudemaniNavGraph(
                        currentTheme = currentTheme,
                        onThemeChange = themeViewModel::setTheme
                    )
                }
            }
        }
    }
}
