package com.flashlearn.domain.usecase

import com.flashlearn.domain.model.Concept
import com.flashlearn.domain.model.Content
import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.model.LanguagePair
import com.flashlearn.domain.model.VocabularyDifficulty
import com.flashlearn.domain.model.computeCanonicalKey
import com.flashlearn.domain.repository.ConceptRepository
import com.flashlearn.domain.repository.ContentRepository
import com.flashlearn.domain.repository.DifficultyStateRepository
import javax.inject.Inject

/**
 * Result of [GenerateQuizQuestionUseCase] — exactly one of the two shapes
 * defined by the Algorithm (Algorithms v4.20 §12.4).
 */
sealed interface QuizGenerationResult {
    /** [options] has exactly 4 entries, already shuffled; [correctAnswerText] is one of them. */
    data class QuizQuestion(
        val promptText: String,
        val correctAnswerText: String,
        val options: List<String>
    ) : QuizGenerationResult

    /** Fewer than 3 valid Distractors were found even using the whole bank. */
    data object FlashcardFallback : QuizGenerationResult
}

/**
 * GenerateQuizQuestion (Algorithms v4.20 §12.4 "الگوریتم نهایی — Quiz
 * Question Generation"). Read-only: builds a 4-option multiple-choice
 * question for an already-selected Concept, or reports that a Flashcard
 * fallback is required. Never mutates Concept, Content, LearningState or
 * DifficultyState, and never decides Difficulty/Stage/Scheduling — those
 * belong to their own independent algorithms.
 *
 * Distractor pool rules (§ مرحله ۲ و قواعد قطعی):
 * - Content must be in [LanguagePair.targetLanguage].
 * - Content must belong to a *different*, currently active Concept —
 *   multiple translations of the SAME Concept can never be Distractors.
 * - Content whose normalized text equals the correct answer's normalized
 *   text is excluded (never a duplicate-looking option).
 * - Candidates are de-duplicated by normalized text (so two different
 *   Concepts sharing the same translation text only occupy one slot).
 *
 * Pool is built in three widening passes without ever discarding a
 * previous pass's candidates (§۲.۱–۲.۳):
 * 1. Exact same Difficulty as [difficultyState.current].
 * 2. + one Difficulty level up and one level down (whichever exist).
 * 3. + the entire bank, no Difficulty filter at all.
 *
 * If fewer than 3 valid Distractors exist even after step 3 →
 * [QuizGenerationResult.FlashcardFallback] (§ قانون ۱۸).
 */
class GenerateQuizQuestionUseCase @Inject constructor(
    private val contentRepository: ContentRepository,
    private val conceptRepository: ConceptRepository,
    private val difficultyStateRepository: DifficultyStateRepository
) {
    suspend operator fun invoke(
        concept: Concept,
        activeLanguagePair: LanguagePair,
        difficultyState: DifficultyState
    ): QuizGenerationResult {
        val promptContent =
            contentRepository.getByConceptIdAndLanguage(concept.id, activeLanguagePair.sourceLanguage)
                ?: return QuizGenerationResult.FlashcardFallback
        val correctContent =
            contentRepository.getByConceptIdAndLanguage(concept.id, activeLanguagePair.targetLanguage)
                ?: return QuizGenerationResult.FlashcardFallback

        val correctNormalized = normalize(correctContent.text)

        // Every Content in the target language that does not belong to this Concept.
        // Multiple translations of THIS Concept are excluded entirely, up front — they
        // must never become Distractors regardless of Difficulty filtering below.
        val otherConceptsPool = contentRepository.getAllByLanguageCode(activeLanguagePair.targetLanguage)
            .filter { it.conceptId != concept.id }

        suspend fun findCandidates(difficultyFilter: VocabularyDifficulty?): List<Content> {
            if (otherConceptsPool.isEmpty()) return emptyList()
            val result = mutableListOf<Content>()
            for (content in otherConceptsPool) {
                if (normalize(content.text) == correctNormalized) continue
                // getById only returns active (non soft-deleted) Concepts — an inactive
                // Concept's translations are silently excluded from the Distractor pool.
                val candidateConcept = conceptRepository.getById(content.conceptId) ?: continue
                if (difficultyFilter != null) {
                    val candidateDifficulty = difficultyStateRepository.get(candidateConcept.id)
                        ?: continue
                    if (candidateDifficulty.current != difficultyFilter) continue
                }
                result.add(content)
            }
            return result
        }

        fun uniqueByNormalizedText(list: List<Content>): List<Content> =
            list.distinctBy { normalize(it.text) }

        // Step 2.1 — same Difficulty as the Concept currently being quizzed.
        var candidates = uniqueByNormalizedText(findCandidates(difficultyState.current))

        // Step 2.2 — widen to one adjacent level up and down (bounded at EASY/VERY_HARD).
        if (candidates.size < 3) {
            val levels = VocabularyDifficulty.entries
            val idx = difficultyState.current.ordinal
            val adjacentLevels = listOfNotNull(levels.getOrNull(idx - 1), levels.getOrNull(idx + 1))
            val extra = adjacentLevels.flatMap { findCandidates(it) }
            candidates = uniqueByNormalizedText(candidates + extra)
        }

        // Step 2.3 — no Difficulty filter at all, use the whole bank.
        if (candidates.size < 3) {
            val extra = findCandidates(null)
            candidates = uniqueByNormalizedText(candidates + extra)
        }

        if (candidates.size < 3) return QuizGenerationResult.FlashcardFallback

        val wrongOptions = candidates.shuffled().take(3)
        val options = (listOf(correctContent.text) + wrongOptions.map { it.text }).shuffled()

        return QuizGenerationResult.QuizQuestion(
            promptText = promptContent.text,
            correctAnswerText = correctContent.text,
            options = options
        )
    }

    /**
     * Text-equality normalization for Distractor comparison (§ Normalization).
     * Reuses [computeCanonicalKey] — same "trim + lowercase + collapse
     * whitespace, keep accents/punctuation" rule already used for
     * canonicalKey; this is purely a comparison helper, not a persisted field.
     */
    private fun normalize(text: String): String = computeCanonicalKey(text)
}
