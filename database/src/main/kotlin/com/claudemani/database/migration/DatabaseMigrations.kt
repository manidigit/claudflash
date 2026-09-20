package com.claudemani.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Ordered list of every schema migration ever shipped, passed as-is to
 * `Room.databaseBuilder(...).addMigrations(*ALL_MIGRATIONS)` in the Hilt
 * DatabaseModule (Phase 21).
 *
 * Currently one entry: MIGRATION_1_2 (schema version 2).
 *
 * Procedure for the next schema change (Descriptions §18.4):
 * 1. Add a new `val MIGRATION_N_M = object : Migration(N, M) { override
 *    fun migrate(db: SupportSQLiteDatabase) { db.execSQL(...) } }` below.
 * 2. Bump CLAUDEMANI_SCHEMA_VERSION to M in ClaudemaniRoomDatabase.
 * 3. Append the new Migration to [ALL_MIGRATIONS], in order.
 * 4. NEVER edit or remove a previously shipped Migration — existing
 *    installs on disk depend on that exact upgrade path executing
 *    unchanged.
 * 5. Room Schema Migration (structure) is entirely separate from
 *    RefreshDataUseCase (content) — Phase 19. Do not combine the two.
 */
/**
 * 1 → 2 (v1.4.5): data repair, no structural change. Words created by
 * v1.4.0–1.4.4 were stored as DAILY with nextReviewAt = NULL, which the
 * due query excludes — they never appeared in any review. Make them due now.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "UPDATE learning_states " +
                "SET nextReviewAt = CAST(strftime('%s','now') AS INTEGER) * 1000 " +
                "WHERE stage IN ('DAILY','WEEKLY','MONTHLY') AND nextReviewAt IS NULL"
        )
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
