package com.flashlearn.app.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashlearn.domain.exception.InvalidCategoryInputException
import com.flashlearn.domain.usecase.CreateCategoryUseCase
import com.flashlearn.domain.usecase.GetAllCategoriesUseCase
import com.flashlearn.domain.usecase.UpdateCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns Category management state — list, create, rename. No delete
 * (see [CategoriesUiState]'s KDoc). Only through UseCases, same rule as
 * every other screen.
 */
@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val getAllCategories: GetAllCategoriesUseCase,
    private val createCategory: CreateCategoryUseCase,
    private val updateCategory: UpdateCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val categories = getAllCategories()
            _uiState.value = _uiState.value.copy(isLoading = false, categories = categories)
        }
    }

    fun onNewCategoryNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(newCategoryName = name, errorMessage = null)
    }

    fun addCategory() {
        val name = _uiState.value.newCategoryName
        viewModelScope.launch {
            try {
                createCategory(name)
                _uiState.value = _uiState.value.copy(newCategoryName = "", errorMessage = null)
                load()
            } catch (e: InvalidCategoryInputException) {
                _uiState.value = _uiState.value.copy(errorMessage = "نام دسته‌بندی نمی‌تواند خالی باشد")
            }
        }
    }

    fun startEditing(id: UUID, currentName: String) {
        _uiState.value = _uiState.value.copy(editingCategoryId = id, editingCategoryName = currentName, errorMessage = null)
    }

    fun onEditingNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(editingCategoryName = name, errorMessage = null)
    }

    fun cancelEditing() {
        _uiState.value = _uiState.value.copy(editingCategoryId = null, editingCategoryName = "")
    }

    fun saveEditing() {
        val id = _uiState.value.editingCategoryId ?: return
        val name = _uiState.value.editingCategoryName
        viewModelScope.launch {
            try {
                updateCategory(id, name)
                _uiState.value = _uiState.value.copy(editingCategoryId = null, editingCategoryName = "", errorMessage = null)
                load()
            } catch (e: InvalidCategoryInputException) {
                _uiState.value = _uiState.value.copy(errorMessage = "نام دسته‌بندی نمی‌تواند خالی باشد")
            }
        }
    }
}
