package com.flashlearn.app.presentation.addword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flashlearn.domain.parser.ParseResult
import com.flashlearn.domain.parser.VocabularyParser
import com.flashlearn.domain.usecase.CreateConceptCommand
import com.flashlearn.domain.usecase.CreateConceptUseCase
import com.flashlearn.domain.usecase.ImportEntryResult
import com.flashlearn.domain.usecase.ImportParsedEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns AddWord screen state only: manual single-entry add and the Paste
 * Text bulk-import flow. Never touches a Repository directly — only the
 * two UseCases below and the pure, dependency-free [VocabularyParser].
 */
@HiltViewModel
class AddWordViewModel @Inject constructor(
    private val createConcept: CreateConceptUseCase,
    private val importParsedEntry: ImportParsedEntryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddWordUiState())
    val uiState: StateFlow<AddWordUiState> = _uiState.asStateFlow()

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

    /** CreateConceptUseCase itself enforces "at least one translation required" (§14 Edge Case #8). */
    fun saveManualEntry() {
        val state = _uiState.value
        viewModelScope.launch {
            try {
                createConcept(
                    CreateConceptCommand(
                        sourceText = state.manualSourceText.trim(),
                        targetText = state.manualTargetText.trim(),
                        notes = state.manualNotes.trim().takeIf { it.isNotEmpty() }
                    )
                )
                _uiState.value = AddWordUiState(mode = AddWordMode.MANUAL, manualSavedSuccessfully = true)
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
