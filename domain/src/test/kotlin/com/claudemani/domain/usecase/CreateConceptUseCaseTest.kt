package com.claudemani.domain.usecase

import com.claudemani.domain.exception.InvalidConceptInputException
import com.claudemani.domain.model.EntryType
import com.claudemani.domain.model.Stage
import com.claudemani.domain.model.VocabularyDifficulty
import com.claudemani.domain.repository.FakeConceptRepository
import com.claudemani.domain.repository.FakeConceptTagRepository
import com.claudemani.domain.repository.FakeContentRepository
import com.claudemani.domain.repository.FakeDifficultyStateRepository
import com.claudemani.domain.repository.FakeClaudemaniDatabase
import com.claudemani.domain.repository.FakeLearningStateRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class CreateConceptUseCaseTest {

    private fun buildUseCase(
        concepts: FakeConceptRepository = FakeConceptRepository(),
        contents: FakeContentRepository = FakeContentRepository(),
        learningStates: FakeLearningStateRepository = FakeLearningStateRepository(),
        difficultyStates: FakeDifficultyStateRepository = FakeDifficultyStateRepository(),
        conceptTags: FakeConceptTagRepository = FakeConceptTagRepository()
    ) = CreateConceptUseCase(
        conceptRepository = concepts,
        contentRepository = contents,
        learningStateRepository = learningStates,
        difficultyStateRepository = difficultyStates,
        conceptTagRepository = conceptTags,
        database = FakeClaudemaniDatabase()
    )

    @Test
    fun `creates concept with two contents, DAILY learning state and EASY difficulty`() = runTest {
        val concepts = FakeConceptRepository()
        val contents = FakeContentRepository()
        val learningStates = FakeLearningStateRepository()
        val difficultyStates = FakeDifficultyStateRepository()
        val useCase = buildUseCase(concepts, contents, learningStates, difficultyStates)

        val conceptId = useCase(
            CreateConceptCommand(sourceText = "el científico", targetText = "دانشمند")
        )

        val concept = concepts.getById(conceptId)
        assertTrue(concept != null && concept.active)
        assertEquals(EntryType.WORD, concept?.entryType)

        val conceptContents = contents.getAllByConceptId(conceptId)
        assertEquals(2, conceptContents.size)
        assertTrue(conceptContents.any { it.languageCode == "es" && it.text == "el científico" })
        assertTrue(conceptContents.any { it.languageCode == "fa" && it.text == "دانشمند" })

        val learningState = learningStates.get(conceptId)
        assertEquals(Stage.DAILY, learningState?.stage)
        // A new word must be due immediately, otherwise no review queue ever contains it.
        val dueAt = learningState?.nextReviewAt
        assertTrue(dueAt != null && !dueAt.isAfter(java.time.Instant.now()))

        val difficultyState = difficultyStates.get(conceptId)
        assertEquals(VocabularyDifficulty.EASY, difficultyState?.current)
        assertFalse(difficultyState?.hasReachedVeryHard ?: true)
    }

    @Test
    fun `canonicalKey is computed for both contents`() = runTest {
        val contents = FakeContentRepository()
        val useCase = buildUseCase(contents = contents)

        val conceptId = useCase(
            CreateConceptCommand(sourceText = "  El Científico  ", targetText = "دانشمند")
        )

        val sourceContent = contents.getByConceptIdAndLanguage(conceptId, "es")
        assertEquals("el científico", sourceContent?.canonicalKey)
    }

    @Test
    fun `optional tags are attached`() = runTest {
        val conceptTags = FakeConceptTagRepository()
        val useCase = buildUseCase(conceptTags = conceptTags)
        val tagId = UUID.randomUUID()

        val conceptId = useCase(
            CreateConceptCommand(sourceText = "correr", targetText = "دویدن", tags = listOf(tagId))
        )

        assertEquals(listOf(tagId), conceptTags.getTagIdsForConcept(conceptId))
    }

    @Test
    fun `blank sourceText throws InvalidConceptInputException`() {
        val useCase = buildUseCase()
        assertThrows(InvalidConceptInputException::class.java) {
            runBlocking { useCase(CreateConceptCommand(sourceText = "   ", targetText = "x")) }
        }
    }

    @Test
    fun `blank targetText throws InvalidConceptInputException`() {
        val useCase = buildUseCase()
        assertThrows(InvalidConceptInputException::class.java) {
            runBlocking { useCase(CreateConceptCommand(sourceText = "x", targetText = "")) }
        }
    }

    @Test
    fun `custom language pair overrides the es-fa default`() = runTest {
        val contents = FakeContentRepository()
        val useCase = buildUseCase(contents = contents)

        val conceptId = useCase(
            CreateConceptCommand(
                sourceText = "hello",
                targetText = "سلام",
                sourceLanguage = "en",
                targetLanguage = "fa"
            )
        )

        assertTrue(contents.getAllByConceptId(conceptId).any { it.languageCode == "en" })
    }
}
