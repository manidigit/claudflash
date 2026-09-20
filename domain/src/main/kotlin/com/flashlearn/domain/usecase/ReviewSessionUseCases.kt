package com.flashlearn.domain.usecase

import com.flashlearn.domain.exception.DataIntegrityException
import com.flashlearn.domain.model.ReviewSession
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.repository.ReviewSessionRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class StartReviewSessionUseCase @Inject constructor(
    private val reviewSessionRepository: ReviewSessionRepository
) {
    suspend operator fun invoke(
        reviewType: ReviewType,
        startedAt: Instant
    ): UUID {
        val sessionId = UUID.randomUUID()
        reviewSessionRepository.insert(
            ReviewSession(
                id = sessionId,
                startedAt = startedAt,
                endedAt = null,
                reviewType = reviewType
            )
        )
        return sessionId
    }
}

class EndReviewSessionUseCase @Inject constructor(
    private val reviewSessionRepository: ReviewSessionRepository
) {
    suspend operator fun invoke(sessionId: UUID, endedAt: Instant) {
        val session = reviewSessionRepository.getById(sessionId)
            ?: throw DataIntegrityException("ReviewSession not found: " + sessionId)

        if (session.endedAt != null) return

        require(!endedAt.isBefore(session.startedAt)) {
            "endedAt cannot be before startedAt"
        }

        reviewSessionRepository.update(session.copy(endedAt = endedAt))
    }
}