package com.flashlearn.app.presentation.review

import com.flashlearn.domain.model.ReviewType

/** Visual feedback after answering one card — red/green per Descriptions §12.4. */
enum class AnswerFeedback { CORRECT, WRONG }

/** Flashcard = flip-based; Quiz = four-option multiple choice (Descriptions §12.4). */
enum class ReviewMode { FLASHCARD, QUIZ }

/**
 * How the current card is actually being shown. A QUIZ-mode session can
 * still show an individual card as [Flashcard] when
 * [com.flashlearn.domain.usecase.GenerateQuizQuestionUseCase] returns
 * `FlashcardFallback` (fewer than 3 valid Distractors) — the mode is a
 * per-session request, the presentation is decided per card.
 */
sealed interface CardPresentation {
    data class Flashcard(
        val backText: String,
        val notes: String?,
        val isFlipped: Boolean
    ) : CardPresentation

    data class Quiz(
        val options: List<String>,
        val correctAnswerText: String,
        val selectedOption: String? = null
    ) : CardPresentation
}

/** Review session state (Phase 25 Flashcard + Phase 26 Quiz/type-selector). */
sealed interface ReviewUiState {

    /** Shown before the session starts — lets the user change [reviewType]/[mode] from whatever Home preselected. */
    data class SelectingOptions(
        val reviewType: ReviewType,
        val mode: ReviewMode
    ) : ReviewUiState

    data object Loading : ReviewUiState

    /** Nothing due for [reviewType] right now — not an error (Algorithm §12: "این حالت خطا محسوب نمی‌شود"). */
    data class Empty(val reviewType: ReviewType) : ReviewUiState

    data class InProgress(
        val reviewType: ReviewType,
        val mode: ReviewMode,
        val currentIndex: Int,
        val totalCount: Int,
        val frontText: String,
        val presentation: CardPresentation,
        /** Non-null once the user has answered this card — buttons disable to block a double-submit (Algorithms §7.2). */
        val feedback: AnswerFeedback? = null
    ) : ReviewUiState

    data class Finished(
        val reviewType: ReviewType,
        val correctCount: Int,
        val totalCount: Int
    ) : ReviewUiState
}
