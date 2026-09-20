package com.claudemani.app.presentation.addword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.claudemani.domain.parser.ParseResult
import com.claudemani.domain.parser.VocabularyParser
import com.claudemani.domain.usecase.CreateConceptCommand
import com.claudemani.domain.usecase.CreateConceptUseCase
import com.claudemani.domain.usecase.GetAllCategoriesUseCase
import com.claudemani.domain.usecase.ImportEntryResult
import com.claudemani.domain.usecase.ImportParsedEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns AddWord screen state only: manual single-entry add and the Paste
 * Text bulk-import flow. Never touches a Repository directly — only the
 * UseCases below and the pure, dependency-free [VocabularyParser].
 */
@HiltViewModel
class AddWordViewModel @Inject constructor(
    private val createConcept: CreateConceptUseCase,
    private val importParsedEntry: ImportParsedEntryUseCase,
    private val getAllCategories: GetAllCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddWordUiState())
    val uiState: StateFlow<AddWordUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(categories = getAllCategories())
        }
    }

    fun onModeSelected(mode: AddWordMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
    }

    fun onManualSourceTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(
            manualSourceText = text, manualSaveError = null, manualSavedSuccessfully = false
        )
    }

    fun onManualTargetTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(
            manualTargetText = text, manualSaveError = null, manualSavedSuccessfully = false
        )
    }

    fun onManualNotesChanged(text: String) {
        _uiState.value = _uiState.value.copy(manualNotes = text)
    }

    /** `null` means "no category" — a perfectly valid choice, not an unset/loading state. */
    fun onCategorySelected(categoryId: UUID?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    /** CreateConceptUseCase itself enforces "at least one translation required" (§14 Edge Case #8). */
    fun saveManualEntry() {
        val state = _uiState.value
        viewModelScope.launch {
            try {
                createConcept(
                    CreateConceptCommand(
                        sourceText = state.manualSourceText.trim(),
                        targetText = state.manualTargetText.trim(),
                        categoryId = state.selectedCategoryId,
                        notes = state.manualNotes.trim().takeIf { it.isNotEmpty() }
                    )
                )
                // Keep the loaded category list and selection across a save — the user is
                // likely adding several words in a row to the same category next.
                _uiState.value = state.copy(
                    manualSourceText = "", manualTargetText = "", manualNotes = "",
                    manualSaveError = null, manualSavedSuccessfully = true
                )
            } catch (e: Exception) {
                _uiState.value = state.copy(manualSaveError = e.message ?: "Could not save")
            }
        }
    }

    fun onPasteTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(pasteText = text, parsedEntries = emptyList(), importSummary = null)
    }

    fun parsePastedText() {
        val result = VocabularyParser.parse(_uiState.value.pasteText)
        _uiState.value = applyParseResult(_uiState.value, result)
    }

    fun importParsedEntries() {
        val entries = _uiState.value.parsedEntries
        if (entries.isEmpty()) return
        _uiState.value = _uiState.value.copy(isImporting = true)
        viewModelScope.launch {
            val results = entries.map { importParsedEntry(it) }
            _uiState.value = _uiState.value.copy(
                isImporting = false,
                parsedEntries = emptyList(),
                pasteText = "",
                importSummary = summarizeImportResults(results)
            )
        }
    }
}

/** Pure — testable without a coroutine or ViewModel instance. */
internal fun applyParseResult(current: AddWordUiState, result: ParseResult): AddWordUiState =
    current.copy(
        parsedEntries = result.entries,
        commentCount = result.comments.size,
        orphanCount = result.orphanLines.size,
        duplicateGroupCount = result.duplicates.size,
        importSummary = null
    )

/** Pure — testable without a coroutine or ViewModel instance. */
internal fun summarizeImportResults(results: List<ImportEntryResult>): ImportSummary {
    var created = 0
    var merged = 0
    var alreadyExists = 0
    var conflicts = 0
    results.forEach { result ->
        when (result) {
            is ImportEntryResult.Created -> created++
            is ImportEntryResult.Merged -> merged++
            ImportEntryResult.AlreadyExists -> alreadyExists++
            is ImportEntryResult.NeedsUserChoice -> conflicts++
        }
    }
    return ImportSummary(created = created, merged = merged, alreadyExists = alreadyExists, conflicts = conflicts)
}
