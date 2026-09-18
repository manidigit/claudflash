package com.flashlearn.domain.usecase

import com.flashlearn.domain.exception.DataIntegrityException
import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.repository.ContentRepository
import com.flashlearn.domain.repository.DifficultyStateRepository
import java.util.UUID
import javax.inject.Inject

/** The front (prompt)/back (answer) text pair shown on one Flashcard, plus any notes. */
data class FlashcardContent(
    val frontText: String,
    val backText: String,
    val notes: String?
)

/**
 * Read-only: builds the front (source-language)/back (target-language)
 * text pair for one Concept's flashcard (Descriptions §12.4 — "یادداشت
 * پشت فلش‌کارت نمایش داده شود").
 *
 * [sourceLanguage]/[targetLanguage] default to the same V1 literals
 * [CreateConceptUseCase] already defaults to ("es"→"fa") rather than
 * reading `LanguagePairRepository.getActive()`: nothing in the project
 * seeds/activates a [com.flashlearn.domain.model.LanguagePair] row yet
 * (Descriptions §19.1 frames multi-pair *management* as a Future
 * Extension), so `getActive()` would return null on every fresh install
 * and break Review entirely. [GenerateQuizQuestionUseCase]'s own test
 * suite makes the same choice — it constructs a `LanguagePair("es","fa")`
 * value directly rather than fetching one.
 */
class GetFlashcardContentUseCase @Inject constructor(
    private val contentRepository: ContentRepository
) {
    suspend operator fun invoke(
        conceptId: UUID,
        sourceLanguage: String = "es",
        targetLanguage: String = "fa"
    ): FlashcardContent {
        val contents = contentRepository.getAllByConceptId(conceptId)
        val front = contents.firstOrNull { it.languageCode == sourceLanguage }
            ?: throw DataIntegrityException(
                "Missing source-language ($sourceLanguage) Content for concept $conceptId"
            )
        val back = contents.firstOrNull { it.languageCode == targetLanguage }
            ?: throw DataIntegrityException(
                "Missing target-language ($targetLanguage) Content for concept $conceptId"
            )
        // Notes are only ever written onto the source-language Content — see
        // CreateConceptUseCase (both the manual-add and Paste-import paths funnel
        // through it), which never sets `notes` on the target-language Content.
        return FlashcardContent(frontText = front.text, backText = back.text, notes = front.notes)
    }
}

/**
 * The Phase 6 Descriptions doc named this UseCase ("GetDifficultyStateUseCase
 * with explicit DATA_INTEGRITY_ERROR when state is missing") but nothing
 * needed it as a standalone UseCase until Quiz mode (Phase 26): every
 * earlier caller of [DifficultyStateRepository] already had the
 * DifficultyState loaded some other way, or threw its own
 * [DataIntegrityException] inline. [com.flashlearn.app.presentation.review.ReviewViewModel]
 * is the first caller that needs to fetch one on its own.
 */
class GetDifficultyStateUseCase @Inject constructor(
    private val difficultyStateRepository: DifficultyStateRepository
) {
    suspend operator fun invoke(conceptId: UUID): DifficultyState =
        difficultyStateRepository.get(conceptId)
            ?: throw DataIntegrityException("Missing DifficultyState for concept $conceptId")
}
