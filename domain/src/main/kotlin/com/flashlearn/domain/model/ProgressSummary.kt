package com.flashlearn.domain.model

/**
 * Aggregate snapshot returned by GetProgressSummaryUseCase (Phase 15).
 * Read-only aggregation over active Concepts + LearningState; never
 * persisted separately (no duplicated Progress table).
 */
data class ProgressSummary(
    val activeConceptCount: Int,
    val learnedCount: Int,
    val dueCount: Int,
    val totalCorrect: Int,
    val totalWrong: Int,
    val accuracyPercentage: Double
)
