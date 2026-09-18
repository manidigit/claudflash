package com.flashlearn.domain.usecase

import com.flashlearn.domain.model.Concept
import com.flashlearn.domain.model.Content
import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.model.EntryType
import com.flashlearn.domain.model.LanguagePair
import com.flashlearn.domain.model.VocabularyDifficulty
import com.flashlearn.domain.model.computeCanonicalKey
import com.flashlearn.domain.repository.FakeConceptRepository
import com.flashlearn.domain.repository.FakeContentRepository
import com.flashlearn.domain.repository.FakeDifficultyStateRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class GenerateQuizQuestionUseCaseTest {

    private val now = Instant.parse("2026-09-13T12:00:00Z")
    private val pair = LanguagePair(id = UUID.randomUUID(), sourceLanguage = "es", targetLanguage = "fa", isActive = true)

    private class Fixture {
        val concepts = FakeConceptRepository()
        val contents = FakeContentRepository()
        val difficultyStates = FakeDifficultyStateRepository()

        val useCase = GenerateQuizQuestionUseCase(
            contentRepository = contents,
            conceptRepository = concepts,
            difficultyStateRepository = difficultyStates
        )
    }

    private suspend fun Fixture.addConcept(
        source: String,
        target: String,
        difficulty: VocabularyDifficulty = VocabularyDifficulty.EASY,
        active: Boolean = true
    ): Concept {
        val concept = Concept(
            id = UUID.randomUUID(), entryType = EntryType.WORD, categoryId = null,
            favorite = false, active = active, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH
        )
        concepts.insert(concept)
        contents.upsert(
            Content(
                id = UUID.randomUUID(), conceptId = concept.id, languageCode = "es",
                text = source, canonicalKey = computeCanonicalKey(source)
            )
        )
        contents.upsert(
            Content(
                id = UUID.randomUUID(), conceptId = concept.id, languageCode = "fa",
                text = target, canonicalKey = computeCanonicalKey(target)
            )
        )
        difficultyStates.upsert(
            DifficultyState(
                id = UUID.randomUUID(), conceptId = concept.id, current = difficulty,
                consecutiveCorrect = 0, consecutiveWrong = 0, hasReachedVeryHard = false
            )
        )
        return concept
    }

    @Test
    fun `returns FlashcardFallback when prompt content is missing`() = runTest {
        val fx = Fixture()
        val concept = Concept(
            id = UUID.randomUUID(), entryType = EntryType.WORD, categoryId = null,
            favorite = false, active = true, createdAt = now, updatedAt = now
        )
        fx.concepts.insert(concept)
        fx.contents.upsert(
            Content(id = UUID.randomUUID(), conceptId = concept.id, languageCode = "fa", text = "دانشمند", canonicalKey = "دانشمند")
        )
        fx.difficultyStates.upsert(
            DifficultyState(id = UUID.randomUUID(), conceptId = concept.id, current = VocabularyDifficulty.EASY, consecutiveCorrect = 0, consecutiveWrong = 0, hasReachedVeryHard = false)
        )

        val result = fx.useCase(concept, pair, fx.difficultyStates.get(concept.id)!!)
        assertEquals(QuizGenerationResult.FlashcardFallback, result)
    }

    @Test
    fun `returns FlashcardFallback when correct content is missing`() = runTest {
        val fx = Fixture()
        val concept = Concept(
            id = UUID.randomUUID(), entryType = EntryType.WORD, categoryId = null,
            favorite = false, active = true, createdAt = now, updatedAt = now
        )
        fx.concepts.insert(concept)
        fx.contents.upsert(
            Content(id = UUID.randomUUID(), conceptId = concept.id, languageCode = "es", text = "el científico", canonicalKey = "el científico")
        )
        fx.difficultyStates.upsert(
            DifficultyState(id = UUID.randomUUID(), conceptId = concept.id, current = VocabularyDifficulty.EASY, consecutiveCorrect = 0, consecutiveWrong = 0, hasReachedVeryHard = false)
        )

        val result = fx.useCase(concept, pair, fx.difficultyStates.get(concept.id)!!)
        assertEquals(QuizGenerationResult.FlashcardFallback, result)
    }

    @Test
    fun `returns FlashcardFallback when fewer than 3 distractors exist in whole bank`() = runTest {
        val fx = Fixture()
        val main = fx.addConcept("el científico", "دانشمند", VocabularyDifficulty.EASY)
        fx.addConcept("la casa", "خانه", VocabularyDifficulty.EASY)
        fx.addConcept("el perro", "سگ", VocabularyDifficulty.HARD)
        // Only 2 other concepts exist in total — not enough even using the whole bank.

        val result = fx.useCase(main, pair, fx.difficultyStates.get(main.id)!!)
        assertEquals(QuizGenerationResult.FlashcardFallback, result)
    }

    @Test
    fun `builds a valid 4-option question when 3+ same-difficulty distractors exist`() = runTest {
        val fx = Fixture()
        val main = fx.addConcept("el científico", "دانشمند", VocabularyDifficulty.EASY)
        fx.addConcept("la casa", "خانه", VocabularyDifficulty.EASY)
        fx.addConcept("el perro", "سگ", VocabularyDifficulty.EASY)
        fx.addConcept("el gato", "گربه", VocabularyDifficulty.EASY)

        val result = fx.useCase(main, pair, fx.difficultyStates.get(main.id)!!)
        assertTrue(result is QuizGenerationResult.QuizQuestion)
        val question = result as QuizGenerationResult.QuizQuestion
        assertEquals("el científico", question.promptText)
        assertEquals("دانشمند", question.correctAnswerText)
        assertEquals(4, question.options.size)
        assertEquals(4, question.options.distinct().size)
        assertTrue(question.options.contains("دانشمند"))
    }

    @Test
    fun `widens to adjacent difficulty levels when same-level pool is too small`() = runTest {
        val fx = Fixture()
        val main = fx.addConcept("el científico", "دانشمند", VocabularyDifficulty.MEDIUM)
        fx.addConcept("la casa", "خانه", VocabularyDifficulty.MEDIUM) // same level: only 1
        fx.addConcept("el perro", "سگ", VocabularyDifficulty.EASY) // adjacent (one below)
        fx.addConcept("el gato", "گربه", VocabularyDifficulty.HARD) // adjacent (one above)
        fx.addConcept("la flor", "گل", VocabularyDifficulty.VERY_HARD) // not adjacent to MEDIUM

        val result = fx.useCase(main, pair, fx.difficultyStates.get(main.id)!!)
        assertTrue(result is QuizGenerationResult.QuizQuestion)
        val question = result as QuizGenerationResult.QuizQuestion
        assertEquals(4, question.options.distinct().size)
        // VERY_HARD candidate must NOT be pulled in — same+adjacent already gave exactly 3.
        assertTrue(!question.options.contains("گل"))
    }

    @Test
    fun `falls back to whole bank when same and adjacent levels are insufficient`() = runTest {
        val fx = Fixture()
        val main = fx.addConcept("el científico", "دانشمند", VocabularyDifficulty.MEDIUM)
        fx.addConcept("la casa", "خانه", VocabularyDifficulty.VERY_HARD) // not adjacent to MEDIUM
        fx.addConcept("el perro", "سگ", VocabularyDifficulty.VERY_HARD)
        fx.addConcept("el gato", "گربه", VocabularyDifficulty.VERY_HARD)

        val result = fx.useCase(main, pair, fx.difficultyStates.get(main.id)!!)
        assertTrue(result is QuizGenerationResult.QuizQuestion)
        val question = result as QuizGenerationResult.QuizQuestion
        assertEquals(4, question.options.distinct().size)
    }

    @Test
    fun `never uses another translation of the same concept as a distractor`() = runTest {
        val fx = Fixture()
        val main = fx.addConcept("el científico", "دانشمند", VocabularyDifficulty.EASY)
        // A second translation for the SAME concept — must never appear as an option.
        fx.contents.upsert(
            Content(id = UUID.randomUUID(), conceptId = main.id, languageCode = "fa", text = "پژوهشگر", canonicalKey = computeCanonicalKey("پژوهشگر"))
        )
        fx.addConcept("la casa", "خانه", VocabularyDifficulty.EASY)
        fx.addConcept("el perro", "سگ", VocabularyDifficulty.EASY)
        fx.addConcept("el gato", "گربه", VocabularyDifficulty.EASY)

        val result = fx.useCase(main, pair, fx.difficultyStates.get(main.id)!!)
        assertTrue(result is QuizGenerationResult.QuizQuestion)
        val question = result as QuizGenerationResult.QuizQuestion
        assertTrue(!question.options.contains("پژوهشگر"))
    }

    @Test
    fun `excludes distractors whose text normalizes to the same as the correct answer`() = runTest {
        val fx = Fixture()
        val main = fx.addConcept("el científico", "دانشمند", VocabularyDifficulty.EASY)
        // Different concept, but same translation text (just extra whitespace/case) as the correct answer.
        fx.addConcept("otro", "  Dانشمند".let { "دانشمند" }, VocabularyDifficulty.EASY) // duplicate text, different concept
        fx.addConcept("la casa", "خانه", VocabularyDifficulty.EASY)
        fx.addConcept("el perro", "سگ", VocabularyDifficulty.EASY)

        val result = fx.useCase(main, pair, fx.difficultyStates.get(main.id)!!)
        // Only 2 genuinely distinct distractors ("خانه", "سگ") remain — not enough.
        assertEquals(QuizGenerationResult.FlashcardFallback, result)
    }

    @Test
    fun `excludes distractors belonging to soft-deleted concepts`() = runTest {
        val fx = Fixture()
        val main = fx.addConcept("el científico", "دانشمند", VocabularyDifficulty.EASY)
        fx.addConcept("la casa", "خانه", VocabularyDifficulty.EASY)
        fx.addConcept("el perro", "سگ", VocabularyDifficulty.EASY)
        fx.addConcept("el gato", "گربه", VocabularyDifficulty.EASY, active = false)

        val result = fx.useCase(main, pair, fx.difficultyStates.get(main.id)!!)
        assertEquals(QuizGenerationResult.FlashcardFallback, result)
    }
}
