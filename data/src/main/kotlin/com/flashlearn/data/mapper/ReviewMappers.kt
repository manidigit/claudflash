package com.flashlearn.data.mapper

import com.flashlearn.database.entity.ReviewHistoryEntity
import com.flashlearn.database.entity.ReviewSessionEntity
import com.flashlearn.domain.model.ReviewHistory
import com.flashlearn.domain.model.ReviewSession
import com.flashlearn.domain.model.ReviewType

object ReviewSessionMapper {
    fun toDomain(e: ReviewSessionEntity): ReviewSession = ReviewSession(
        id = e.id,
        startedAt = e.startedAt,
        endedAt = e.endedAt,
        reviewType = ReviewType.valueOf(e.reviewType)
    )

    fun toEntity(d: ReviewSession): ReviewSessionEntity = ReviewSessionEntity(
        id = d.id,
        startedAt = d.startedAt,
        endedAt = d.endedAt,
        reviewType = d.reviewType.name
    )
}

object ReviewHistoryMapper {
    fun toDomain(e: ReviewHistoryEntity): ReviewHistory = ReviewHistory(
        id = e.id,
        sessionId = e.sessionId,
        reviewAttemptId = e.reviewAttemptId,
        conceptId = e.conceptId,
        reviewedAt = e.reviewedAt,
        isCorrect = e.isCorrect,
        reviewType = ReviewType.valueOf(e.reviewType)
    )

    fun toEntity(d: ReviewHistory): ReviewHistoryEntity = ReviewHistoryEntity(
        id = d.id,
        sessionId = d.sessionId,
        reviewAttemptId = d.reviewAttemptId,
        conceptId = d.conceptId,
        reviewedAt = d.reviewedAt,
        isCorrect = d.isCorrect,
        reviewType = d.reviewType.name
    )
}
