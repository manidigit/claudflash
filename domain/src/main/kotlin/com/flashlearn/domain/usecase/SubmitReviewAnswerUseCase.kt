package com.flashlearn.domain.usecase

import com.flashlearn.domain.algorithm.TransitionResult
import com.flashlearn.domain.algorithm.calculateDifficulty
import com.flashlearn.domain.algorithm.calculateLearningTransition
import com.flashlearn.domain.exception.DataIntegrityException
import com.flashlearn.domain.exception.DuplicateReviewAttemptException
import com.flashlearn.domain.exception.ReviewNotDueException
import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.model.LearningState
import com.flashlearn.domain.model.ReviewHistory
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.model.SettingKeys
import com.flashlearn.domain.model.Stage
import com.flashlearn.domain.repository.DifficultyStateRepository
import com.flashlearn.domain.repository.FlashLearnDatabase
import com.flashlearn.domain.repository.LearningStateRepository
import com.flashlearn.domain.repository.ReviewHistoryRepository
import com.flashlearn.domain.repository.SettingsRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class SubmitReviewAnswerRequest(
    val conceptId: UUID,
    val sessionId: UUID,
    val reviewAttemptId: UUID,
    val reviewType: ReviewType,
    val isCorrect: Boolean,
    val reviewedAt: Instant
)

data class SubmitReviewAnswerResult(
    val learningState: LearningState,
    val difficultyState: DifficultyState,
    val transition: TransitionResult
)

/**
 * The single shared answer-submission path for both Flashcard and Quiz
 * review modes (Post v3.0 Integration Update §A). Atomically updates
 * LearningState + DifficultyState and appends one ReviewHistory row.
 *
 * STEP ORDER IS MANDATORY (v4.10 Duplicate-attempt ordering correction —
 * do not reorder):
 * 1. Load LearningState + DifficultyState — missing either is a
 *    [DataIntegrityException], never silently repaired.
 * 2. Duplicate-attempt check on (sessionId, reviewAttemptId) — BEFORE due
 *    validation, so replaying an already-recorded attempt is always
 *    reported as a duplicate, even if nextReviewAt already advanced past
 *    "due" on the first attempt.
 * 3. Due validation — nextReviewAt <= reviewedAt, except ReviewType.LEARNED
 *    which is always allowed.
 * 4. Learning Transition (pure) — computed first so its resulting
 *    monthlyWrongCount reflects this event.
 * 5. Difficulty Calculation (pure) — MUST receive the pre-transition
 *    monthlyWrongCount, and MUST NOT run at all when the Concept's stage
 *    is LEARNED (Difficulty Calculation §12.2).
 * 6-8. Persist LearningState, DifficultyState, append ReviewHistory.
 */
class SubmitReviewAnswerUseCase @Inject constructor(
    private val learningStateRepository: LearningStateRepository,
    private val difficultyStateRepository: DifficultyStateRepository,
    private val reviewHistoryRepository: ReviewHistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val database: FlashLearnDatabase
) {
    suspend operator fun invoke(request: SubmitReviewAnswerRequest): SubmitReviewAnswerResult {
        return database.withTransaction {
            // 1. Load current states (must exist)
            val learning = learningStateRepository.get(request.conceptId)
                ?: throw DataIntegrityException(
                    "LearningState not found for concept ${request.conceptId}"
                )
            val difficulty = difficultyStateRepository.get(request.conceptId)
                ?: throw DataIntegrityException(
                    "DifficultyState not found for concept ${request.conceptId}"
                )

            // 2. Duplicate-attempt check — BEFORE due validation.
            if (reviewHistoryRepository.existsByAttemptId(request.sessionId, request.reviewAttemptId)) {
                throw DuplicateReviewAttemptException(
                    "Duplicate review attempt: sessionId=${request.sessionId}, " +
                        "reviewAttemptId=${request.reviewAttemptId}"
                )
            }

            // 3. Due validation (LEARNED is always allowed).
            if (request.reviewType != ReviewType.LEARNED) {
                val dueAt = learning.nextReviewAt
                if (dueAt != null && dueAt.isAfter(request.reviewedAt)) {
                    throw ReviewNotDueException("Concept is not due yet (nextReviewAt = $dueAt)")
                }
            }

            // 4. Learning Transition (pure).
            val transition = calculateLearningTransition(
                learningState = learning,
                isCorrect = request.isCorrect,
                reviewedAt = request.reviewedAt
            )

            // 5. Difficulty Calculation (pure) — skipped entirely for LEARNED,
            // and given the PRE-transition monthlyWrongCount.
            val newDifficulty = if (learning.stage == Stage.LEARNED) {
                difficulty
            } else {
                val threshold = settingsRepository.getInt(
                    SettingKeys.THRESHOLD_DIFFICULTY,
                    SettingKeys.DEFAULT_THRESHOLD_DIFFICULTY
                )
                calculateDifficulty(
                    state = difficulty,
                    isCorrect = request.isCorrect,
                    reviewType = request.reviewType,
                    monthlyWrongCountBefore = learning.monthlyWrongCount,
                    threshold = threshold
                )
            }

            // 6. Persist LearningState.
            val updatedLearning = learning.copy(
                stage = transition.newStage,
                nextReviewAt = transition.nextReviewAt,
                hasPathFailure = transition.hasPathFailure,
                monthlyWrongCount = transition.monthlyWrongCount,
                totalCorrect = if (request.isCorrect) learning.totalCorrect + 1 else learning.totalCorrect,
                totalWrong = if (!request.isCorrect) learning.totalWrong + 1 else learning.totalWrong,
                lastReviewedAt = request.reviewedAt
            )
            learningStateRepository.upsert(updatedLearning)

            // 7. Persist DifficultyState.
            difficultyStateRepository.upsert(newDifficulty)

            // 8. Append-only ReviewHistory.
            reviewHistoryRepository.insert(
                ReviewHistory(
                    id = UUID.randomUUID(),
                    sessionId = request.sessionId,
                    reviewAttemptId = request.reviewAttemptId,
                    conceptId = request.conceptId,
                    reviewedAt = request.reviewedAt,
                    isCorrect = request.isCorrect,
                    reviewType = request.reviewType
                )
            )

            SubmitReviewAnswerResult(
                learningState = updatedLearning,
                difficultyState = newDifficulty,
                transition = transition
            )
        }
    }
}
