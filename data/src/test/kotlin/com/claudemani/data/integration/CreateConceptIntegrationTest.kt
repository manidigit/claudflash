package com.claudemani.data.integration

import com.claudemani.domain.model.Stage
import com.claudemani.domain.model.VocabularyDifficulty
import com.claudemani.domain.usecase.CreateConceptCommand
import com.claudemani.domain.usecase.CreateConceptUseCase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Real-Room integration test for [CreateConceptUseCase] (Descriptions
 * §4 Integrity Rule / §14). Every prior test of this UseCase used
 * [com.claudemani.domain.repository.FakeClaudemaniDatabase], whose own
 * KDoc admits it has "no real rollback semantics" — this class replaces
 * that with the actual Room-backed transaction via [RealRoomTestHarness].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CreateConceptIntegrationTest {

    private lateinit var harness: RealRoomTestHarness
    private lateinit var createConcept: CreateConceptUseCase

    @Before
    fun setUp() {
        harness = RealRoomTestHarness()
        createConcept = CreateConceptUseCase(
            conceptRepository = harness.conceptRepository,
            contentRepository = harness.contentRepository,
            learningStateRepository = harness.learningStateRepository,
            difficultyStateRepository = harness.difficultyStateRepository,
            conceptTagRepository = harness.conceptTagRepository,
            database = harness.database
        )
    }

    @After
    fun tearDown() {
        harness.close()
    }

    @Test
    fun `creates Concept plus both Contents plus initial LearningState and DifficultyState atomically`() = runTest {
        val conceptId = createConcept(
            CreateConceptCommand(sourceText = "hola", targetText = "سلام")
        )

        val concept = harness.roomDb.conceptDao().getById(conceptId)
        assertNotNull("Concept must be persisted", concept)
        assertEquals(true, concept?.active)

        val contents = harness.roomDb.contentDao().getAllByConceptId(conceptId)
        assertEquals(2, contents.size)
        assertEquals(setOf("hola", "سلام"), contents.map { it.text }.toSet())

        val learningState = harness.learningStateRepository.get(conceptId)
        assertNotNull(learningState)
        assertEquals(Stage.DAILY, learningState?.stage)

        val difficultyState = harness.difficultyStateRepository.get(conceptId)
        assertNotNull(difficultyState)
        assertEquals(VocabularyDifficulty.EASY, difficultyState?.current)
        assertEquals(0, difficultyState?.consecutiveCorrect)
        assertEquals(0, difficultyState?.consecutiveWrong)
        assertEquals(false, difficultyState?.hasReachedVeryHard)
    }

    @Test
    fun `canonicalKey is persisted and computed the same way for both Contents`() = runTest {
        val conceptId = createConcept(
            CreateConceptCommand(sourceText = "  El Científico  ", targetText = "دانشمند")
        )

        val source = harness.roomDb.contentDao().getAllByConceptId(conceptId)
            .single { it.languageCode == "es" }
        assertEquals("el científico", source.canonicalKey)
    }
}
