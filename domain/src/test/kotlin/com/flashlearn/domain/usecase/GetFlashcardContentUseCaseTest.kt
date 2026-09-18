package com.flashlearn.domain.usecase

import com.flashlearn.domain.exception.DataIntegrityException
import com.flashlearn.domain.model.Content
import com.flashlearn.domain.model.computeCanonicalKey
import com.flashlearn.domain.repository.FakeContentRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class GetFlashcardContentUseCaseTest {

    private val contents = FakeContentRepository()
    private val useCase = GetFlashcardContentUseCase(contents)

    private suspend fun addContent(conceptId: UUID, languageCode: String, text: String, notes: String? = null) {
        contents.upsert(
            Content(
                id = UUID.randomUUID(),
                conceptId = conceptId,
                languageCode = languageCode,
                text = text,
                canonicalKey = computeCanonicalKey(text),
                notes = notes
            )
        )
    }

    @Test
    fun `returns source text as front and target text as back`() = runTest {
        val conceptId = UUID.randomUUID()
        addContent(conceptId, "es", "gato")
        addContent(conceptId, "fa", "گربه")

        val result = useCase(conceptId)

        assertEquals("gato", result.frontText)
        assertEquals("گربه", result.backText)
    }

    @Test
    fun `notes come from the source-language content only`() = runTest {
        val conceptId = UUID.randomUUID()
        addContent(conceptId, "es", "gato", notes = "یادداشت")
        addContent(conceptId, "fa", "گربه")

        val result = useCase(conceptId)

        assertEquals("یادداشت", result.notes)
    }

    @Test
    fun `notes are null when the source content has none`() = runTest {
        val conceptId = UUID.randomUUID()
        addContent(conceptId, "es", "gato")
        addContent(conceptId, "fa", "گربه")

        assertNull(useCase(conceptId).notes)
    }

    @Test
    fun `throws DataIntegrityException when the source language content is missing`() {
        val conceptId = UUID.randomUUID()
        assertThrows(DataIntegrityException::class.java) {
            runBlocking {
                addContent(conceptId, "fa", "گربه")
                useCase(conceptId)
            }
        }
    }

    @Test
    fun `throws DataIntegrityException when the target language content is missing`() {
        val conceptId = UUID.randomUUID()
        assertThrows(DataIntegrityException::class.java) {
            runBlocking {
                addContent(conceptId, "es", "gato")
                useCase(conceptId)
            }
        }
    }

    @Test
    fun `respects custom source and target languages`() = runTest {
        val conceptId = UUID.randomUUID()
        addContent(conceptId, "en", "cat")
        addContent(conceptId, "fa", "گربه")

        val result = useCase(conceptId, sourceLanguage = "en", targetLanguage = "fa")

        assertEquals("cat", result.frontText)
        assertEquals("گربه", result.backText)
    }
}
