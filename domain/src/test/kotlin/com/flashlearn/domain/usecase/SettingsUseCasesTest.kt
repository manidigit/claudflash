package com.flashlearn.domain.usecase

import com.flashlearn.domain.model.AppSetting
import com.flashlearn.domain.model.AppTheme
import com.flashlearn.domain.model.SettingKeys
import com.flashlearn.domain.repository.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

class SettingsUseCasesTest {

    private val now = Instant.parse("2026-09-14T12:00:00Z")

    @Test
    fun `threshold difficulty defaults to 3 when never set`() = runTest {
        val useCase = GetThresholdDifficultyUseCase(FakeSettingsRepository())
        assertEquals(3, useCase())
    }

    @Test
    fun `threshold difficulty reflects whatever is actually stored`() = runTest {
        val settings = FakeSettingsRepository()
        settings.put(AppSetting(SettingKeys.THRESHOLD_DIFFICULTY, "5", now))
        assertEquals(5, GetThresholdDifficultyUseCase(settings)())
    }

    @Test
    fun `max review cards defaults to null (no limit)`() = runTest {
        val useCase = GetMaxReviewCardsUseCase(FakeSettingsRepository())
        assertNull(useCase())
    }

    @Test
    fun `setting and reading back max review cards round-trips`() = runTest {
        val settings = FakeSettingsRepository()
        SetMaxReviewCardsUseCase(settings)(20, now)
        assertEquals(20, GetMaxReviewCardsUseCase(settings)())
    }

    @Test
    fun `clearing max review cards by passing null removes the limit`() = runTest {
        val settings = FakeSettingsRepository()
        SetMaxReviewCardsUseCase(settings)(20, now)
        SetMaxReviewCardsUseCase(settings)(null, now)
        assertNull(GetMaxReviewCardsUseCase(settings)())
    }

    @Test
    fun `setting a non-positive max review cards is rejected`() = runTest {
        val settings = FakeSettingsRepository()
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { SetMaxReviewCardsUseCase(settings)(0, now) }
        }
    }

    @Test
    fun `theme defaults to SYSTEM when never set`() = runTest {
        assertEquals(AppTheme.SYSTEM, GetThemeUseCase(FakeSettingsRepository())())
    }

    @Test
    fun `setting and reading back theme round-trips`() = runTest {
        val settings = FakeSettingsRepository()
        SetThemeUseCase(settings)(AppTheme.DARK, now)
        assertEquals(AppTheme.DARK, GetThemeUseCase(settings)())
    }
}
