package com.flashlearn.app.presentation.progress

import com.flashlearn.domain.model.Achievement
import com.flashlearn.domain.model.AchievementType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressUiStateMappingTest {

    private val allLocked = AchievementType.entries.map {
        Achievement(type = it, isUnlocked = false, unlockedAt = null)
    }

    @Test
    fun `maps every raw input field into the matching ui state field`() {
        val state = buildProgressUiState(
            totalActiveWords = 40,
            practicedWords = 30,
            unpracticedWords = 10,
            learnedWords = 5,
            dueCount = 7,
            totalCorrect = 20,
            totalWrong = 4,
            accuracyPercentage = 83.33,
            streak = 6,
            progressPercentage = 62.6,
            achievements = allLocked,
            newlyUnlocked = emptyList()
        )

        assertFalse(state.isLoading)
        assertEquals(40, state.totalActiveWords)
        assertEquals(30, state.practicedWords)
        assertEquals(10, state.unpracticedWords)
        assertEquals(5, state.learnedWords)
        assertEquals(7, state.dueCount)
        assertEquals(20, state.totalCorrect)
        assertEquals(4, state.totalWrong)
        assertEquals(83, state.accuracyPercentage)
        assertEquals(6, state.streak)
        assertEquals(63, state.progressPercentage)
    }

    @Test
    fun `every achievement type is present exactly once regardless of input order`() {
        val state = buildProgressUiState(
            totalActiveWords = 0, practicedWords = 0, unpracticedWords = 0, learnedWords = 0,
            dueCount = 0, totalCorrect = 0, totalWrong = 0, accuracyPercentage = 0.0,
            streak = 0, progressPercentage = 0.0,
            achievements = allLocked, newlyUnlocked = emptyList()
        )

        assertEquals(AchievementType.entries.size, state.achievements.size)
        assertEquals(AchievementType.entries.toSet(), state.achievements.map { it.type }.toSet())
        assertTrue(state.achievements.all { !it.isUnlocked && it.unlockedAt == null })
    }

    @Test
    fun `unlocked achievement carries its unlock timestamp and description`() {
        val unlockedAt = Instant.parse("2026-09-17T10:00:00Z")
        val achievements = allLocked.map {
            if (it.type == AchievementType.SEVEN_DAY_STREAK) {
                Achievement(type = it.type, isUnlocked = true, unlockedAt = unlockedAt)
            } else {
                it
            }
        }

        val state = buildProgressUiState(
            totalActiveWords = 0, practicedWords = 0, unpracticedWords = 0, learnedWords = 0,
            dueCount = 0, totalCorrect = 0, totalWrong = 0, accuracyPercentage = 0.0,
            streak = 7, progressPercentage = 0.0,
            achievements = achievements, newlyUnlocked = emptyList()
        )

        val row = state.achievements.single { it.type == AchievementType.SEVEN_DAY_STREAK }
        assertTrue(row.isUnlocked)
        assertEquals(unlockedAt, row.unlockedAt)
        assertEquals("۷ روز پیوسته تمرین", row.description)
    }

    @Test
    fun `newly unlocked types are extracted from the achievements returned by this call only`() {
        val newlyUnlocked = listOf(
            Achievement(type = AchievementType.FIRST_TEN_WORDS, isUnlocked = true, unlockedAt = Instant.now())
        )

        val state = buildProgressUiState(
            totalActiveWords = 0, practicedWords = 0, unpracticedWords = 0, learnedWords = 0,
            dueCount = 0, totalCorrect = 0, totalWrong = 0, accuracyPercentage = 0.0,
            streak = 0, progressPercentage = 0.0,
            achievements = allLocked, newlyUnlocked = newlyUnlocked
        )

        assertEquals(listOf(AchievementType.FIRST_TEN_WORDS), state.newlyUnlockedTypes)
    }

    @Test
    fun `dismissing clears newly unlocked without touching the rest of the state`() {
        var state = ProgressUiState(isLoading = false, streak = 6, newlyUnlockedTypes = listOf(AchievementType.MEMORY_BUILDER))
        state = state.copy(newlyUnlockedTypes = emptyList())

        assertTrue(state.newlyUnlockedTypes.isEmpty())
        assertEquals(6, state.streak)
    }
}
