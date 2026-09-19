package com.flashlearn.app.presentation.backup

import androidx.lifecycle.ViewModel
import com.flashlearn.domain.usecase.ExportVocabularyCsvUseCase
import com.flashlearn.domain.usecase.ImportVocabularyCsvUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Closes README/tracker gap #5's CSV half (Algorithms §9 lists
 * CSV/JSON/XLSX/SQLite; JSON is [BackupViewModel], XLSX/SQLite are
 * explicitly out of scope — see the tracker). Same architecture split as
 * [BackupViewModel]: no Uri/Context here, only plain Strings in and out;
 * the Settings screen does the actual SAF file reading/writing.
 *
 * Reuses [BackupUiState] as-is rather than a near-identical duplicate —
 * both are just "isWorking / message / isError" for a file operation.
 */
@HiltViewModel
class VocabularyCsvViewModel @Inject constructor(
    private val exportVocabularyCsv: ExportVocabularyCsvUseCase,
    private val importVocabularyCsv: ImportVocabularyCsvUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    suspend fun exportCsv(): String {
        _uiState.value = BackupUiState(isWorking = true)
        val csv = exportVocabularyCsv()
        _uiState.value = BackupUiState(message = "فایل CSV آماده شد")
        return csv
    }

    suspend fun importCsv(csvText: String) {
        _uiState.value = BackupUiState(isWorking = true)
        val summary = importVocabularyCsv(csvText)
        val conflictNote = if (summary.conflicts > 0) "، ${summary.conflicts} نیازمند بررسی دستی" else ""
        _uiState.value = BackupUiState(
            message = "وارد شد: ${summary.created} جدید، ${summary.merged} ادغام‌شده، " +
                "${summary.alreadyExists} تکراری$conflictNote"
        )
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
