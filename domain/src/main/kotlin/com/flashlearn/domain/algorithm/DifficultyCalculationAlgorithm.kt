package com.flashlearn.domain.algorithm

import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.model.VocabularyDifficulty

/**
 * Difficulty Calculation Algorithm — Version 1.1, Status: FROZEN
 * (Algorithms v4.20).
 *
 * Pure function. Computes and updates [DifficultyState] only — it never
 * decides Stage, nextReviewAt or Review Scheduling (that is
 * [calculateLearningTransition]'s job). Fully independent of Learning
 * Stage, deterministic, offline, no randomness.
 *
 * CALLER CONTRACT (enforced by SubmitReviewAnswerUseCase, not here):
 * - This function must NOT be called at all when the Concept's
 *   LearningState.stage == LEARNED (§12.2) — LEARNED review never touches
 *   Difficulty.
 * - [monthlyWrongCountBefore] MUST be the value from LearningState
 *   *before* [calculateLearningTransition] is applied for the same
 *   review event, so "first MONTHLY failure" vs "subsequent" is detected
 *   correctly.
 * - Exactly one of Difficulty Calculation's two paths (Forced Update or
 *   Normal threshold logic) runs per review event — never both.
 *
 * Priority order (§5 Forced Updates take precedence over §6 normal counters):
 * 1. WEEKLY + Wrong  → at least MEDIUM (never downgrades an existing
 *    higher level), both counters reset to 0.
 * 2. MONTHLY + Wrong → first failure (monthlyWrongCountBefore == 0) → HARD;
 *    subsequent failures → VERY_HARD. Both counters reset to 0.
 *    (Forced updates only apply to real WEEKLY/MONTHLY review events —
 *    not RANDOM or LEARNED review types, which always fall through to
 *    the normal consecutive-counter path below.)
 * 3. Otherwise: normal path — consecutive correct/wrong counters against
 *    [threshold] (default 3). An answer of the opposite kind zeros the
 *    other counter. Reaching threshold moves Difficulty exactly one step
 *    and resets both counters to 0.
 * 4. Any level change (forced or normal) resets both consecutiveCorrect
 *    and consecutiveWrong to 0.
 * 5. [DifficultyState.hasReachedVeryHard] is monotonic: once VERY_HARD is
 *    reached it stays true forever, even after Difficulty later decreases.
 */
fun calculateDifficulty(
    state: DifficultyState,
    isCorrect: Boolean,
    reviewType: ReviewType,
    monthlyWrongCountBefore: Int,
    threshold: Int = 3
): DifficultyState {
    var newLevel = state.current
    var cc = state.consecutiveCorrect
    var cw = state.consecutiveWrong
    var reachedVeryHard = state.hasReachedVeryHard

    when {
        // ---------- 1. Forced Update: WEEKLY wrong ----------
        reviewType == ReviewType.WEEKLY && !isCorrect -> {
            newLevel = if (state.current.ordinal < VocabularyDifficulty.MEDIUM.ordinal) {
                VocabularyDifficulty.MEDIUM
            } else {
                state.current
            }
            cc = 0
            cw = 0
        }

        // ---------- 2. Forced Update: MONTHLY wrong ----------
        reviewType == ReviewType.MONTHLY && !isCorrect -> {
            newLevel = if (monthlyWrongCountBefore == 0) {
                VocabularyDifficulty.HARD
            } else {
                VocabularyDifficulty.VERY_HARD
            }
            cc = 0
            cw = 0
        }

        // ---------- 3. Normal consecutive-counter path ----------
        else -> {
            if (isCorrect) {
                cw = 0
                val newCC = cc + 1
                if (newCC >= threshold) {
                    newLevel = oneStepEasier(state.current)
                    cc = 0
                } else {
                    newLevel = state.current
                    cc = newCC
                }
            } else {
                cc = 0
                val newCW = cw + 1
                if (newCW >= threshold) {
                    newLevel = oneStepHarder(state.current)
                    cw = 0
                } else {
                    newLevel = state.current
                    cw = newCW
                }
            }
        }
    }

    // ---------- Monotonic hasReachedVeryHard ----------
    if (newLevel == VocabularyDifficulty.VERY_HARD) {
        reachedVeryHard = true
    }

    return state.copy(
        current = newLevel,
        consecutiveCorrect = cc,
        consecutiveWrong = cw,
        hasReachedVeryHard = reachedVeryHard
    )
}

/** VERY_HARD→HARD→MEDIUM→EASY→EASY (floor: never drops below EASY). */
private fun oneStepEasier(d: VocabularyDifficulty): VocabularyDifficulty = when (d) {
    VocabularyDifficulty.VERY_HARD -> VocabularyDifficulty.HARD
    VocabularyDifficulty.HARD -> VocabularyDifficulty.MEDIUM
    VocabularyDifficulty.MEDIUM -> VocabularyDifficulty.EASY
    VocabularyDifficulty.EASY -> VocabularyDifficulty.EASY
}

/** EASY→MEDIUM→HARD→VERY_HARD→VERY_HARD (ceiling: never exceeds VERY_HARD). */
private fun oneStepHarder(d: VocabularyDifficulty): VocabularyDifficulty = when (d) {
    VocabularyDifficulty.EASY -> VocabularyDifficulty.MEDIUM
    VocabularyDifficulty.MEDIUM -> VocabularyDifficulty.HARD
    VocabularyDifficulty.HARD -> VocabularyDifficulty.VERY_HARD
    VocabularyDifficulty.VERY_HARD -> VocabularyDifficulty.VERY_HARD
}
