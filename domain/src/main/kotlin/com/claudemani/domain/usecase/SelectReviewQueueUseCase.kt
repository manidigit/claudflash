package com.claudemani.domain.usecase

import com.claudemani.domain.exception.DataIntegrityException
import com.claudemani.domain.model.Concept
import com.claudemani.domain.model.LearningState
import com.claudemani.domain.model.ReviewType
import com.claudemani.domain.model.Stage
import com.claudemani.domain.model.VocabularyDifficulty
import com.claudemani.domain.repository.ConceptRepository
import com.claudemani.domain.repository.ConceptTagRepository
import com.claudemani.domain.repository.DifficultyStateRepository
import com.claudemani.domain.repository.LearningStateRepository
import com.claudemani.domain.repository.ReviewHistoryRepository
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class ReviewQueueFilters(
    val difficulty: VocabularyDifficulty? = null,
    val category: UUID? = null,
    val tag: UUID? = null,
    val languagePair: UUID? = null
)

class SelectReviewQueueUseCase @Inject constructor(
    private val learningStateRepository: LearningStateRepository,
    private val difficultyStateRepository: DifficultyStateRepository,
    private val conceptRepository: ConceptRepository,
    private val conceptTagRepository: ConceptTagRepository,
    private val reviewHistoryRepository: ReviewHistoryRepository
) {
    constructor(
        learningStateRepository: LearningStateRepository,
        difficultyStateRepository: DifficultyStateRepository,
        conceptRepository: ConceptRepository,
        conceptTagRepository: ConceptTagRepository
    ) : this(
        learningStateRepository,
        difficultyStateRepository,
        conceptRepository,
        conceptTagRepository,
        EmptyReviewHistoryRepository
    )

    private data class Candidate(
        val concept: Concept,
        val learningState: LearningState,
        val difficulty: VocabularyDifficulty
    )

    suspend operator fun invoke(
        reviewType: ReviewType,
        filters: ReviewQueueFilters = ReviewQueueFilters(),
        now: Instant
    ): List<Concept> {
        val learningStates = when (reviewType) {
            ReviewType.DAILY -> learningStateRepository.getDueByStage(Stage.DAILY, now)
            ReviewType.WEEKLY -> learningStateRepository.getDueByStage(Stage.WEEKLY, now)
            ReviewType.MONTHLY -> learningStateRepository.getDueByStage(Stage.MONTHLY, now)
            ReviewType.LEARNED -> learningStateRepository.getAllByStage(Stage.LEARNED)
            ReviewType.RANDOM -> learningStateRepository.getAllDueNonLearned(now)
        }

        val localToday = now.atZone(ZoneId.systemDefault()).toLocalDate()
        val practicedToday = reviewHistoryRepository.getAll()
            .asSequence()
            .filter {
                it.reviewedAt.atZone(ZoneId.systemDefault()).toLocalDate() == localToday
            }
            .map { it.conceptId }
            .toSet()

        val candidates = ArrayList<Candidate>(learningStates.size)
        for (learningState in learningStates) {
            if (learningState.conceptId in practicedToday) continue

            val concept = conceptRepository.getById(learningState.conceptId) ?: continue
            val difficulty = difficultyStateRepository.get(concept.id)
                ?: throw DataIntegrityException(
                    "DifficultyState not found for concept " + concept.id
                )

            if (filters.difficulty != null && difficulty.current != filters.difficulty) continue
            if (filters.category != null && concept.categoryId != filters.category) continue

            if (filters.tag != null) {
                val tagIds = conceptTagRepository.getTagIdsForConcept(concept.id)
                if (filters.tag !in tagIds) continue
            }

            candidates += Candidate(concept, learningState, difficulty.current)
        }

        val uniqueCandidates = candidates.distinctBy { it.concept.id }

        val ordered = when (reviewType) {
            ReviewType.DAILY, ReviewType.WEEKLY, ReviewType.MONTHLY ->
                uniqueCandidates.sortedWith(
                    compareBy<Candidate> { it.learningState.nextReviewAt }
                        .thenBy { it.concept.id }
                )
            ReviewType.LEARNED, ReviewType.RANDOM -> uniqueCandidates.shuffled()
        }

        return ordered.map { it.concept }
    }

    private object EmptyReviewHistoryRepository : ReviewHistoryRepository {
        override suspend fun insert(history: com.claudemani.domain.model.ReviewHistory) = Unit
        override suspend fun getById(id: UUID): com.claudemani.domain.model.ReviewHistory? = null
        override suspend fun getByConceptId(conceptId: UUID) = emptyList<com.claudemani.domain.model.ReviewHistory>()
        override suspend fun getBySessionId(sessionId: UUID) = emptyList<com.claudemani.domain.model.ReviewHistory>()
        override suspend fun existsByAttemptId(sessionId: UUID, attemptId: UUID) = false
        override suspend fun getDistinctConceptIds() = emptyList<UUID>()
        override suspend fun getAll() = emptyList<com.claudemani.domain.model.ReviewHistory>()
    }
}