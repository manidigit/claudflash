package com.flashlearn.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Creates schema objects that Room's declarative `@Entity(indices = ...)`
 * cannot express — specifically the conditional (partial) unique index
 * enforcing "at most one active LanguagePair at a time" (Descriptions
 * §19.1; decision recorded in Phase 6). Passed to
 * `Room.databaseBuilder(...).addCallback(FlashLearnDatabaseCallback)` in
 * the Hilt DatabaseModule (Phase 21).
 *
 * `onCreate` only fires once, the first time the database file is
 * created on a fresh install — that is sufficient here since schema
 * version 1 has no upgrade path yet. If a future migration recreates
 * this table, that migration must also re-create this index explicitly.
 */
object FlashLearnDatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_language_pairs_active_unique " +
                "ON language_pairs(isActive) WHERE isActive = 1"
        )
    }
}
