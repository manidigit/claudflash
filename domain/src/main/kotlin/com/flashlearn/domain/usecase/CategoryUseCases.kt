package com.flashlearn.domain.usecase

import com.flashlearn.domain.exception.DataIntegrityException
import com.flashlearn.domain.exception.InvalidCategoryInputException
import com.flashlearn.domain.model.Category
import com.flashlearn.domain.repository.CategoryRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Category deletion is intentionally NOT implemented yet. Whether it
 * should be soft (matching the general Descriptions §2 soft-delete
 * principle) or hard, and what happens to Concepts whose categoryId
 * points at a deleted Category (null it out vs. block deletion), are not
 * specified anywhere in Descriptions/Algorithms v4.20 — this is an open
 * product decision to be settled explicitly before implementing, not
 * something to guess here.
 */
class CreateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(name: String): UUID {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            throw InvalidCategoryInputException("Category name must not be blank")
        }
        val id = UUID.randomUUID()
        categoryRepository.insert(Category(id = id, name = trimmed))
        return id
    }
}

class UpdateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(id: UUID, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            throw InvalidCategoryInputException("Category name must not be blank")
        }
        val existing = categoryRepository.getById(id)
            ?: throw DataIntegrityException("Category not found: $id")
        categoryRepository.update(existing.copy(name = trimmed))
    }
}

class GetAllCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(): List<Category> =
        categoryRepository.getAll().sortedBy { it.name }
}

class GetCategoryByIdUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(id: UUID): Category? = categoryRepository.getById(id)
}
