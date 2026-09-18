package com.flashlearn.domain.usecase

import com.flashlearn.domain.model.Stage
import com.flashlearn.domain.repository.ConceptRepository
import com.flashlearn.domain.repository.LearningStateRepository
import com.flashlearn.domain.repository.ReviewHistoryRepository
import javax.inject.Inject

/** GetBasicStatistics output (Algorithms v4.20 §11.1). */
data class BasicStatistics(
    val totalActiveWords: Int,
    val practicedWords: Int,
    val unpracticedWords: Int,
    val learnedWords: Int
)

/**
 * GetBasicStatistics (Algorithms v4.20 §11.1). All four metrics are
 * computed strictly over active Concepts — a soft-deleted Concept's
 * review history or learning state never counts. Read-only.
 */
class GetBasicStatisticsUseCase @Inject constructor(
    private val conceptRepository: ConceptRepository,
    private val reviewHistoryRepository: ReviewHistoryRepository,
    private val learningStateRepository: LearningStateRepository
) {
    suspend operator fun invoke(): BasicStatistics {
        val activeIds = conceptRepository.getAllActive().map { it.id }.toSet()
        val totalActiveWords = activeIds.size

        val practicedWords = reviewHistoryRepository.getDistinctConceptIds().count { it in activeIds }
        val unpracticedWords = maxOf(0, totalActiveWords - practicedWords)

        val learnedWords = learningStateRepository.getAll()
            .count { it.stage == Stage.LEARNED && it.conceptId in activeIds }

        return BasicStatistics(
            totalActiveWords = totalActiveWords,
            practicedWords = practicedWords,
            unpracticedWords = unpracticedWords,
            learnedWords = learnedWords
        )
    }
}
