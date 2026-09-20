package com.claudemani.domain.usecase

import com.claudemani.domain.model.Language
import com.claudemani.domain.model.V1_LANGUAGES
import com.claudemani.domain.model.deterministicLanguageId
import com.claudemani.domain.model.deterministicLanguagePairId
import com.claudemani.domain.repository.FakeLanguageRepository
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnsureDefaultLanguagesUseCaseTest {

    @Test
    fun `inserts fa, en and es on an empty repository`() = runTest {
        val repository = FakeLanguageRepository()

        val result = EnsureDefaultLanguagesUseCase(repository)()

        assertEquals(setOf("fa", "en", "es"), repository.getAll().map { it.code }.toSet())
        assertEquals(3, result.size)
    }

    @Test
    fun `calling it repeatedly never creates duplicates`() = runTest {
        val repository = FakeLanguageRepository()

        EnsureDefaultLanguagesUseCase(repository)()
        EnsureDefaultLanguagesUseCase(repository)()
        EnsureDefaultLanguagesUseCase(repository)()

        assertEquals(3, repository.getAll().size)
    }

    @Test
    fun `an existing row with the same code is kept untouched, even under a different id`() = runTest {
        val repository = FakeLanguageRepository()
        val legacyEs = Language(UUID.randomUUID(), "es", "Spanish (legacy)")
        repository.insert(legacyEs)

        val result = EnsureDefaultLanguagesUseCase(repository)()

        assertEquals(3, repository.getAll().size)
        assertEquals(legacyEs, repository.getByCode("es"))
        assertTrue(result.contains(legacyEs))
    }

    @Test
    fun `seeded ids are the deterministic ones, identical on every install`() = runTest {
        val first = FakeLanguageRepository()
        val second = FakeLanguageRepository()

        EnsureDefaultLanguagesUseCase(first)()
        EnsureDefaultLanguagesUseCase(second)()

        assertEquals(first.getByCode("es")?.id, second.getByCode("es")?.id)
        assertEquals(deterministicLanguageId("es"), first.getByCode("es")?.id)
    }

    @Test
    fun `deterministic ids differ per language and per pair direction`() {
        assertNotEquals(deterministicLanguageId("es"), deterministicLanguageId("fa"))
        assertNotEquals(deterministicLanguagePairId("es", "fa"), deterministicLanguagePairId("fa", "es"))
        assertEquals(deterministicLanguagePairId("es", "fa"), deterministicLanguagePairId("es", "fa"))
    }

    @Test
    fun `every V1 language has a unique code and a non-blank name`() {
        assertEquals(V1_LANGUAGES.size, V1_LANGUAGES.map { it.code }.distinct().size)
        assertTrue(V1_LANGUAGES.all { it.name.isNotBlank() })
    }
}
