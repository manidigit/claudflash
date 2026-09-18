package com.flashlearn.domain.usecase

import com.flashlearn.domain.model.ReviewHistory
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.repository.FakeReviewHistoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class CalculateStreakUseCaseTest {

    private val zone = ZoneOffset.UTC
    private val today = Instant.parse("2026-09-14T12:00:00Z") // a Monday, UTC

    private fun historyOn(daysBeforeToday: Long, hour: Int = 9): ReviewHistory =
        ReviewHistory(
            id = UUID.randomUUID(), sessionId = UUID.randomUUID(), reviewAttemptId = UUID.randomUUID(),
            conceptId = UUID.randomUUID(),
            reviewedAt = today.minusSeconds(daysBeforeToday * 86400).let {
                it.atZone(zone).toLocalDate().atTime(hour, 0).atZone(zone).toInstant()
            },
            isCorrect = true, reviewType = ReviewType.DAILY
        )

    @Test
    fun `zero when there is no review history at all`() = runTest {
        val repo = FakeReviewHistoryRepository()
        val useCase = CalculateStreakUseCase(repo)
        assertEquals(0, useCase(today, zone))
    }

    @Test
    fun `zero when the most recent review is more than one day old`() = runTest {
        val repo = FakeReviewHistoryRepository()
        repo.insert(historyOn(daysBeforeToday = 2))
        val useCase = CalculateStreakUseCase(repo)
        assertEquals(0, useCase(today, zone))
    }

    @Test
    fun `counts consecutive days ending today`() = runTest {
        val repo = FakeReviewHistoryRepository()
        repo.insert(historyOn(0))
        repo.insert(historyOn(1))
        repo.insert(historyOn(2))
        val useCase = CalculateStreakUseCase(repo)
        assertEquals(3, useCase(today, zone))
    }

    @Test
    fun `grace rule keeps yesterday's streak alive even with no review today yet`() = runTest {
        val repo = FakeReviewHistoryRepository()
        repo.insert(historyOn(1))
        repo.insert(historyOn(2))
        val useCase = CalculateStreakUseCase(repo)
        assertEquals(2, useCase(today, zone))
    }

    @Test
    fun `stops counting at the first gap`() = runTest {
        val repo = FakeReviewHistoryRepository()
        repo.insert(historyOn(0))
        repo.insert(historyOn(1))
        // gap at day 2
        repo.insert(historyOn(3))
        val useCase = CalculateStreakUseCase(repo)
        assertEquals(2, useCase(today, zone))
    }

    @Test
    fun `multiple reviews on the same day count as one`() = runTest {
        val repo = FakeReviewHistoryRepository()
        repo.insert(historyOn(0, hour = 8))
        repo.insert(historyOn(0, hour = 20))
        val useCase = CalculateStreakUseCase(repo)
        assertEquals(1, useCase(today, zone))
    }
}
