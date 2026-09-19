package com.flashlearn.domain.usecase

import com.flashlearn.domain.model.LanguagePair
import com.flashlearn.domain.model.defaultV1LanguagePair
import com.flashlearn.domain.repository.LanguagePairRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Makes sure exactly one real, persisted [LanguagePair] row exists and is
 * active — the seeding gap the README/PROGRESS_TRACKER's "شکاف‌های
 * شناخته‌شده" list flagged: nothing in the project ever wrote a real row
 * to `language_pairs`, so [LanguagePairRepository.getActive] always
 * returned null on a fresh install. [com.flashlearn.domain.model.defaultV1LanguagePair]
 * only ever produced an in-memory value object for callers that skip the
 * repository entirely — this UseCase is what makes a real row exist so a
 * *future* caller can safely start reading from the repository instead.
 *
 * Deliberately does NOT touch [com.flashlearn.domain.usecase.GenerateQuizQuestionUseCase]
 * or any other consumer still using `defaultV1LanguagePair()` directly —
 * rewiring those to read from the repository is its own, separate change
 * (their constructors would need a new dependency), not part of seeding
 * the data they would read.
 *
 * Idempotent by design, not "run once": every call first checks for an
 * existing active pair and returns it unchanged if found, so it is safe
 * to invoke on every app start (no separate "first launch ever" flag to
 * get wrong) rather than relying on a Room `onCreate` callback that would
 * also silently reseed every fresh in-memory test database, breaking the
 * atomicity assumptions in `LanguagePairActiveIndexTest` (Phase 29).
 */
class EnsureDefaultLanguagePairUseCase @Inject constructor(
    private val languagePairRepository: LanguagePairRepository
) {
    suspend operator fun invoke(): LanguagePair {
        languagePairRepository.getActive()?.let { return it }

        val pair = LanguagePair(
            id = UUID.randomUUID(),
            sourceLanguage = "es",
            targetLanguage = "fa",
            isActive = true
        )
        languagePairRepository.insert(pair)
        return pair
    }
}

/**
 * Read the current active [LanguagePair], falling back to
 * [defaultV1LanguagePair] if somehow none is active yet (repository read
 * failure, or a test using a Fake repository that was never seeded —
 * production code always has a real seeded row after
 * [EnsureDefaultLanguagePairUseCase] runs at app startup, so this
 * fallback should never actually trigger there). This is what closes the
 * README/tracker gap "هیچ Consumer واقعی هنوز از LanguagePairRepository
 * نمی‌خواند" — [com.flashlearn.app.presentation.review.ReviewViewModel]
 * is the first real caller.
 */
class GetActiveLanguagePairUseCase @Inject constructor(
    private val languagePairRepository: LanguagePairRepository
) {
    suspend operator fun invoke(): LanguagePair =
        languagePairRepository.getActive() ?: defaultV1LanguagePair()
}
