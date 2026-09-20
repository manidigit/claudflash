package com.claudemani.domain.usecase

import com.claudemani.domain.exception.DataIntegrityException
import com.claudemani.domain.model.DifficultyState
import com.claudemani.domain.model.VocabularyDifficulty
import com.claudemani.domain.repository.FakeDifficultyStateRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class GetDifficultyStateUseCaseTest {

    private val difficultyStates = FakeDifficultyStateRepository()
    private val useCase = GetDifficultyStateUseCase(difficultyStates)

    @Test
    fun `returns the existing DifficultyState for the concept`() = runTest {
        val conceptId = UUID.randomUUID()
        val state = DifficultyState(UUID.randomUUID(), conceptId, VocabularyDifficulty.MEDIUM, 1, 0, false)
        difficultyStates.upsert(state)

        assertEquals(state, useCase(conceptId))
    }

    @Test
    fun `throws DataIntegrityException when no DifficultyState exists for the concept`() {
        val conceptId = UUID.randomUUID()
        assertThrows(DataIntegrityException::class.java) {
            runBlocking { useCase(conceptId) }
        }
    }
}
