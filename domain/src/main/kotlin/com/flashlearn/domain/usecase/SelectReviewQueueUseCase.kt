package com.flashlearn.domain.usecase

import com.flashlearn.domain.exception.DataIntegrityException
import com.flashlearn.domain.model.Concept
import com.flashlearn.domain.model.LearningState
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.model.Stage
import com.flashlearn.domain.model.VocabularyDifficulty
import com.flashlearn.domain.repository.ConceptRepository
import com.flashlearn.domain.repository.ConceptTagRepository
import com.flashlearn.domain.repository.DifficultyStateRepository
import com.flashlearn.domain.repository.LearningStateRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * All filters are optional (AND-combined when present). [languagePair] is
 * accepted for forward compatibility with the Algorithm's general
 * contract, but is currently a no-op: V1's Concept/LearningState carry no
 * languagePairId (Descriptions §19.1 — multi-active-pair support is a
 * documented Future Extension), so there is nothing yet to filter on.
 */
data class ReviewQueueFilters(
    val difficulty: VocabularyDifficulty? = null,
    val category: UUID? = null,
    val tag: UUID? = null,
    val languagePair: UUID? = null
)

/**
 * SelectReviewQueue (Algorithms v4.20, "REVIEW SCHEDULING / CARD SELECTION
 * ALGORITHM", Final). Purely read-only: selects, filters and orders
 * Concepts for a Review Session. Never mutates Stage, Difficulty,
 * nextReviewAt, or any other state.
 *
 * - DAILY/WEEKLY/MONTHLY: candidates are LearningStates in that Stage
 *   with nextReviewAt <= now, ordered nextReviewAt ASC then Concept.id ASC.
 * - LEARNED: ALL LearningStates in Stage.LEARNED, regardless of
 *   nextReviewAt (which is ignored entirely), shuffled.
 * - RANDOM (Appendix O.1 correction, folded into this algorithm): due
 *   DAILY+WEEKLY+MONTHLY concepts only (LEARNED excluded), shuffled.
 *
 * A LearningState whose Concept is missing or soft-deleted is silently
 * dropped (§5: "اگر Concept مرتبط پیدا نشود، آن Candidate حذف می‌شود").
 * A LearningState whose Concept HAS no DifficultyState, however, is a
 * DATA_INTEGRITY_ERROR (never silently guessed) — same invariant as
 * SubmitReviewAnswerUseCase.
 */
class SelectReviewQueueUseCase @Inject constructor(
    private val learningStateRepository: LearningStateRepository,
    private val difficultyStateRepository: DifficultyStateRepository,
    private val conceptRepository: ConceptRepository,
    private val conceptTagRepository: ConceptTagRepository
) {
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
        val learningStates: List<LearningState> = when (reviewType) {
            ReviewType.DAILY -> learningStateRepository.getDueByStage(Stage.DAILY, now)
            ReviewType.WEEKLY -> learningStateRepository.getDueByStage(Stage.WEEKLY, now)
            ReviewType.MONTHLY -> learningStateRepository.getDueByStage(Stage.MONTHLY, now)
            ReviewType.LEARNED -> learningStateRepository.getAllByStage(Stage.LEARNED)
            ReviewType.RANDOM -> learningStateRepository.getAllDueNonLearned(now)
        }

        val candidates = learningStates.mapNotNull { learningState ->
            val concept = conceptRepository.getById(learningState.conceptId) ?: return@mapNotNull null
            val difficulty = difficultyStateRepository.get(concept.id)
                ?: throw DataIntegrityException("DifficultyState not found for concept ${concept.id}")
            Candidate(concept, learningState, difficulty.current)
        }.filter { candidate ->
            (filters.difficulty == null || candidate.difficulty == filters.difficulty) &&
                (filters.category == null || candidate.concept.categoryId == filters.category) &&
                (
                    filters.tag == null ||
                        conceptTagRepository.getTagIdsForConcept(candidate.concept.id).contains(filters.tag)
                    )
            // filters.languagePair intentionally not applied — see ReviewQueueFilters KDoc.
        }.distinctBy { it.concept.id }

        val ordered = when (reviewType) {
            ReviewType.DAILY, ReviewType.WEEKLY, ReviewType.MONTHLY ->
                candidates.sortedWith(compareBy({ it.learningState.nextReviewAt }, { it.concept.id }))
            ReviewType.LEARNED, ReviewType.RANDOM ->
                candidates.shuffled()
        }

        return ordered.map { it.concept }
    }
}
