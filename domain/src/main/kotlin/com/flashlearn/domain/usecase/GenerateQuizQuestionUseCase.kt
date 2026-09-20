package com.flashlearn.domain.usecase

import com.flashlearn.domain.model.Concept
import com.flashlearn.domain.model.Content
import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.model.LanguagePair
import com.flashlearn.domain.model.QuizDifficulty
import com.flashlearn.domain.model.VocabularyDifficulty
import com.flashlearn.domain.model.computeCanonicalKey
import com.flashlearn.domain.repository.ConceptRepository
import com.flashlearn.domain.repository.ContentRepository
import com.flashlearn.domain.repository.DifficultyStateRepository
import java.util.UUID
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
 *
 * [quizDifficulty] (Algorithms O.3 Backlog #1/#2/#3, Phase 38) is a
 * SEPARATE, later re-ranking step over that SAME pool — it never changes
 * which Contents are eligible, only which 3 are preferred once picking
 * the final winners:
 * - [QuizDifficulty.EASY]: Contents from a *different* Category than
 *   [concept] are preferred ("نسبتاً متفاوت‌تر").
 * - [QuizDifficulty.MEDIUM] (the default — preserves this Use Case's
 *   exact pre-existing behavior byte-for-byte): no category preference
 *   at all. The Algorithm's own text for MEDIUM ("Category یا
 *   Subcategory مشابه") depends on a Subcategory/similarity concept that
 *   does not exist anywhere in this domain model (Category is flat, no
 *   hierarchy) — inventing one would be exactly the kind of unspecified
 *   product decision this project avoids making unilaterally, so MEDIUM
 *   is defined here as "no reordering", not a guessed approximation.
 * - [QuizDifficulty.HARD]: Contents from the *same* Category as
 *   [concept] are preferred ("همان ... Category در اولویت است").
 * A Concept with `categoryId == null`, or too few same/different-category
 * candidates to fill 3 slots, silently falls back to the rest of the
 * pool — this preference can never be the reason a question falls back
 * to [QuizGenerationResult.FlashcardFallback]; only "fewer than 3 valid
 * Distractors at all" can.
 */
class GenerateQuizQuestionUseCase @Inject constructor(
    private val contentRepository: ContentRepository,
    private val conceptRepository: ConceptRepository,
    private val difficultyStateRepository: DifficultyStateRepository
) {
    suspend operator fun invoke(
        concept: Concept,
        activeLanguagePair: LanguagePair,
        difficultyState: DifficultyState,
        quizDifficulty: QuizDifficulty = QuizDifficulty.MEDIUM
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

        // conceptId -> Concept, filled in alongside pool-building below so the later
        // category-preference step never has to re-query for the same Concept twice.
        val conceptCache = mutableMapOf<UUID, Concept>()

        suspend fun findCandidates(difficultyFilter: VocabularyDifficulty?): List<Content> {
            if (otherConceptsPool.isEmpty()) return emptyList()
            val result = mutableListOf<Content>()
            for (content in otherConceptsPool) {
                if (normalize(content.text) == correctNormalized) continue
                // getById only returns active (non soft-deleted) Concepts — an inactive
                // Concept's translations are silently excluded from the Distractor pool.
                val candidateConcept = conceptCache.getOrPut(content.conceptId) {
                    conceptRepository.getById(content.conceptId) ?: continue
                }
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

        val wrongOptions = pickPreferringCategory(candidates, concept, quizDifficulty, conceptCache)
        val options = (listOf(correctContent.text) + wrongOptions.map { it.text }).shuffled()

        return QuizGenerationResult.QuizQuestion(
            promptText = promptContent.text,
            correctAnswerText = correctContent.text,
            options = options
        )
    }

    /** conceptCache is already fully populated for every Content in [pool] by the caller's findCandidates loop. */
    private fun pickPreferringCategory(
        pool: List<Content>,
        concept: Concept,
        quizDifficulty: QuizDifficulty,
        conceptCache: Map<UUID, Concept>
    ): List<Content> {
        if (quizDifficulty == QuizDifficulty.MEDIUM || concept.categoryId == null) {
            return pool.shuffled().take(3)
        }

        val sameCategory = pool.filter { conceptCache[it.conceptId]?.categoryId == concept.categoryId }
        val otherCategory = pool.filter { conceptCache[it.conceptId]?.categoryId != concept.categoryId }

        val ordered = when (quizDifficulty) {
            QuizDifficulty.HARD -> sameCategory.shuffled() + otherCategory.shuffled()
            QuizDifficulty.EASY -> otherCategory.shuffled() + sameCategory.shuffled()
            QuizDifficulty.MEDIUM -> pool.shuffled() // unreachable, handled above; exhaustive `when` requires it
        }
        return ordered.take(3)
    }

    /**
     * Text-equality normalization for Distractor comparison (§ Normalization).
     * Reuses [computeCanonicalKey] — same "trim + lowercase + collapse
     * whitespace, keep accents/punctuation" rule already used for
     * canonicalKey; this is purely a comparison helper, not a persisted field.
     */
    private fun normalize(text: String): String = computeCanonicalKey(text)
}
