package com.claudemani.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.claudemani.database.entity.LanguagePairEntity
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Real-Room integration test (Phase 29). Everything else in this project
 * was tested against Fake in-memory repositories (fast, but they can
 * never catch a bug in actual SQL/Room wiring). This one specifically
 * targets [ClaudemaniDatabaseCallback]'s raw `CREATE UNIQUE INDEX ...
 * WHERE isActive = 1` — hand-written SQL that Room's own annotation
 * processor never validates, so a typo there would previously have gone
 * completely undetected by every test in this repo.
 *
 * Uses Robolectric instead of a true instrumented `androidTest` so it
 * runs as a normal JVM unit test (picked up by the existing CI `gradle
 * test` step, no emulator needed) — see [database/build.gradle.kts] for
 * the added test dependencies. This was the "بازبینی در فاز ۲۹" the
 * decision log at Phase ~9 deferred.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LanguagePairActiveIndexTest {

    private lateinit var db: ClaudemaniRoomDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ClaudemaniRoomDatabase::class.java
        )
            .addCallback(ClaudemaniDatabaseCallback)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun pair(isActive: Boolean) = LanguagePairEntity(
        id = UUID.randomUUID(), sourceLanguage = "es", targetLanguage = "fa", isActive = isActive
    )

    @Test
    fun `a single active language pair inserts fine`(): Unit = runBlocking {
        db.languagePairDao().insert(pair(isActive = true))
        assertEquals(1, db.languagePairDao().getAll().size)
    }

    @Test
    fun `a second active language pair is rejected by the partial unique index`(): Unit = runBlocking {
        db.languagePairDao().insert(pair(isActive = true))

        assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) {
            runBlocking { db.languagePairDao().insert(pair(isActive = true)) }
        }
    }

    @Test
    fun `multiple inactive language pairs are not restricted by the index`(): Unit = runBlocking {
        db.languagePairDao().insert(pair(isActive = false))
        db.languagePairDao().insert(pair(isActive = false))
        db.languagePairDao().insert(pair(isActive = false))

        assertEquals(3, db.languagePairDao().getAll().size)
    }
}
