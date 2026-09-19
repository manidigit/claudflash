package com.flashlearn.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Persists the automatic safety backup Algorithms v4.20 §9 Rule 6
 * requires before every Restore ("قبل از Restore همیشه تلاش می‌شود FULL
 * Backup از وضعیت فعلی گرفته و ذخیره شود"). Lives in `data` (not
 * `domain`) specifically so it can hold a real [Context] — this is the
 * one Android-specific piece [com.flashlearn.app.presentation.backup.BackupViewModel]
 * depends on, kept behind a plain `suspend fun save(json): Boolean` so
 * the ViewModel itself never imports anything Android-specific
 * (Descriptions §3.4 — a ViewModel's only Android/lifecycle references
 * are `ViewModel`/`viewModelScope` themselves), the same way it never
 * touches a Repository's own Room/Context internals directly either.
 *
 * Written to app-private internal storage (`filesDir`, never external/
 * shared storage) — the file the *user* explicitly exports via Settings
 * is a separate, deliberate action through Android's Storage Access
 * Framework in the Compose layer; this one is an invisible, automatic
 * safety net the user never has to manage.
 */
class SafetyBackupStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun save(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = File(context.filesDir, "safety_backups").apply { mkdirs() }
            // Timestamped, not overwritten — a failed Restore right after a
            // previous one shouldn't destroy the previous safety net.
            val file = File(dir, "safety_${Instant.now().toEpochMilli()}.json")
            file.writeText(json)
            true
        } catch (e: IOException) {
            false
        }
    }
}
