package com.flashlearn.domain.repository

import com.flashlearn.domain.model.AppSetting

/** Settings are keyed by [AppSetting.key] — never by a database row ID (matters for Restore merge). */
interface SettingsRepository {
    suspend fun findByKey(key: String): AppSetting?
    suspend fun put(setting: AppSetting)

    /** Removes a key entirely — used to represent "no value set" (e.g. an unset optional limit) rather than storing a sentinel. */
    suspend fun delete(key: String)

    /** Convenience typed accessor; returns [default] when the key is absent or unparsable. */
    suspend fun getInt(key: String, default: Int): Int

    /** Convenience typed accessor; returns [default] when the key is absent or unparsable. */
    suspend fun getBoolean(key: String, default: Boolean): Boolean

    /** Every Setting — used by CreateBackup (Phase 17). */
    suspend fun getAll(): List<AppSetting>
}
