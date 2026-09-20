package com.claudemani.data.repository

import com.claudemani.data.mapper.ReviewHistoryMapper
import com.claudemani.database.dao.ReviewHistoryDao
import com.claudemani.domain.model.ReviewHistory
import com.claudemani.domain.repository.ReviewHistoryRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewHistoryRepositoryImpl @Inject constructor(
    private val dao: ReviewHistoryDao
) : ReviewHistoryRepository {

    override suspend fun insert(history: ReviewHistory) {
        dao.insert(ReviewHistoryMapper.toEntity(history))
    }

    override suspend fun getById(id: UUID): ReviewHistory? =
        dao.getById(id)?.let(ReviewHistoryMapper::toDomain)

    override suspend fun getByConceptId(conceptId: UUID): List<ReviewHistory> =
        dao.getByConceptId(conceptId).map(ReviewHistoryMapper::toDomain)

    override suspend fun getBySessionId(sessionId: UUID): List<ReviewHistory> =
        dao.getBySessionId(sessionId).map(ReviewHistoryMapper::toDomain)

    override suspend fun existsByAttemptId(sessionId: UUID, attemptId: UUID): Boolean =
        dao.existsByAttemptId(sessionId, attemptId)

    override suspend fun getDistinctConceptIds(): List<UUID> =
        dao.getDistinctConceptIds()

    override suspend fun getAll(): List<ReviewHistory> =
        dao.getAll().map(ReviewHistoryMapper::toDomain)
}
