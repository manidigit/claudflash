package com.flashlearn.domain.usecase

import com.flashlearn.domain.exception.DataIntegrityException
import com.flashlearn.domain.model.ReviewSession
import com.flashlearn.domain.model.ReviewType
import com.flashlearn.domain.repository.ReviewSessionRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/** Creates a new ReviewSession and returns its id. Each session is independent — resuming a previous one is not required in V1. */
class StartReviewSessionUseCase @Inject constructor(
    private val reviewSessionRepository: ReviewSessionRepository
) {
    suspend operator fun invoke(reviewType: ReviewType, startedAt: Instant): UUID {
        val sessionId = UUID.randomUUID()
        reviewSessionRepository.insert(
            ReviewSession(id = sessionId, startedAt = startedAt, endedAt = null, reviewType = reviewType)
        )
        return sessionId
    }
}

/**
 * Marks a session as ended. Session Exit decision (V1 FROZEN): accepted
 * answers already in ReviewHistory are unaffected either way — this only
 * closes the session record itself. A session left with endedAt = null
 * (app killed mid-review) is an accepted, non-error state and is never
 * auto-closed by anything else.
 */
class EndReviewSessionUseCase @Inject constructor(
    private val reviewSessionRepository: ReviewSessionRepository
) {
    suspend operator fun invoke(sessionId: UUID, endedAt: Instant) {
        val session = reviewSessionRepository.getById(sessionId)
            ?: throw DataIntegrityException("ReviewSession not found: $sessionId")
        reviewSessionRepository.update(session.copy(endedAt = endedAt))
    }
}
