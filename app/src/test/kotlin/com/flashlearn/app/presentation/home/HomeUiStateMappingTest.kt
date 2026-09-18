package com.flashlearn.app.presentation.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HomeUiStateMappingTest {

    @Test
    fun `maps every raw input field into the matching ui state field`() {
        val state = buildHomeUiState(
            totalActiveWords = 40,
            learnedWords = 10,
            streak = 5,
            progressPercentage = 62.6,
            dueDaily = 3,
            dueWeekly = 1,
            dueMonthly = 0,
            dueRandom = 4,
            learnedAvailable = 10
        )

        assertFalse(state.isLoading)
        assertEquals(40, state.totalActiveWords)
        assertEquals(10, state.learnedWords)
        assertEquals(5, state.streak)
        assertEquals(3, state.dueDaily)
        assertEquals(1, state.dueWeekly)
        assertEquals(0, state.dueMonthly)
        assertEquals(4, state.dueRandom)
        assertEquals(10, state.learnedAvailable)
    }

    @Test
    fun `progress percentage rounds up at half`() {
        val state = buildHomeUiState(
            totalActiveWords = 1, learnedWords = 0, streak = 0, progressPercentage = 62.5,
            dueDaily = 0, dueWeekly = 0, dueMonthly = 0, dueRandom = 0, learnedAvailable = 0
        )
        assertEquals(63, state.progressPercentage)
    }

    @Test
    fun `progress percentage rounds down below half`() {
        val state = buildHomeUiState(
            totalActiveWords = 1, learnedWords = 0, streak = 0, progressPercentage = 35.4,
            dueDaily = 0, dueWeekly = 0, dueMonthly = 0, dueRandom = 0, learnedAvailable = 0
        )
        assertEquals(35, state.progressPercentage)
    }

    @Test
    fun `zero percentage stays zero, not negative or NaN`() {
        val state = buildHomeUiState(
            totalActiveWords = 0, learnedWords = 0, streak = 0, progressPercentage = 0.0,
            dueDaily = 0, dueWeekly = 0, dueMonthly = 0, dueRandom = 0, learnedAvailable = 0
        )
        assertEquals(0, state.progressPercentage)
    }
}
