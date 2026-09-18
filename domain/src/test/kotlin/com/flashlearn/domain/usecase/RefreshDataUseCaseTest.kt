package com.flashlearn.domain.usecase

import com.flashlearn.domain.model.Concept
import com.flashlearn.domain.model.Content
import com.flashlearn.domain.model.EntryType
import com.flashlearn.domain.repository.FakeConceptRepository
import com.flashlearn.domain.repository.FakeContentRepository
import com.flashlearn.domain.repository.FakeFlashLearnDatabase
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RefreshDataUseCaseTest {

    private val now = Instant.parse("2026-09-14T12:00:00Z")

    private fun concept(id: UUID = UUID.randomUUID(), dataVersion: Int = 0, active: Boolean = true) = Concept(
        id = id, entryType = EntryType.WORD, categoryId = null, favorite = false,
        active = active, createdAt = now, updatedAt = now, dataVersion = dataVersion
    )

    private fun content(conceptId: UUID, id: UUID = UUID.randomUUID(), text: String = "hola", dataVersion: Int = 0) = Content(
        id = id, conceptId = conceptId, languageCode = "es", text = text, canonicalKey = text, dataVersion = dataVersion
    )

    @Test
    fun `NoChange when no migrations are registered`() = runTest {
        val concepts = FakeConceptRepository()
        val contents = FakeContentRepository()
        val c = concept()
        concepts.insert(c)
        contents.upsert(content(c.id))

        val useCase = RefreshDataUseCase(concepts, contents, FakeFlashLearnDatabase())

        assertEquals(RefreshDataResult.NoChange, useCase())
    }

    @Test
    fun `migrates a concept whose dataVersion lags currentConceptVersion`() = runTest {
        val concepts = FakeConceptRepository()
        val contents = FakeContentRepository()
        val c = concept(dataVersion = 0)
        concepts.insert(c)

        val useCase = RefreshDataUseCase(
            conceptRepository = concepts, contentRepository = contents, database = FakeFlashLearnDatabase(),
            currentConceptVersion = 1,
            conceptMigrations = mapOf(1 to { concept: Concept -> concept.copy(favorite = true) })
        )

        val result = useCase()

        assertEquals(RefreshDataResult.Success(1), result)
        val migrated = concepts.findAnyById(c.id)!!
        assertTrue(migrated.favorite)
        assertEquals(1, migrated.dataVersion)
    }

    @Test
    fun `migrates a content whose dataVersion lags currentContentVersion`() = runTest {
        val concepts = FakeConceptRepository()
        val contents = FakeContentRepository()
        val c = concept()
        concepts.insert(c)
        val existingContent = content(c.id, text = "hola", dataVersion = 0)
        contents.upsert(existingContent)

        val useCase = RefreshDataUseCase(
            conceptRepository = concepts, contentRepository = contents, database = FakeFlashLearnDatabase(),
            currentContentVersion = 1,
            contentMigrations = mapOf(1 to { content: Content -> content.copy(text = content.text.trim()) })
        )

        val result = useCase()

        assertEquals(RefreshDataResult.Success(1), result)
        val migrated = contents.getAllByConceptId(c.id).single()
        assertEquals(1, migrated.dataVersion)
    }

    @Test
    fun `applies multiple version steps in order`() = runTest {
        val concepts = FakeConceptRepository()
        val contents = FakeContentRepository()
        val c = concept(dataVersion = 0)
        concepts.insert(c)

        val log = mutableListOf<Int>()
        val useCase = RefreshDataUseCase(
            conceptRepository = concepts, contentRepository = contents, database = FakeFlashLearnDatabase(),
            currentConceptVersion = 3,
            conceptMigrations = mapOf(
                1 to { concept: Concept -> log.add(1); concept },
                2 to { concept: Concept -> log.add(2); concept },
                3 to { concept: Concept -> log.add(3); concept }
            )
        )

        useCase()

        assertEquals(listOf(1, 2, 3), log)
        assertEquals(3, concepts.findAnyById(c.id)!!.dataVersion)
    }

    @Test
    fun `re-running after a version is fully applied reports NoChange (idempotent)`() = runTest {
        val concepts = FakeConceptRepository()
        val contents = FakeContentRepository()
        val c = concept(dataVersion = 0)
        concepts.insert(c)

        val useCase = RefreshDataUseCase(
            conceptRepository = concepts, contentRepository = contents, database = FakeFlashLearnDatabase(),
            currentConceptVersion = 1,
            conceptMigrations = mapOf(1 to { concept: Concept -> concept.copy(favorite = true) })
        )

        useCase() // first run migrates
        val second = useCase()

        assertEquals(RefreshDataResult.NoChange, second)
    }

    @Test
    fun `Concept and Content versions advance independently`() = runTest {
        val concepts = FakeConceptRepository()
        val contents = FakeContentRepository()
        val c = concept(dataVersion = 4)
        concepts.insert(c)
        contents.upsert(content(c.id, dataVersion = 2))

        val useCase = RefreshDataUseCase(
            conceptRepository = concepts, contentRepository = contents, database = FakeFlashLearnDatabase(),
            currentConceptVersion = 4, // Concept already up to date -> no concept migration runs
            currentContentVersion = 5,
            contentMigrations = mapOf(
                3 to { content: Content -> content },
                4 to { content: Content -> content },
                5 to { content: Content -> content }
            )
        )

        val result = useCase()

        assertEquals(RefreshDataResult.Success(1), result) // the Concept counts as "updated" because its Content changed
        assertEquals(4, concepts.findAnyById(c.id)!!.dataVersion) // untouched
        assertEquals(5, contents.getAllByConceptId(c.id).single().dataVersion)
    }

    @Test
    fun `missing migration surfaces as Error rather than throwing`() = runTest {
        val concepts = FakeConceptRepository()
        val contents = FakeContentRepository()
        val c = concept(dataVersion = 0)
        concepts.insert(c)

        val useCase = RefreshDataUseCase(
            conceptRepository = concepts, contentRepository = contents, database = FakeFlashLearnDatabase(),
            currentConceptVersion = 2,
            conceptMigrations = mapOf(1 to { concept: Concept -> concept }) // version 2 migration missing
        )

        val result = useCase()

        assertTrue(result is RefreshDataResult.Error)
    }

    @Test
    fun `soft-deleted concepts are never migrated`() = runTest {
        val concepts = FakeConceptRepository()
        val contents = FakeContentRepository()
        val c = concept(dataVersion = 0, active = false)
        concepts.insert(c)

        val useCase = RefreshDataUseCase(
            conceptRepository = concepts, contentRepository = contents, database = FakeFlashLearnDatabase(),
            currentConceptVersion = 1,
            conceptMigrations = mapOf(1 to { concept: Concept -> concept.copy(favorite = true) })
        )

        val result = useCase()

        assertEquals(RefreshDataResult.NoChange, result)
        assertEquals(0, concepts.findAnyById(c.id)!!.dataVersion)
    }
}
