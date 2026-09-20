package com.claudemani.domain.algorithm

import com.claudemani.domain.model.Stage
import java.time.Instant

/**
 * Output of the Learning Transition Algorithm (Algorithms v4.20,
 * "Learning Transition Algorithm" v1.1 FROZEN). Pure data — the
 * computing function `calculateLearningTransition` is added in Phase 3.
 *
 * Learning Transition owns exactly these four fields; nothing about
 * Difficulty is decided here.
 */
data class TransitionResult(
    val newStage: Stage,
    val nextReviewAt: Instant?,
    val hasPathFailure: Boolean,
    val monthlyWrongCount: Int
)
