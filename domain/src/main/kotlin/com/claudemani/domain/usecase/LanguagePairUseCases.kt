package com.claudemani.domain.usecase

import com.claudemani.domain.model.Language
import com.claudemani.domain.model.LanguagePair
import com.claudemani.domain.model.V1_LANGUAGES
import com.claudemani.domain.model.defaultV1LanguagePair
import com.claudemani.domain.model.deterministicLanguagePairId
import com.claudemani.domain.repository.LanguagePairRepository
import com.claudemani.domain.repository.LanguageRepository
import javax.inject.Inject

/**
 * Makes sure every language in [V1_LANGUAGES] exists as a real row in
 * `languages`. Before Phase 41 nothing ever wrote a Language row, so a
 * Backup containing the seeded [LanguagePair] but no Language failed
 * `validateBackup()` (Algorithms §9: a pair's language codes must exist
 * in the same Backup) and could never be restored.
 *
 * Idempotent and safe on every app start: rows are matched by `code`
 * (UNIQUE in the schema); an existing row is returned untouched, only
 * missing ones are inserted. Ids are deterministic
 * ([com.claudemani.domain.model.deterministicLanguageId]), so the same
 * language has the same id on every device.
 */
class EnsureDefaultLanguagesUseCase @Inject constructor(
    private val languageRepository: LanguageRepository
) {
    suspend operator fun invoke(): List<Language> {
        val result = mutableListOf<Language>()
        for (language in V1_LANGUAGES) {
            val existing = languageRepository.getByCode(language.code)
            if (existing != null) {
                result.add(existing)
            } else {
                languageRepository.insert(language)
                result.add(language)
            }
        }
        return result
    }
}

/**
 * Makes sure exactly one real, persisted [LanguagePair] row exists and is
 * active — the seeding gap the README/PROGRESS_TRACKER's "شکاف‌های
 * شناخته‌شده" list flagged: nothing in the project ever wrote a real row
 * to `language_pairs`, so [LanguagePairRepository.getActive] always
 * returned null on a fresh install. [com.claudemani.domain.model.defaultV1LanguagePair]
 * only ever produced an in-memory value object for callers that skip the
 * repository entirely — this UseCase is what makes a real row exist so a
 * *future* caller can safely start reading from the repository instead.
 *
 * Deliberately does NOT touch [com.claudemani.domain.usecase.GenerateQuizQuestionUseCase]
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

        // Deterministic id (same on every device). If that exact row somehow already
        // exists but is inactive, re-activate it instead of a doomed duplicate-PK insert.
        val id = deterministicLanguagePairId("es", "fa")
        val inactive = languagePairRepository.getById(id)
        if (inactive != null) {
            val reactivated = inactive.copy(isActive = true)
            languagePairRepository.update(reactivated)
            return reactivated
        }

        val pair = LanguagePair(
            id = id,
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
 * نمی‌خواند" — [com.claudemani.app.presentation.review.ReviewViewModel]
 * is the first real caller.
 */
class GetActiveLanguagePairUseCase @Inject constructor(
    private val languagePairRepository: LanguagePairRepository
) {
    suspend operator fun invoke(): LanguagePair =
        languagePairRepository.getActive() ?: defaultV1LanguagePair()
}
