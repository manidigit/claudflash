package com.flashlearn.domain.algorithm

import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.model.VocabularyDifficulty

fun calculateDifficulty(
    state: DifficultyState,
    isCorrect: Boolean,
    reviewType: ReviewType,
    monthlyWrongCountBefore: Int,
    threshold: Int = 3
): DifficultyState {
    if (reviewType == ReviewType.LEARNED) return state

    var level = state.current
    var cc = state.consecutiveCorrect
    var cw = state.consecutiveWrong
    var reached = state.hasReachedVeryHard

    when {
        reviewType == ReviewType.WEEKLY && !isCorrect -> {
            level = if (level.ordinal < VocabularyDifficulty.MEDIUM.ordinal) VocabularyDifficulty.MEDIUM else level
            cc = 0
            cw = 0
        }
        reviewType == ReviewType.MONTHLY && !isCorrect -> {
            level = if (monthlyWrongCountBefore == 0) VocabularyDifficulty.HARD else VocabularyDifficulty.VERY_HARD
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

    if (level == VocabularyDifficulty.VERY_HARD) reached = true

    return state.copy(
        current = level,
        consecutiveCorrect = cc,
        consecutiveWrong = cw,
        hasReachedVeryHard = reached
    )
}

private fun easier(d: VocabularyDifficulty): VocabularyDifficulty = when (d) {
    VocabularyDifficulty.VERY_HARD -> VocabularyDifficulty.HARD
    VocabularyDifficulty.HARD -> VocabularyDifficulty.MEDIUM
    VocabularyDifficulty.MEDIUM -> VocabularyDifficulty.EASY
    VocabularyDifficulty.EASY -> VocabularyDifficulty.EASY
}

private fun harder(d: VocabularyDifficulty): VocabularyDifficulty = when (d) {
    VocabularyDifficulty.EASY -> VocabularyDifficulty.MEDIUM
    VocabularyDifficulty.MEDIUM -> VocabularyDifficulty.HARD
    VocabularyDifficulty.HARD -> VocabularyDifficulty.VERY_HARD
    VocabularyDifficulty.VERY_HARD -> VocabularyDifficulty.VERY_HARD
}