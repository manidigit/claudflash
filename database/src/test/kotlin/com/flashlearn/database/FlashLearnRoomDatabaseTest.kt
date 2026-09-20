package com.flashlearn.database

import com.flashlearn.database.migration.ALL_MIGRATIONS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashLearnRoomDatabaseTest {

    @Test
    fun `schema version is 2`() {
        assertEquals(2, FLASHLEARN_SCHEMA_VERSION)
    }

    @Test
    fun `migration chain reaches the current schema version`() {
        // If FLASHLEARN_SCHEMA_VERSION is bumped again, add the Migration and update this test.
        assertEquals(1, ALL_MIGRATIONS.size)
        assertEquals(1, ALL_MIGRATIONS[0].startVersion)
        assertEquals(FLASHLEARN_SCHEMA_VERSION, ALL_MIGRATIONS[0].endVersion)
    }

    @Test
    fun `database file name is set`() {
        assertEquals("flashlearn.db", FlashLearnRoomDatabase.DATABASE_NAME)
    }
}
