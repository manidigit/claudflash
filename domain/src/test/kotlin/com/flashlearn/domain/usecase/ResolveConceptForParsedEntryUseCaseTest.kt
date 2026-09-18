package com.flashlearn.domain.usecase

import com.flashlearn.domain.model.Concept
import com.flashlearn.domain.model.Content
import com.flashlearn.domain.model.EntryType
import com.flashlearn.domain.model.computeCanonicalKey
import com.flashlearn.domain.repository.FakeContentRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ResolveConceptForParsedEntryUseCaseTest {

    private fun content(conceptId: UUID, languageCode: String, text: String) = Content(
        id = UUID.randomUUID(), conceptId = conceptId, languageCode = languageCode,
        text = text, canonicalKey = computeCanonicalKey(text)
    )

    @Test
    fun `neither piece matches - CreateNewConcept with both pieces`() = runTest {
        val contents = FakeContentRepository()
        val useCase = ResolveConceptForParsedEntryUseCase(contents)

        val pieces = listOf(ResolvePiece("es", "el gato"), ResolvePiece("fa", "گربه"))
        val result = useCase(pieces)

        assertTrue(result is ResolveConceptResult.CreateNewConcept)
        assertEquals(pieces, (result as ResolveConceptResult.CreateNewConcept).allContentsToInsert)
    }

    @Test
    fun `both pieces match the same concept with nothing new - ReuseConcept with empty list`() = runTest {
        val contents = FakeContentRepository()
        val conceptId = UUID.randomUUID()
        contents.upsert(content(conceptId, "es", "el gato"))
        contents.upsert(content(conceptId, "fa", "گربه"))
        val useCase = ResolveConceptForParsedEntryUseCase(contents)

        val result = useCase(listOf(ResolvePiece("es", "el gato"), ResolvePiece("fa", "گربه")))

        assertTrue(result is ResolveConceptResult.ReuseConcept)
        result as ResolveConceptResult.ReuseConcept
        assertEquals(conceptId, result.conceptId)
        assertTrue(result.newContentsToInsert.isEmpty())
    }

    @Test
    fun `one piece matches an existing concept - ReuseConcept with only the missing piece`() = runTest {
        val contents = FakeContentRepository()
        val conceptId = UUID.randomUUID()
        contents.upsert(content(conceptId, "es", "el gato"))
        val useCase = ResolveConceptForParsedEntryUseCase(contents)

        // "el gato" already exists for this concept; a second Persian translation is new.
        val result = useCase(listOf(ResolvePiece("es", "el gato"), ResolvePiece("fa", "گربه کوچولو")))

        assertTrue(result is ResolveConceptResult.ReuseConcept)
        result as ResolveConceptResult.ReuseConcept
        assertEquals(conceptId, result.conceptId)
        assertEquals(listOf(ResolvePiece("fa", "گربه کوچولو")), result.newContentsToInsert)
    }

    @Test
    fun `pieces match two different concepts - Conflict, nothing inserted`() = runTest {
        val contents = FakeContentRepository()
        val conceptA = UUID.randomUUID()
        val conceptB = UUID.randomUUID()
        contents.upsert(content(conceptA, "es", "cura"))
        contents.upsert(content(conceptB, "fa", "کشیش"))
        val useCase = ResolveConceptForParsedEntryUseCase(contents)

        val result = useCase(listOf(ResolvePiece("es", "cura"), ResolvePiece("fa", "کشیش")))

        assertTrue(result is ResolveConceptResult.Conflict)
        assertEquals(setOf(conceptA, conceptB), (result as ResolveConceptResult.Conflict).matchedConceptIds)
    }

    @Test
    fun `matching is scoped by languageCode - same text different language does not match`() = runTest {
        val contents = FakeContentRepository()
        val conceptId = UUID.randomUUID()
        contents.upsert(content(conceptId, "fa", "hola")) // same text, wrong language
        val useCase = ResolveConceptForParsedEntryUseCase(contents)

        val result = useCase(listOf(ResolvePiece("es", "hola"), ResolvePiece("fa", "سلام")))

        assertTrue(result is ResolveConceptResult.CreateNewConcept)
    }
}
