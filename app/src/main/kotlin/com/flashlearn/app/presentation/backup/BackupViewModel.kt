package com.flashlearn.app.presentation.backup

import androidx.lifecycle.ViewModel
import com.flashlearn.data.backup.BackupFileFormatException
import com.flashlearn.data.backup.BackupJsonCodec
import com.flashlearn.data.backup.SafetyBackupStore
import com.flashlearn.database.FLASHLEARN_SCHEMA_VERSION
import com.flashlearn.domain.model.BackupType
import com.flashlearn.domain.usecase.CreateBackupUseCase
import com.flashlearn.domain.usecase.RestoreBackupUseCase
import com.flashlearn.domain.usecase.RestoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Closes the README/tracker gap "UI برای Backup/Restore وجود ندارد" —
 * [CreateBackupUseCase]/[RestoreBackupUseCase] have existed since Phase
 * 17 but nothing ever called them from a screen.
 *
 * Deliberately touches no Android file/Uri API itself — [exportBackup]
 * hands the finished JSON string back to the caller (the Screen, which
 * writes it to wherever the user picked via Storage Access Framework),
 * and [restoreBackup] takes the JSON string the Screen already read from
 * the file the user picked. This keeps the "ViewModel has no Android
 * reference beyond ViewModel/viewModelScope" rule (Descriptions §3.4)
 * intact — the one genuinely Android-specific piece, the automatic
 * pre-Restore safety backup, is delegated to [SafetyBackupStore] in
 * `data` instead of being inlined here.
 *
 * Format is JSON only (Algorithms §9 also lists CSV/XLSX/SQLite — see
 * the tracker for why those are out of scope here).
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val createBackup: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val safetyBackupStore: SafetyBackupStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    /** Produces the backup JSON. Writing it to wherever the user picked (via SAF) is the Screen's job. */
    suspend fun exportBackup(): String {
        _uiState.value = BackupUiState(isWorking = true)
        val data = createBackup(BackupType.FULL, FLASHLEARN_SCHEMA_VERSION, Instant.now())
        val json = BackupJsonCodec.encode(data)
        _uiState.value = BackupUiState(message = "فایل پشتیبان آماده شد")
        return json
    }

    /** [json] is raw text the Screen already read from the file the user picked via SAF. */
    suspend fun restoreBackup(json: String) {
        _uiState.value = BackupUiState(isWorking = true)
        val data = try {
            BackupJsonCodec.decode(json)
        } catch (e: BackupFileFormatException) {
            _uiState.value = BackupUiState(message = "فایل پشتیبان معتبر نیست", isError = true)
            return
        }

        val result = restoreBackupUseCase(
            data = data,
            currentSchemaVersion = FLASHLEARN_SCHEMA_VERSION,
            now = Instant.now(),
            persistSafetyBackup = { safetyData -> safetyBackupStore.save(BackupJsonCodec.encode(safetyData)) },
            // No confirmation dialog yet for "safety backup failed, continue anyway?" —
            // if the automatic safety backup itself can't be written, Restore proceeds
            // regardless rather than blocking the user; a real confirmation UI is future work.
            confirmProceedWithoutSafetyBackup = { true }
        )

        _uiState.value = when (result) {
            is RestoreResult.Success ->
                BackupUiState(message = "بازیابی موفق — ${result.newCount} مورد جدید، ${result.mergedCount} مورد ادغام‌شده")
            is RestoreResult.Error ->
                BackupUiState(message = "بازیابی ناموفق: ${result.message}", isError = true)
            RestoreResult.AbortedByUser ->
                BackupUiState(message = "بازیابی لغو شد", isError = true)
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
