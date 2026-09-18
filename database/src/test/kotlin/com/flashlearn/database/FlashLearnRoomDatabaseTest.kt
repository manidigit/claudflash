package com.flashlearn.database

import com.flashlearn.database.migration.ALL_MIGRATIONS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashLearnRoomDatabaseTest {

    @Test
    fun `schema version is 1 for this initial release`() {
        assertEquals(1, FLASHLEARN_SCHEMA_VERSION)
    }

    @Test
    fun `no migrations exist yet - version 1 is the first release`() {
        // If this ever fails because FLASHLEARN_SCHEMA_VERSION was bumped,
        // it's a reminder to also add the corresponding Migration and
        // update this test rather than silently leaving it empty.
        assertTrue(ALL_MIGRATIONS.isEmpty())
    }

    @Test
    fun `database file name is set`() {
        assertEquals("flashlearn.db", FlashLearnRoomDatabase.DATABASE_NAME)
    }
}
