package com.claudemani.database

import com.claudemani.database.migration.ALL_MIGRATIONS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClaudemaniRoomDatabaseTest {

    @Test
    fun `schema version is 2`() {
        assertEquals(2, CLAUDEMANI_SCHEMA_VERSION)
    }

    @Test
    fun `migration chain reaches the current schema version`() {
        // If CLAUDEMANI_SCHEMA_VERSION is bumped again, add the Migration and update this test.
        assertEquals(1, ALL_MIGRATIONS.size)
        assertEquals(1, ALL_MIGRATIONS[0].startVersion)
        assertEquals(CLAUDEMANI_SCHEMA_VERSION, ALL_MIGRATIONS[0].endVersion)
    }

    @Test
    fun `database file name is set`() {
        assertEquals("claudemani.db", ClaudemaniRoomDatabase.DATABASE_NAME)
    }
}
