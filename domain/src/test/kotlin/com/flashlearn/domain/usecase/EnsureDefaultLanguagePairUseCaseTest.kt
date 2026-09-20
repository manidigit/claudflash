package com.flashlearn.domain.usecase

import com.flashlearn.domain.model.LanguagePair
import com.flashlearn.domain.model.deterministicLanguagePairId
import com.flashlearn.domain.repository.FakeLanguagePairRepository
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnsureDefaultLanguagePairUseCaseTest {

    @Test
    fun `inserts a real active es to fa pair when none exists`() = runTest {
        val repository = FakeLanguagePairRepository()

        val result = EnsureDefaultLanguagePairUseCase(repository)()

        assertEquals("es", result.sourceLanguage)
        assertEquals("fa", result.targetLanguage)
        assertTrue(result.isActive)
        assertEquals(result, repository.getActive())
    }

    @Test
    fun `does nothing and returns the existing pair when one is already active`() = runTest {
        val repository = FakeLanguagePairRepository()
        val existing = LanguagePair(
            id = UUID.randomUUID(), sourceLanguage = "en", targetLanguage = "fa", isActive = true
        )
        repository.insert(existing)

        val result = EnsureDefaultLanguagePairUseCase(repository)()

        assertEquals(existing, result)
        assertEquals(1, repository.getAll().size)
    }

    @Test
    fun `the seeded pair uses the deterministic id`() = runTest {
        val repository = FakeLanguagePairRepository()

        val result = EnsureDefaultLanguagePairUseCase(repository)()

        assertEquals(deterministicLanguagePairId("es", "fa"), result.id)
    }

    @Test
    fun `re-activates the deterministic pair if it exists but is inactive`() = runTest {
        val repository = FakeLanguagePairRepository()
        repository.insert(
            LanguagePair(deterministicLanguagePairId("es", "fa"), "es", "fa", isActive = false)
        )

        val result = EnsureDefaultLanguagePairUseCase(repository)()

        assertTrue(result.isActive)
        assertEquals(1, repository.getAll().size)
        assertEquals(result, repository.getActive())
    }

    @Test
    fun `calling it twice on an empty repository only ever creates one pair`() = runTest {
        val repository = FakeLanguagePairRepository()

        val first = EnsureDefaultLanguagePairUseCase(repository)()
        val second = EnsureDefaultLanguagePairUseCase(repository)()

        assertEquals(first, second)
        assertEquals(1, repository.getAll().size)
    }
}

class GetActiveLanguagePairUseCaseTest {

    @Test
    fun `returns the repository's active pair when one exists`() = runTest {
        val repository = FakeLanguagePairRepository()
        val active = LanguagePair(id = UUID.randomUUID(), sourceLanguage = "en", targetLanguage = "fa", isActive = true)
        repository.insert(active)

        val result = GetActiveLanguagePairUseCase(repository)()

        assertEquals(active, result)
    }

    @Test
    fun `falls back to the default V1 pair when nothing is active`() = runTest {
        val repository = FakeLanguagePairRepository()

        val result = GetActiveLanguagePairUseCase(repository)()

        assertEquals("es", result.sourceLanguage)
        assertEquals("fa", result.targetLanguage)
        assertTrue(result.isActive)
    }
}
