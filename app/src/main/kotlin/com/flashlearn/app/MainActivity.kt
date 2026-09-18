package com.flashlearn.app

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
import com.flashlearn.app.navigation.FlashLearnNavGraph
import com.flashlearn.app.presentation.StartupViewModel
import com.flashlearn.app.presentation.ThemeViewModel
import com.flashlearn.app.ui.theme.FlashLearnTheme
import com.flashlearn.domain.model.AppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Entry point. Real navigation was wired in Phase 22 — this class itself
 * has no business logic and never will (Descriptions §3.3): it only
 * hosts the Compose tree, theme, and NavGraph.
 *
 * [ThemeViewModel] is created here, before [FlashLearnNavGraph] is
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

            FlashLearnTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FlashLearnNavGraph(
                        currentTheme = currentTheme,
                        onThemeChange = themeViewModel::setTheme
                    )
                }
            }
        }
    }
}
