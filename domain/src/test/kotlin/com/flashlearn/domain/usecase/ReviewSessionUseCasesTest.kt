package com.flashlearn.domain.usecase

import com.flashlearn.domain.exception.DataIntegrityException
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.repository.FakeReviewSessionRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ReviewSessionUseCasesTest {

    private val now = Instant.parse("2026-09-13T12:00:00Z")

    @Test
    fun `StartReviewSession creates a session with endedAt null`() = runTest {
        val repo = FakeReviewSessionRepository()
        val start = StartReviewSessionUseCase(repo)

        val sessionId = start(ReviewType.DAILY, now)

        val session = repo.getById(sessionId)
        assertEquals(ReviewType.DAILY, session?.reviewType)
        assertNull(session?.endedAt)
    }

    @Test
    fun `EndReviewSession sets endedAt on an existing session`() = runTest {
        val repo = FakeReviewSessionRepository()
        val start = StartReviewSessionUseCase(repo)
        val end = EndReviewSessionUseCase(repo)

        val sessionId = start(ReviewType.WEEKLY, now)
        end(sessionId, now.plusSeconds(600))

        assertEquals(now.plusSeconds(600), repo.getById(sessionId)?.endedAt)
    }

    @Test
    fun `EndReviewSession on unknown id throws DataIntegrityException`() {
        val repo = FakeReviewSessionRepository()
        val end = EndReviewSessionUseCase(repo)
        assertThrows(DataIntegrityException::class.java) {
            runBlocking { end(UUID.randomUUID(), now) }
        }
    }
}
