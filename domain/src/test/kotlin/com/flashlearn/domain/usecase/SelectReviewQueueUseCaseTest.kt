package com.flashlearn.domain.usecase

import com.flashlearn.domain.exception.DataIntegrityException
import com.flashlearn.domain.model.Concept
import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.model.EntryType
import com.flashlearn.domain.model.LearningState
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.model.Stage
import com.flashlearn.domain.model.VocabularyDifficulty
import com.flashlearn.domain.repository.FakeConceptRepository
import com.flashlearn.domain.repository.FakeConceptTagRepository
import com.flashlearn.domain.repository.FakeDifficultyStateRepository
import com.flashlearn.domain.repository.FakeLearningStateRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class SelectReviewQueueUseCaseTest {

    private val now = Instant.parse("2026-09-13T12:00:00Z")

    // `inner`: Fixture reads the outer class's `now` (`private val now =
    // Instant.parse(...)` above) — a plain nested class has no implicit
    // reference to the enclosing instance, which is exactly why this was
    // "Unresolved reference: now" the first time it was ever compiled.
    private inner class Fixture {
        val concepts = FakeConceptRepository()
        val learningStates = FakeLearningStateRepository()
        val difficultyStates = FakeDifficultyStateRepository()
        val conceptTags = FakeConceptTagRepository()

        val useCase = SelectReviewQueueUseCase(
            learningStateRepository = learningStates,
            difficultyStateRepository = difficultyStates,
            conceptRepository = concepts,
            conceptTagRepository = conceptTags
        )

        suspend fun addConcept(
            stage: Stage,
            nextReviewAt: Instant?,
            difficulty: VocabularyDifficulty = VocabularyDifficulty.EASY,
            categoryId: UUID? = null,
            active: Boolean = true,
            withDifficultyState: Boolean = true
        ): UUID {
            val id = UUID.randomUUID()
            concepts.insert(
                Concept(
                    id = id, entryType = EntryType.WORD, categoryId = categoryId,
                    favorite = false, active = active, createdAt = now, updatedAt = now
                )
            )
            learningStates.upsert(
                LearningState(
                    id = UUID.randomUUID(), conceptId = id, stage = stage, nextReviewAt = nextReviewAt,
                    monthlyWrongCount = 0, hasPathFailure = false, totalCorrect = 0, totalWrong = 0,
                    lastReviewedAt = null
                )
            )
            if (withDifficultyState) {
                difficultyStates.upsert(
                    DifficultyState(
                        id = UUID.randomUUID(), conceptId = id, current = difficulty,
                        consecutiveCorrect = 0, consecutiveWrong = 0, hasReachedVeryHard = false
                    )
                )
            }
            return id
        }
    }

    @Test
    fun `DAILY returns only due DAILY concepts, ordered by nextReviewAt ascending`() = runTest {
        val f = Fixture()
        val oldest = f.addConcept(Stage.DAILY, now.minusSeconds(3600))
        val newer = f.addConcept(Stage.DAILY, now.minusSeconds(60))
        f.addConcept(Stage.DAILY, now.plusSeconds(3600)) // not due yet
        f.addConcept(Stage.WEEKLY, now.minusSeconds(60)) // different stage

        val result = f.useCase(ReviewType.DAILY, now = now)

        assertEquals(listOf(oldest, newer), result.map { it.id })
    }

    @Test
    fun `LEARNED ignores nextReviewAt entirely - even null and future values are included`() = runTest {
        val f = Fixture()
        val a = f.addConcept(Stage.LEARNED, nextReviewAt = null)
        val b = f.addConcept(Stage.LEARNED, nextReviewAt = now.plusSeconds(999_999))
        f.addConcept(Stage.DAILY, now.minusSeconds(60)) // must not appear

        val result = f.useCase(ReviewType.LEARNED, now = now)

        assertEquals(setOf(a, b), result.map { it.id }.toSet())
        assertEquals(2, result.size)
    }

    @Test
    fun `RANDOM draws from due DAILY WEEKLY MONTHLY only, never LEARNED`() = runTest {
        val f = Fixture()
        val daily = f.addConcept(Stage.DAILY, now.minusSeconds(60))
        val weekly = f.addConcept(Stage.WEEKLY, now.minusSeconds(60))
        val monthly = f.addConcept(Stage.MONTHLY, now.minusSeconds(60))
        f.addConcept(Stage.LEARNED, nextReviewAt = null) // must be excluded
        f.addConcept(Stage.MONTHLY, now.plusSeconds(3600)) // not due, excluded

        val result = f.useCase(ReviewType.RANDOM, now = now)

        assertEquals(setOf(daily, weekly, monthly), result.map { it.id }.toSet())
    }

    @Test
    fun `difficulty filter narrows results`() = runTest {
        val f = Fixture()
        val hard = f.addConcept(Stage.DAILY, now.minusSeconds(60), difficulty = VocabularyDifficulty.HARD)
        f.addConcept(Stage.DAILY, now.minusSeconds(60), difficulty = VocabularyDifficulty.EASY)

        val result = f.useCase(ReviewType.DAILY, ReviewQueueFilters(difficulty = VocabularyDifficulty.HARD), now)

        assertEquals(listOf(hard), result.map { it.id })
    }

    @Test
    fun `category filter narrows results`() = runTest {
        val f = Fixture()
        val categoryId = UUID.randomUUID()
        val matching = f.addConcept(Stage.DAILY, now.minusSeconds(60), categoryId = categoryId)
        f.addConcept(Stage.DAILY, now.minusSeconds(60), categoryId = UUID.randomUUID())

        val result = f.useCase(ReviewType.DAILY, ReviewQueueFilters(category = categoryId), now)

        assertEquals(listOf(matching), result.map { it.id })
    }

    @Test
    fun `tag filter narrows results`() = runTest {
        val f = Fixture()
        val tagId = UUID.randomUUID()
        val tagged = f.addConcept(Stage.DAILY, now.minusSeconds(60))
        f.conceptTags.insert(com.flashlearn.domain.model.ConceptTag(tagged, tagId))
        f.addConcept(Stage.DAILY, now.minusSeconds(60)) // untagged

        val result = f.useCase(ReviewType.DAILY, ReviewQueueFilters(tag = tagId), now)

        assertEquals(listOf(tagged), result.map { it.id })
    }

    @Test
    fun `filters combine with AND`() = runTest {
        val f = Fixture()
        val categoryId = UUID.randomUUID()
        val tagId = UUID.randomUUID()

        val matches = f.addConcept(Stage.DAILY, now.minusSeconds(60), VocabularyDifficulty.HARD, categoryId)
        f.conceptTags.insert(com.flashlearn.domain.model.ConceptTag(matches, tagId))

        // Same category+tag but wrong difficulty -> must be excluded.
        val wrongDifficulty = f.addConcept(Stage.DAILY, now.minusSeconds(60), VocabularyDifficulty.EASY, categoryId)
        f.conceptTags.insert(com.flashlearn.domain.model.ConceptTag(wrongDifficulty, tagId))

        val result = f.useCase(
            ReviewType.DAILY,
            ReviewQueueFilters(difficulty = VocabularyDifficulty.HARD, category = categoryId, tag = tagId),
            now
        )

        assertEquals(listOf(matches), result.map { it.id })
    }

    @Test
    fun `soft-deleted concept is silently excluded, not an error`() = runTest {
        val f = Fixture()
        val id = f.addConcept(Stage.DAILY, now.minusSeconds(60))
        f.concepts.softDelete(id, now)

        val result = f.useCase(ReviewType.DAILY, now = now)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `missing DifficultyState throws DataIntegrityException`() {
        val f = Fixture()
        assertThrows(DataIntegrityException::class.java) {
            runBlocking {
                f.addConcept(Stage.DAILY, now.minusSeconds(60), withDifficultyState = false)
                f.useCase(ReviewType.DAILY, now = now)
            }
        }
    }

    @Test
    fun `empty result when nothing is due returns empty list, not an error`() = runTest {
        val f = Fixture()
        f.addConcept(Stage.DAILY, now.plusSeconds(3600))

        val result = f.useCase(ReviewType.MONTHLY, now = now)

        assertTrue(result.isEmpty())
    }
}
