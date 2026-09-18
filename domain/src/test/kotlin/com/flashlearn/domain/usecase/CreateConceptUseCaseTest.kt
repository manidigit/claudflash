package com.flashlearn.domain.usecase

import com.flashlearn.domain.exception.InvalidConceptInputException
import com.flashlearn.domain.model.EntryType
import com.flashlearn.domain.model.Stage
import com.flashlearn.domain.model.VocabularyDifficulty
import com.flashlearn.domain.repository.FakeConceptRepository
import com.flashlearn.domain.repository.FakeConceptTagRepository
import com.flashlearn.domain.repository.FakeContentRepository
import com.flashlearn.domain.repository.FakeDifficultyStateRepository
import com.flashlearn.domain.repository.FakeFlashLearnDatabase
import com.flashlearn.domain.repository.FakeLearningStateRepository
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
        database = FakeFlashLearnDatabase()
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
        assertEquals(null, learningState?.nextReviewAt)

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
