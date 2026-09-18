package com.flashlearn.database.migration

import androidx.room.migration.Migration

/**
 * Ordered list of every schema migration ever shipped, passed as-is to
 * `Room.databaseBuilder(...).addMigrations(*ALL_MIGRATIONS)` in the Hilt
 * DatabaseModule (Phase 21).
 *
 * Currently empty: [com.flashlearn.database.FLASHLEARN_SCHEMA_VERSION] is
 * 1, the initial release of this from-scratch rewrite, so there is no
 * prior schema to migrate from.
 *
 * Procedure for the next schema change (Descriptions §18.4):
 * 1. Add a new `val MIGRATION_N_M = object : Migration(N, M) { override
 *    fun migrate(db: SupportSQLiteDatabase) { db.execSQL(...) } }` below.
 * 2. Bump FLASHLEARN_SCHEMA_VERSION to M in FlashLearnRoomDatabase.
 * 3. Append the new Migration to [ALL_MIGRATIONS], in order.
 * 4. NEVER edit or remove a previously shipped Migration — existing
 *    installs on disk depend on that exact upgrade path executing
 *    unchanged.
 * 5. Room Schema Migration (structure) is entirely separate from
 *    RefreshDataUseCase (content) — Phase 19. Do not combine the two.
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf()
