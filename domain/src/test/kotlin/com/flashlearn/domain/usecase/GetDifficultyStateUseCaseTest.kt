package com.flashlearn.domain.usecase

import com.flashlearn.domain.exception.DataIntegrityException
import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.model.VocabularyDifficulty
import com.flashlearn.domain.repository.FakeDifficultyStateRepository
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
