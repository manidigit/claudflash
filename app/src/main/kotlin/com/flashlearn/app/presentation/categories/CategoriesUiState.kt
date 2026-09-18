package com.flashlearn.app.presentation.categories

import com.flashlearn.domain.model.Category
import java.util.UUID

/**
 * Category management screen state. There is deliberately no delete
 * affordance anywhere in this state — [com.flashlearn.domain.usecase.CreateCategoryUseCase]'s
 * KDoc explains Category deletion is an open product decision, not yet
 * implemented in the domain layer.
 */
data class CategoriesUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val newCategoryName: String = "",
    val editingCategoryId: UUID? = null,
    val editingCategoryName: String = "",
    val errorMessage: String? = null
)
