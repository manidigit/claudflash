package com.flashlearn.app.presentation.backup

import androidx.lifecycle.ViewModel
import com.flashlearn.data.backup.BackupDecryptionException
import com.flashlearn.data.backup.BackupEncryption
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
 * 17 but nothing ever called them from a screen. [BackupEncryption]
 * support (Descriptions §16.2) was added in Phase 38 — see that class's
 * KDoc for why it's PIN-derived, not Android-Keystore-backed.
 *
 * Deliberately touches no Android file/Uri API itself — [exportBackup]
 * hands the finished bytes back to the caller (the Screen, which writes
 * them to wherever the user picked via Storage Access Framework), and
 * [restoreBackup] takes the raw bytes the Screen already read from the
 * file the user picked. This keeps the "ViewModel has no Android
 * reference beyond ViewModel/viewModelScope" rule (Descriptions §3.4)
 * intact — the one genuinely Android-specific piece, the automatic
 * pre-Restore safety backup, is delegated to [SafetyBackupStore] in
 * `data` instead of being inlined here. [BackupEncryption] itself needs
 * no Android API at all (plain `javax.crypto`), so it's safe to hold
 * directly here too.
 *
 * Format is JSON only (Algorithms §9 also lists CSV — see
 * `VocabularyCsvViewModel` — and XLSX/SQLite, which are out of scope;
 * see the tracker).
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val createBackup: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val safetyBackupStore: SafetyBackupStore,
    private val backupEncryption: BackupEncryption
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    /** Pure header check — safe to call straight from the Screen without a coroutine. */
    fun isEncryptedBackup(bytes: ByteArray): Boolean = backupEncryption.isEncrypted(bytes)

    /**
     * Produces the backup file bytes. [pin] non-null means "encrypt with
     * this PIN" (the Screen only passes one when the user has Backup
     * encryption enabled in Settings); `null` means plain UTF-8 JSON.
     * Writing the result to wherever the user picked (via SAF) is the
     * Screen's job.
     */
    suspend fun exportBackup(pin: String?): ByteArray {
        _uiState.value = BackupUiState(isWorking = true)
        val data = createBackup(BackupType.FULL, FLASHLEARN_SCHEMA_VERSION, Instant.now())
        val jsonBytes = BackupJsonCodec.encode(data).toByteArray()
        val outputBytes = if (pin != null) backupEncryption.encrypt(jsonBytes, pin) else jsonBytes
        _uiState.value = BackupUiState(message = "فایل پشتیبان آماده شد")
        return outputBytes
    }

    /**
     * [bytes] is raw data the Screen already read from the file the user
     * picked via SAF. [pin] is required only if [isEncryptedBackup]
     * returned true for these bytes — the Screen is responsible for
     * asking the user for a PIN in that case before calling this.
     */
    suspend fun restoreBackup(bytes: ByteArray, pin: String?) {
        _uiState.value = BackupUiState(isWorking = true)

        val jsonBytes = if (backupEncryption.isEncrypted(bytes)) {
            if (pin == null) {
                _uiState.value = BackupUiState(message = "این فایل رمزنگاری‌شده است — PIN لازم است", isError = true)
                return
            }
            try {
                backupEncryption.decrypt(bytes, pin)
            } catch (e: BackupDecryptionException) {
                _uiState.value = BackupUiState(message = "PIN اشتباه است یا فایل خراب شده", isError = true)
                return
            }
        } else {
            bytes
        }

        val data = try {
            BackupJsonCodec.decode(String(jsonBytes))
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
