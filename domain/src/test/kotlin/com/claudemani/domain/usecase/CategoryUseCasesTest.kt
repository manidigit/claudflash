package com.claudemani.domain.usecase

import com.claudemani.domain.exception.DataIntegrityException
import com.claudemani.domain.exception.InvalidCategoryInputException
import com.claudemani.domain.repository.FakeCategoryRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class CategoryUseCasesTest {

    @Test
    fun `CreateCategory trims name and returns a usable id`() = runTest {
        val repo = FakeCategoryRepository()
        val create = CreateCategoryUseCase(repo)

        val id = create("  Animals  ")

        assertEquals("Animals", repo.getById(id)?.name)
    }

    @Test
    fun `CreateCategory rejects blank name`() {
        val repo = FakeCategoryRepository()
        val create = CreateCategoryUseCase(repo)
        assertThrows(InvalidCategoryInputException::class.java) {
            runBlocking { create("   ") }
        }
    }

    @Test
    fun `UpdateCategory renames an existing category`() = runTest {
        val repo = FakeCategoryRepository()
        val create = CreateCategoryUseCase(repo)
        val update = UpdateCategoryUseCase(repo)

        val id = create("Travel")
        update(id, "Travel & Places")

        assertEquals("Travel & Places", repo.getById(id)?.name)
    }

    @Test
    fun `UpdateCategory on unknown id throws DataIntegrityException`() {
        val repo = FakeCategoryRepository()
        val update = UpdateCategoryUseCase(repo)
        assertThrows(DataIntegrityException::class.java) {
            runBlocking { update(UUID.randomUUID(), "x") }
        }
    }

    @Test
    fun `UpdateCategory rejects blank name`() = runTest {
        val repo = FakeCategoryRepository()
        val create = CreateCategoryUseCase(repo)
        val update = UpdateCategoryUseCase(repo)
        val id = create("Food")

        assertThrows(InvalidCategoryInputException::class.java) {
            runBlocking { update(id, "  ") }
        }
    }

    @Test
    fun `GetAllCategories returns everything sorted by name`() = runTest {
        val repo = FakeCategoryRepository()
        val create = CreateCategoryUseCase(repo)
        val getAll = GetAllCategoriesUseCase(repo)

        create("Zoo")
        create("Animals")
        create("Mountains")

        val names = getAll().map { it.name }
        assertEquals(listOf("Animals", "Mountains", "Zoo"), names)
    }

    @Test
    fun `GetCategoryById returns null for unknown id`() = runTest {
        val repo = FakeCategoryRepository()
        val getById = GetCategoryByIdUseCase(repo)
        assertEquals(null, getById(UUID.randomUUID()))
    }
}
