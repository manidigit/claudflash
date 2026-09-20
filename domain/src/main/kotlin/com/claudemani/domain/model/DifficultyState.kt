package com.claudemani.domain.model

import java.util.UUID

/**
 * Owns Vocabulary Difficulty state, fully independent of [Stage]/[LearningState].
 *
 * - [current] is EASY/MEDIUM/HARD/VERY_HARD, initialized to EASY on Concept creation.
 * - [consecutiveCorrect] and [consecutiveWrong] can never both be > 0 at once,
 *   and both reset to 0 whenever [current] changes (forced or normal path).
 * - [hasReachedVeryHard] is a monotonic historical flag: becomes true once
 *   VERY_HARD is reached and never reverts to false on a normal decrease.
 */
data class DifficultyState(
    val id: UUID,
    val conceptId: UUID,
    val current: VocabularyDifficulty,
    val consecutiveCorrect: Int,
    val consecutiveWrong: Int,
    val hasReachedVeryHard: Boolean
)
