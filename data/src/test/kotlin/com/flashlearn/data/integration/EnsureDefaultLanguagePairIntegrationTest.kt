package com.flashlearn.data.integration

import com.flashlearn.domain.usecase.EnsureDefaultLanguagePairUseCase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Confirms the seed insert actually respects the real partial unique
 * index on `language_pairs` (see [com.flashlearn.database.LanguagePairActiveIndexTest])
 * — a Fake-repository test of [EnsureDefaultLanguagePairUseCase] alone
 * could never touch that constraint at all.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EnsureDefaultLanguagePairIntegrationTest {

    private lateinit var harness: RealRoomTestHarness

    @Before
    fun setUp() {
        harness = RealRoomTestHarness()
    }

    @After
    fun tearDown() {
        harness.close()
    }

    @Test
    fun `seeds exactly one real active row on a fresh database`() = runTest {
        val useCase = EnsureDefaultLanguagePairUseCase(harness.languagePairRepository)

        val pair = useCase()

        val rows = harness.roomDb.languagePairDao().getAll()
        assertEquals(1, rows.size)
        assertEquals(pair.id, rows.single().id)
        assertEquals("es", rows.single().sourceLanguage)
        assertEquals("fa", rows.single().targetLanguage)
        assertEquals(true, rows.single().isActive)
    }

    @Test
    fun `calling it again after a fresh install still only leaves one row`() = runTest {
        val useCase = EnsureDefaultLanguagePairUseCase(harness.languagePairRepository)

        val first = useCase()
        val second = useCase()

        assertEquals(first.id, second.id)
        assertEquals(1, harness.roomDb.languagePairDao().getAll().size)
    }
}
