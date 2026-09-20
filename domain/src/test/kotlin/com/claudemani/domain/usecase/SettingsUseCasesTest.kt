package com.claudemani.domain.usecase

import com.claudemani.domain.model.AppSetting
import com.claudemani.domain.model.AppTheme
import com.claudemani.domain.model.QuizDifficulty
import com.claudemani.domain.model.SettingKeys
import com.claudemani.domain.repository.FakeSettingsRepository
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

    @Test
    fun `quiz difficulty defaults to MEDIUM when never set`() = runTest {
        assertEquals(QuizDifficulty.MEDIUM, GetQuizDifficultyUseCase(FakeSettingsRepository())())
    }

    @Test
    fun `setting and reading back quiz difficulty round-trips`() = runTest {
        val settings = FakeSettingsRepository()
        SetQuizDifficultyUseCase(settings)(QuizDifficulty.HARD, now)
        assertEquals(QuizDifficulty.HARD, GetQuizDifficultyUseCase(settings)())
    }

    @Test
    fun `backup encryption defaults to disabled`() = runTest {
        assertEquals(false, GetBackupEncryptionEnabledUseCase(FakeSettingsRepository())())
    }

    @Test
    fun `enabling backup encryption round-trips`() = runTest {
        val settings = FakeSettingsRepository()
        SetBackupEncryptionEnabledUseCase(settings)(true, now)
        assertEquals(true, GetBackupEncryptionEnabledUseCase(settings)())
    }
}
