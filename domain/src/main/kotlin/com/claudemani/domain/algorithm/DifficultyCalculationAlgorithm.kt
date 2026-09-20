package com.claudemani.domain.algorithm

import com.claudemani.domain.model.DifficultyState
import com.claudemani.domain.model.ReviewType
import com.claudemani.domain.model.VocabularyDifficulty

/**
 * Calculates Vocabulary Difficulty only.
 *
 * LEARNED-stage protection belongs to SubmitReviewAnswerUseCase, which
 * skips this algorithm entirely when the Concept's LearningState is
 * already LEARNED. ReviewType.LEARNED itself does not disable the normal
 * difficulty counter mechanics; this keeps the pure algorithm consistent
 * when called directly and prevents a hidden second rule from diverging
 * from its caller.
 */
fun calculateDifficulty(
    state: DifficultyState,
    isCorrect: Boolean,
    reviewType: ReviewType,
    monthlyWrongCountBefore: Int,
    threshold: Int = 3
): DifficultyState {
    require(threshold > 0) { "threshold must be positive" }

    var level = state.current
    var cc = state.consecutiveCorrect
    var cw = state.consecutiveWrong
    var reached = state.hasReachedVeryHard

    when {
        reviewType == ReviewType.WEEKLY && !isCorrect -> {
            level = if (level.ordinal < VocabularyDifficulty.MEDIUM.ordinal) {
                VocabularyDifficulty.MEDIUM
            } else {
                level
            }
            cc = 0
            cw = 0
        }

        reviewType == ReviewType.MONTHLY && !isCorrect -> {
            level = if (monthlyWrongCountBefore == 0) {
                VocabularyDifficulty.HARD
            } else {
                VocabularyDifficulty.VERY_HARD
            }
            cc = 0
            cw = 0
        }

        isCorrect -> {
            cw = 0
            val next = cc + 1
            if (next >= threshold) {
                level = easier(level)
                cc = 0
            } else {
                cc = next
            }
        }

        else -> {
            cc = 0
            val next = cw + 1
            if (next >= threshold) {
                level = harder(level)
                cw = 0
            } else {
                cw = next
            }
        }
    }

    if (level == VocabularyDifficulty.VERY_HARD) {
        reached = true
    }

    return state.copy(
        current = level,
        consecutiveCorrect = cc,
        consecutiveWrong = cw,
        hasReachedVeryHard = reached
    )
}

private fun easier(difficulty: VocabularyDifficulty): VocabularyDifficulty = when (difficulty) {
    VocabularyDifficulty.VERY_HARD -> VocabularyDifficulty.HARD
    VocabularyDifficulty.HARD -> VocabularyDifficulty.MEDIUM
    VocabularyDifficulty.MEDIUM -> VocabularyDifficulty.EASY
    VocabularyDifficulty.EASY -> VocabularyDifficulty.EASY
}

private fun harder(difficulty: VocabularyDifficulty): VocabularyDifficulty = when (difficulty) {
    VocabularyDifficulty.EASY -> VocabularyDifficulty.MEDIUM
    VocabularyDifficulty.MEDIUM -> VocabularyDifficulty.HARD
    VocabularyDifficulty.HARD -> VocabularyDifficulty.VERY_HARD
    VocabularyDifficulty.VERY_HARD -> VocabularyDifficulty.VERY_HARD
}