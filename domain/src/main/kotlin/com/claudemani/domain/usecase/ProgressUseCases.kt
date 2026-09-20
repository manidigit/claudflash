package com.claudemani.domain.usecase

import com.claudemani.domain.model.ProgressSummary
import com.claudemani.domain.model.Stage
import com.claudemani.domain.repository.ConceptRepository
import com.claudemani.domain.repository.LearningStateRepository
import com.claudemani.domain.repository.ReviewHistoryRepository
import java.time.Instant
import javax.inject.Inject

/**
 * GetProgressSummary (Descriptions v4.10/Phase 6 "Settings/Difficulty/
 * Progress"): active Concepts + LearningState as the sole source of
 * truth. No duplicated Progress persistence — this is a pure read-side
 * aggregation, recomputed on every call. Read-only.
 */
class GetProgressSummaryUseCase @Inject constructor(
    private val conceptRepository: ConceptRepository,
    private val learningStateRepository: LearningStateRepository
) {
    suspend operator fun invoke(now: Instant): ProgressSummary {
        val activeConcepts = conceptRepository.getAllActive()
        val activeIds = activeConcepts.map { it.id }.toSet()

        if (activeIds.isEmpty()) {
            return ProgressSummary(
                activeConceptCount = 0,
                learnedCount = 0,
                dueCount = 0,
                totalCorrect = 0,
                totalWrong = 0,
                accuracyPercentage = 0.0
            )
        }

        val activeStates = learningStateRepository.getAll().filter { it.conceptId in activeIds }
        val learnedCount = activeStates.count { it.stage == Stage.LEARNED }
        val dueCount = learningStateRepository.getAllDueNonLearned(now).count { it.conceptId in activeIds }
        val totalCorrect = activeStates.sumOf { it.totalCorrect }
        val totalWrong = activeStates.sumOf { it.totalWrong }
        val totalAnswers = totalCorrect + totalWrong
        val accuracy = if (totalAnswers == 0) 0.0 else (totalCorrect.toDouble() / totalAnswers) * 100.0

        return ProgressSummary(
            activeConceptCount = activeConcepts.size,
            learnedCount = learnedCount,
            dueCount = dueCount,
            totalCorrect = totalCorrect,
            totalWrong = totalWrong,
            accuracyPercentage = accuracy
        )
    }
}

/**
 * CalculateProgressPercentage (Algorithms v4.20 §11.2). Stage-based score
 * per active Concept (LEARNED=100, MONTHLY=80, WEEKLY=60; DAILY or
 * no-LearningState = 15 if the word has any ReviewHistory, else 0), averaged across all
 * active Concepts — replaces the plain "learned ÷ total" formula so
 * progress is visible before a word ever reaches LEARNED. Read-only.
 *
 * The "state doesn't exist" branch is written exactly as specified by the
 * Algorithm even though CreateConcept/SubmitReviewAnswer always create a
 * LearningState atomically with its Concept in normal operation — this
 * UseCase deliberately stays resilient (15 or 0 points) rather than
 * throwing, since a Statistics/Progress screen must never crash the app
 * over one straggler record.
 */
class CalculateProgressPercentageUseCase @Inject constructor(
    private val conceptRepository: ConceptRepository,
    private val learningStateRepository: LearningStateRepository,
    private val reviewHistoryRepository: ReviewHistoryRepository
) {
    suspend operator fun invoke(): Double {
        val activeConcepts = conceptRepository.getAllActive()
        if (activeConcepts.isEmpty()) return 0.0

        val stateByConceptId = learningStateRepository.getAll().associateBy { it.conceptId }
        val practicedConceptIds = reviewHistoryRepository.getDistinctConceptIds().toSet()

        var totalScore = 0
        for (concept in activeConcepts) {
            val stage = stateByConceptId[concept.id]?.stage
            totalScore += when (stage) {
                Stage.LEARNED -> 100
                Stage.MONTHLY -> 80
                Stage.WEEKLY -> 60
                // DAILY is the *starting* stage of every new word, so it must not score
                // by stage alone (that made a fresh, never-reviewed library show 35%).
                // Descriptions table: never practiced = 0%, first practice = 15%.
                Stage.DAILY, null -> if (concept.id in practicedConceptIds) 15 else 0
            }
        }

        return totalScore.toDouble() / activeConcepts.size
    }
}
