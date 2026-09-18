package com.flashlearn.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashlearn.domain.usecase.EnsureDefaultLanguagePairUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Runs one-time-per-process app startup UseCases — currently just
 * [EnsureDefaultLanguagePairUseCase] (the seeding gap tracked in
 * README's "شکاف‌های شناخته‌شده" #4). Deliberately NOT added to
 * [AppViewModel]: that class's own KDoc states it "handles route
 * selection only... never calls into domain at all", and this is a
 * real, separate concern (bootstrap side-effects, not navigation state)
 * — so it gets its own tiny ViewModel instead of bending that boundary.
 *
 * Created once in `MainActivity`, the same Activity-scoped pattern
 * [ThemeViewModel] already uses, for the same reason: it must run
 * exactly once for the whole app process, not once per NavGraph
 * destination visit.
 *
 * Exposes no UI state on purpose — nothing in the app reads
 * `LanguagePairRepository.getActive()` yet (every current consumer still
 * uses [com.flashlearn.domain.model.defaultV1LanguagePair] directly, see
 * that function's KDoc), so there is nothing for a screen to react to
 * yet. This UseCase only prepares the data for whichever future phase
 * wires a real consumer to the repository.
 */
@HiltViewModel
class StartupViewModel @Inject constructor(
    private val ensureDefaultLanguagePair: EnsureDefaultLanguagePairUseCase
) : ViewModel() {
    init {
        viewModelScope.launch {
            ensureDefaultLanguagePair()
        }
    }
}
