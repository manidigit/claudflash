package com.claudemani.data.repository

import com.claudemani.data.mapper.ReviewSessionMapper
import com.claudemani.database.dao.ReviewSessionDao
import com.claudemani.domain.model.ReviewSession
import com.claudemani.domain.repository.ReviewSessionRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewSessionRepositoryImpl @Inject constructor(
    private val dao: ReviewSessionDao
) : ReviewSessionRepository {

    override suspend fun insert(session: ReviewSession) {
        dao.insert(ReviewSessionMapper.toEntity(session))
    }

    override suspend fun update(session: ReviewSession) {
        dao.update(ReviewSessionMapper.toEntity(session))
    }

    override suspend fun getById(id: UUID): ReviewSession? =
        dao.getById(id)?.let(ReviewSessionMapper::toDomain)

    override suspend fun getAll(): List<ReviewSession> =
        dao.getAll().map(ReviewSessionMapper::toDomain)
}
