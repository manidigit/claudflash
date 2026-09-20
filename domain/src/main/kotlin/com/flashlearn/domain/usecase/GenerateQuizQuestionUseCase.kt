package com.flashlearn.domain.usecase

import com.flashlearn.domain.model.Content
import com.flashlearn.domain.model.Concept
import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.model.EntryType
import com.flashlearn.domain.model.QuizDifficulty
import com.flashlearn.domain.model.VocabularyDifficulty
import com.flashlearn.domain.model.computeCanonicalKey
import com.flashlearn.domain.model.LanguagePair
import com.flashlearn.domain.repository.ContentRepository
import com.flashlearn.domain.repository.ConceptRepository
import com.flashlearn.domain.repository.DifficultyStateRepository
import java.text.Normalizer
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

private fun normalizeQuizText(text: String): String =
    Normalizer.normalize(text.trim().replace(Regex("\\s+"), " "), Normalizer.Form.NFC)
        .lowercase(Locale.ROOT)

private fun quizTokens(text: String): Set<String> =
    normalizeQuizText(text)
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotBlank() }
        .toSet()

private fun levenshteinSimilarity(a: String, b: String): Double {
    if (a == b) return 1.0
    if (a.isEmpty() || b.isEmpty()) return 0.0
    var previous = IntArray(b.length + 1) { it }
    var current = IntArray(b.length + 1)
    for (i in a.indices) {
        current[0] = i + 1
        for (j in b.indices) {
            val substitution = previous[j] + if (a[i] == b[j]) 0 else 1
            current[j + 1] = min(min(previous[j + 1] + 1, current[j] + 1), substitution)
        }
        val tmp = previous
        previous = current
        current = tmp
    }
    return 1.0 - previous[b.length].toDouble() / max(a.length, b.length).toDouble()
}

private fun ngramSimilarity(a: String, b: String, n: Int = 2): Double {
    fun grams(value: String): Set<String> =
        if (value.length <= n) setOf(value) else value.windowed(n).toSet()
    val left = grams(a)
    val right = grams(b)
    if (left.isEmpty() && right.isEmpty()) return 1.0
    if (left.isEmpty() || right.isEmpty()) return 0.0
    return left.intersect(right).size.toDouble() / left.union(right).size
}

private fun lexicalSimilarity(a: String, b: String): Double {
    val left = normalizeQuizText(a)
    val right = normalizeQuizText(b)
    val lt = quizTokens(left)
    val rt = quizTokens(right)
    val token = if (lt.isEmpty() && rt.isEmpty()) 1.0
    else if (lt.isEmpty() || rt.isEmpty()) 0.0
    else lt.intersect(rt).size.toDouble() / lt.union(rt).size
    return (token * 0.45 + levenshteinSimilarity(left, right) * 0.35 +
        ngramSimilarity(left, right) * 0.20).coerceIn(0.0, 1.0)
}

sealed interface QuizGenerationResult {
    data class QuizQuestion(
        val promptText: String,
        val correctAnswerText: String,
        val options: List<String>
    ) : QuizGenerationResult {
        init {
            require(promptText.isNotBlank())
            require(correctAnswerText.isNotBlank())
            require(options.size == 4)
            require(options.all { it.isNotBlank() })
            require(options.map(::normalizeQuizText).distinct().size == 4)
            require(options.count { normalizeQuizText(it) == normalizeQuizText(correctAnswerText) } == 1)
        }
    }

    data object FlashcardFallback : QuizGenerationResult
}

private data class QuizBank(
    val contents: List<Content>,
    val concepts: List<Concept>,
    val difficultiesById: Map<UUID, DifficultyState>
)

private data class DistractorCandidate(
    val displayText: String,
    val canonicalKeys: Set<String>,
    val vocabularyDifficultyDistance: Int,
    val categoryMatch: Boolean,
    val entryTypeMatch: Boolean,
    val lexicalSimilarity: Double
)

class GenerateQuizQuestionUseCase @Inject constructor(
    private val contentRepository: ContentRepository,
    private val conceptRepository: ConceptRepository,
    private val difficultyStateRepository: DifficultyStateRepository
) {
    private var bank: QuizBank? = null

    suspend fun refreshBank() {
        bank = QuizBank(
            contents = contentRepository.getAll(),
            concepts = conceptRepository.getAllActive(),
            difficultiesById = difficultyStateRepository.getAll().associateBy { it.conceptId }
        )
    }

    suspend operator fun invoke(
        concept: Concept,
        activeLanguagePair: LanguagePair,
        difficultyState: DifficultyState?,
        quizDifficulty: QuizDifficulty = QuizDifficulty.MEDIUM,
        excludedDistractorTexts: Set<String> = emptySet()
    ): QuizGenerationResult {
        if (!concept.active) return QuizGenerationResult.FlashcardFallback

        val snapshot = bank ?: run {
            refreshBank()
            bank!!
        }

        val contentsByConcept = snapshot.contents.groupBy { it.conceptId }
        val conceptContents = contentsByConcept[concept.id].orEmpty()

        val prompt = conceptContents
            .filter { it.languageCode.equals(activeLanguagePair.sourceLanguage, true) && it.text.isNotBlank() }
            .minByOrNull { it.id.toString() }
            ?: return QuizGenerationResult.FlashcardFallback

        val targetTranslations = conceptContents
            .filter { it.languageCode.equals(activeLanguagePair.targetLanguage, true) && it.text.isNotBlank() }
            .sortedBy { it.id.toString() }
            .distinctBy { normalizeQuizText(it.text) }

        if (targetTranslations.isEmpty()) return QuizGenerationResult.FlashcardFallback

        fun displayTranslations(values: List<Content>): String =
            values.joinToString(" / ") { it.text.trim() }

        val correctDisplayText = displayTranslations(targetTranslations)
        val normalizedCorrect = normalizeQuizText(correctDisplayText)
        val correctCanonicalKeys = targetTranslations
            .map { normalizeQuizText(it.canonicalKey) }
            .filter { it.isNotBlank() }
            .toSet()

        val effectiveVocabularyDifficulty =
            difficultyState?.current ?: VocabularyDifficulty.MEDIUM

        val targetLanguageIds = contentsByConcept
            .filterValues { values ->
                values.any {
                    it.languageCode.equals(activeLanguagePair.targetLanguage, true) &&
                        it.text.isNotBlank()
                }
            }
            .keys

        val sourceLanguageIds = contentsByConcept
            .filterValues { values ->
                values.any {
                    it.languageCode.equals(activeLanguagePair.sourceLanguage, true) &&
                        it.text.isNotBlank()
                }
            }
            .keys

        val eligibleConcepts = snapshot.concepts.asSequence()
            .filter { it.active && it.id != concept.id }
            .filter { it.id in targetLanguageIds && it.id in sourceLanguageIds }
            .toList()

        fun difficultyDistance(other: Concept): Int {
            val otherDifficulty = snapshot.difficultiesById[other.id]?.current ?: return 3
            return when (effectiveVocabularyDifficulty) {
                VocabularyDifficulty.EASY -> when (otherDifficulty) {
                    VocabularyDifficulty.EASY -> 0
                    VocabularyDifficulty.MEDIUM -> 1
                    VocabularyDifficulty.HARD -> 2
                    VocabularyDifficulty.VERY_HARD -> 3
                }
                VocabularyDifficulty.MEDIUM -> when (otherDifficulty) {
                    VocabularyDifficulty.MEDIUM -> 0
                    VocabularyDifficulty.EASY, VocabularyDifficulty.HARD -> 1
                    VocabularyDifficulty.VERY_HARD -> 2
                }
                VocabularyDifficulty.HARD -> when (otherDifficulty) {
                    VocabularyDifficulty.HARD -> 0
                    VocabularyDifficulty.MEDIUM, VocabularyDifficulty.VERY_HARD -> 1
                    VocabularyDifficulty.EASY -> 2
                }
                VocabularyDifficulty.VERY_HARD -> when (otherDifficulty) {
                    VocabularyDifficulty.VERY_HARD -> 0
                    VocabularyDifficulty.HARD -> 1
                    VocabularyDifficulty.MEDIUM -> 2
                    VocabularyDifficulty.EASY -> 3
                }
            }
        }

        val excluded = excludedDistractorTexts
            .map(::normalizeQuizText)
            .filter { it.isNotBlank() }
            .toSet()

        val candidates = eligibleConcepts.mapNotNull { other ->
            val values = contentsByConcept[other.id].orEmpty()
                .filter {
                    it.languageCode.equals(activeLanguagePair.targetLanguage, true) &&
                        it.text.isNotBlank()
                }
                .sortedBy { it.id.toString() }
                .distinctBy { normalizeQuizText(it.text) }

            if (values.isEmpty()) {
                null
            } else {
                val displayText = displayTranslations(values)
                DistractorCandidate(
                    displayText = displayText,
                    canonicalKeys = values.map { normalizeQuizText(it.canonicalKey) }
                        .filter { it.isNotBlank() }
                        .toSet(),
                    vocabularyDifficultyDistance = difficultyDistance(other),
                    categoryMatch = concept.categoryId != null &&
                        concept.categoryId == other.categoryId,
                    entryTypeMatch = concept.entryType == other.entryType,
                    lexicalSimilarity = lexicalSimilarity(correctDisplayText, displayText)
                )
            }
        }
            .filter { normalizeQuizText(it.displayText) != normalizedCorrect }
            .filter {
                correctCanonicalKeys.isEmpty() ||
                    it.canonicalKeys.none { key -> key in correctCanonicalKeys }
            }
            .distinctBy { normalizeQuizText(it.displayText) }

        val freshCandidates = candidates.filter {
            normalizeQuizText(it.displayText) !in excluded
        }
        val selectionCandidates =
            if (freshCandidates.size >= 3) freshCandidates else candidates

        if (selectionCandidates.size < 3) {
            return QuizGenerationResult.FlashcardFallback
        }

        fun confusabilityScore(candidate: DistractorCandidate): Double {
            val category = if (candidate.categoryMatch) 1.0 else 0.0
            val entryType = if (candidate.entryTypeMatch) 1.0 else 0.0
            return (
                candidate.lexicalSimilarity * 0.55 +
                    category * 0.25 +
                    entryType * 0.20
                ).coerceIn(0.0, 1.0)
        }

        val sameDifficulty = selectionCandidates.filter { it.vocabularyDifficultyDistance == 0 }
        val adjacentDifficulty = selectionCandidates.filter { it.vocabularyDifficultyDistance == 1 }
        val selectedPool = when {
            sameDifficulty.size >= 3 -> sameDifficulty
            (sameDifficulty + adjacentDifficulty)
                .distinctBy { normalizeQuizText(it.displayText) }
                .size >= 3 ->
                (sameDifficulty + adjacentDifficulty)
                    .distinctBy { normalizeQuizText(it.displayText) }
            else -> selectionCandidates
        }

        val easyPreferred = selectedPool.filter { !it.categoryMatch && !it.entryTypeMatch }
        val easyFallback = selectedPool.filter { !it.categoryMatch }
        val mediumPreferred = selectedPool.filter { it.categoryMatch && !it.entryTypeMatch }
        val mediumFallback = selectedPool.filter { it.categoryMatch }
        val hardPreferred = selectedPool.filter { it.categoryMatch && it.entryTypeMatch }
        val hardFallback = selectedPool.filter { it.categoryMatch }

        fun ranked(
            values: List<DistractorCandidate>,
            descending: Boolean
        ): List<DistractorCandidate> =
            if (descending) {
                values.sortedWith(
                    compareByDescending<DistractorCandidate> { confusabilityScore(it) }
                        .thenBy { normalizeQuizText(it.displayText) }
                )
            } else {
                values.sortedWith(
                    compareBy<DistractorCandidate> { confusabilityScore(it) }
                        .thenBy { normalizeQuizText(it.displayText) }
                )
            }

        fun chooseBand(
            preferred: List<DistractorCandidate>,
            fallback: List<DistractorCandidate>,
            mode: QuizDifficulty
        ): List<DistractorCandidate> {
            val source = when {
                preferred.size >= 3 -> preferred
                fallback.size >= 3 -> fallback
                else -> selectedPool
            }
            val ordered = ranked(source, descending = mode == QuizDifficulty.HARD)
            if (ordered.size <= 3) return ordered

            return when (mode) {
                QuizDifficulty.EASY -> ordered.take(3)
                QuizDifficulty.MEDIUM -> {
                    val start = ((ordered.size - 3) / 2).coerceAtLeast(0)
                    ordered.drop(start).take(3)
                }
                QuizDifficulty.HARD -> ordered.take(3)
            }
        }

        val wrong = when (quizDifficulty) {
            QuizDifficulty.EASY -> chooseBand(easyPreferred, easyFallback, QuizDifficulty.EASY)
            QuizDifficulty.MEDIUM -> chooseBand(mediumPreferred, mediumFallback, QuizDifficulty.MEDIUM)
            QuizDifficulty.HARD -> chooseBand(hardPreferred, hardFallback, QuizDifficulty.HARD)
        }.map { it.displayText }

        if (wrong.size < 3) return QuizGenerationResult.FlashcardFallback

        val options = (listOf(correctDisplayText) + wrong).shuffled()

        if (
            options.size != 4 ||
            options.map(::normalizeQuizText).distinct().size != 4 ||
            options.count { normalizeQuizText(it) == normalizedCorrect } != 1
        ) {
            return QuizGenerationResult.FlashcardFallback
        }

        return QuizGenerationResult.QuizQuestion(
            promptText = prompt.text,
            correctAnswerText = correctDisplayText,
            options = options
        )
    }
}
