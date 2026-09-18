package com.flashlearn.app.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashlearn.domain.model.Concept
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.model.defaultV1LanguagePair
import com.flashlearn.domain.usecase.EndReviewSessionUseCase
import com.flashlearn.domain.usecase.GenerateQuizQuestionUseCase
import com.flashlearn.domain.usecase.GetDifficultyStateUseCase
import com.flashlearn.domain.usecase.GetFlashcardContentUseCase
import com.flashlearn.domain.usecase.GetMaxReviewCardsUseCase
import com.flashlearn.domain.usecase.QuizGenerationResult
import com.flashlearn.domain.usecase.SelectReviewQueueUseCase
import com.flashlearn.domain.usecase.StartReviewSessionUseCase
import com.flashlearn.domain.usecase.SubmitReviewAnswerRequest
import com.flashlearn.domain.usecase.SubmitReviewAnswerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Descriptions §12.4: wrong answers pause on red feedback before advancing; correct answers advance immediately. */
private const val WRONG_ANSWER_PAUSE_MS = 2000L

/**
 * Runs one Review Session end to end: [SelectReviewQueueUseCase] builds
 * the queue, [StartReviewSessionUseCase] opens it, every answer — whether
 * it came from a Flashcard flip or a Quiz option tap — goes through the
 * single shared [SubmitReviewAnswerUseCase] path, and
 * [EndReviewSessionUseCase] closes it when the queue is exhausted.
 *
 * Flow: [initialize] (once, from the nav arg) → user optionally adjusts
 * [changeReviewType]/[changeMode] while state is [ReviewUiState.SelectingOptions]
 * → [start] (once) commits to one [ReviewType] + [ReviewMode] for the rest
 * of this ViewModel's lifetime — Descriptions doesn't describe switching
 * type or mode mid-session, so this deliberately doesn't allow it either.
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val selectReviewQueue: SelectReviewQueueUseCase,
    private val getMaxReviewCards: GetMaxReviewCardsUseCase,
    private val startReviewSession: StartReviewSessionUseCase,
    private val endReviewSession: EndReviewSessionUseCase,
    private val submitReviewAnswer: SubmitReviewAnswerUseCase,
    private val getFlashcardContent: GetFlashcardContentUseCase,
    private val getDifficultyState: GetDifficultyStateUseCase,
    private val generateQuizQuestion: GenerateQuizQuestionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private var initialized = false
    private var started = false
    private var sessionId: UUID? = null
    private var queue: List<Concept> = emptyList()
    private var correctCount = 0

    /** Idempotent — shows the options picker pre-filled with [initialReviewType], defaulting mode to Flashcard. */
    fun initialize(initialReviewType: ReviewType) {
        if (initialized) return
        initialized = true
        _uiState.value = ReviewUiState.SelectingOptions(reviewType = initialReviewType, mode = ReviewMode.FLASHCARD)
    }

    fun changeReviewType(reviewType: ReviewType) {
        val current = _uiState.value as? ReviewUiState.SelectingOptions ?: return
        _uiState.value = current.copy(reviewType = reviewType)
    }

    fun changeMode(mode: ReviewMode) {
        val current = _uiState.value as? ReviewUiState.SelectingOptions ?: return
        _uiState.value = current.copy(mode = mode)
    }

    /** Idempotent — a second call once the session has already started is a no-op. */
    fun start() {
        if (started) return
        val options = _uiState.value as? ReviewUiState.SelectingOptions ?: return
        started = true
        viewModelScope.launch {
            _uiState.value = ReviewUiState.Loading
            val now = Instant.now()
            // Descriptions §6: this caps Session *size* only, never Eligibility —
            // the due-set itself already came back from SelectReviewQueueUseCase.
            val maxCards = getMaxReviewCards()
            val due = selectReviewQueue(options.reviewType, now = now)
            queue = if (maxCards != null) due.take(maxCards) else due

            if (queue.isEmpty()) {
                _uiState.value = ReviewUiState.Empty(options.reviewType)
                return@launch
            }

            sessionId = startReviewSession(options.reviewType, now)
            showCard(options.reviewType, options.mode, index = 0)
        }
    }

    private suspend fun showCard(reviewType: ReviewType, mode: ReviewMode, index: Int) {
        val concept = queue[index]
        val flashcard = getFlashcardContent(concept.id)

        val presentation: CardPresentation = if (mode == ReviewMode.QUIZ) {
            val difficultyState = getDifficultyState(concept.id)
            when (val quiz = generateQuizQuestion(concept, defaultV1LanguagePair(), difficultyState)) {
                is QuizGenerationResult.QuizQuestion -> CardPresentation.Quiz(
                    options = quiz.options,
                    correctAnswerText = quiz.correctAnswerText
                )
                // Fewer than 3 valid Distractors — this one card falls back to Flashcard presentation.
                QuizGenerationResult.FlashcardFallback -> CardPresentation.Flashcard(
                    backText = flashcard.backText,
                    notes = flashcard.notes,
                    isFlipped = false
                )
            }
        } else {
            CardPresentation.Flashcard(backText = flashcard.backText, notes = flashcard.notes, isFlipped = false)
        }

        _uiState.value = ReviewUiState.InProgress(
            reviewType = reviewType,
            mode = mode,
            currentIndex = index,
            totalCount = queue.size,
            frontText = flashcard.frontText,
            presentation = presentation
        )
    }

    /** Reveals the back of the current card. Only applies to a [CardPresentation.Flashcard]; no-op once already answered. */
    fun flip() {
        val current = _uiState.value as? ReviewUiState.InProgress ?: return
        if (current.feedback != null) return
        val flashcard = current.presentation as? CardPresentation.Flashcard ?: return
        _uiState.value = current.copy(presentation = flashcard.copy(isFlipped = true))
    }

    /** [isCorrect]: true = "بلدم", false = "بلد نیستم". Ignored once this card already has feedback. */
    fun answerFlashcard(isCorrect: Boolean) {
        val current = _uiState.value as? ReviewUiState.InProgress ?: return
        if (current.feedback != null) return
        if (current.presentation !is CardPresentation.Flashcard) return

        _uiState.value = current.copy(feedback = if (isCorrect) AnswerFeedback.CORRECT else AnswerFeedback.WRONG)
        submitAndAdvance(current, isCorrect)
    }

    /** Correctness is decided by comparing [option] to the Quiz's own correct answer. Ignored once already answered. */
    fun selectQuizOption(option: String) {
        val current = _uiState.value as? ReviewUiState.InProgress ?: return
        if (current.feedback != null) return
        val quiz = current.presentation as? CardPresentation.Quiz ?: return
        val isCorrect = option == quiz.correctAnswerText

        _uiState.value = current.copy(
            presentation = quiz.copy(selectedOption = option),
            feedback = if (isCorrect) AnswerFeedback.CORRECT else AnswerFeedback.WRONG
        )
        submitAndAdvance(current, isCorrect)
    }

    /** Shared submit-then-advance tail for both answer paths — this is the one place [SubmitReviewAnswerUseCase] is called from. */
    private fun submitAndAdvance(current: ReviewUiState.InProgress, isCorrect: Boolean) {
        val session = sessionId ?: return
        val concept = queue[current.currentIndex]

        viewModelScope.launch {
            submitReviewAnswer(
                SubmitReviewAnswerRequest(
                    conceptId = concept.id,
                    sessionId = session,
                    reviewAttemptId = UUID.randomUUID(),
                    reviewType = current.reviewType,
                    isCorrect = isCorrect,
                    reviewedAt = Instant.now()
                )
            )
            if (isCorrect) correctCount++ else delay(WRONG_ANSWER_PAUSE_MS)

            val nextIndex = current.currentIndex + 1
            if (nextIndex < queue.size) {
                showCard(current.reviewType, current.mode, nextIndex)
            } else {
                endReviewSession(session, Instant.now())
                _uiState.value = ReviewUiState.Finished(
                    reviewType = current.reviewType,
                    correctCount = correctCount,
                    totalCount = queue.size
                )
            }
        }
    }
}
