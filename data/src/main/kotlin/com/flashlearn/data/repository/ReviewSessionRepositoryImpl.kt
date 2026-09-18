package com.flashlearn.data.repository

import com.flashlearn.data.mapper.ReviewSessionMapper
import com.flashlearn.database.dao.ReviewSessionDao
import com.flashlearn.domain.model.ReviewSession
import com.flashlearn.domain.repository.ReviewSessionRepository
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
