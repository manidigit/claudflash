package com.claudemani.domain.usecase

import com.claudemani.domain.model.Achievement
import com.claudemani.domain.model.AchievementType
import com.claudemani.domain.repository.FakeAchievementRepository
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetAllAchievementsUseCaseTest {

    @Test
    fun `returns every AchievementType as locked when repository is empty`() = runTest {
        val useCase = GetAllAchievementsUseCase(FakeAchievementRepository())

        val result = useCase()

        assertEquals(AchievementType.entries.size, result.size)
        assertEquals(AchievementType.entries.toSet(), result.map { it.type }.toSet())
        assertTrue(result.all { !it.isUnlocked })
        assertTrue(result.all { it.unlockedAt == null })
    }

    @Test
    fun `unlocked types keep their persisted state, others stay locked`() = runTest {
        val achievements = FakeAchievementRepository()
        val unlockedAt = Instant.parse("2026-09-14T12:00:00Z")
        achievements.upsert(Achievement(AchievementType.FIRST_TEN_WORDS, isUnlocked = true, unlockedAt = unlockedAt))

        val result = GetAllAchievementsUseCase(achievements)()

        val unlockedRow = result.single { it.type == AchievementType.FIRST_TEN_WORDS }
        assertTrue(unlockedRow.isUnlocked)
        assertEquals(unlockedAt, unlockedRow.unlockedAt)

        val stillLocked = result.single { it.type == AchievementType.SEVEN_DAY_STREAK }
        assertEquals(false, stillLocked.isUnlocked)
        assertNull(stillLocked.unlockedAt)
    }
}
