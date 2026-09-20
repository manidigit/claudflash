package com.claudemani.data.repository

import com.claudemani.data.mapper.ConceptMapper
import com.claudemani.database.dao.ConceptDao
import com.claudemani.domain.model.Concept
import com.claudemani.domain.repository.ConceptRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConceptRepositoryImpl @Inject constructor(
    private val dao: ConceptDao
) : ConceptRepository {

    override suspend fun insert(concept: Concept) {
        dao.insert(ConceptMapper.toEntity(concept))
    }

    override suspend fun update(concept: Concept) {
        dao.update(ConceptMapper.toEntity(concept))
    }

    override suspend fun getById(id: UUID): Concept? =
        dao.getById(id)?.let(ConceptMapper::toDomain)

    override suspend fun findAnyById(id: UUID): Concept? =
        dao.getByIdAny(id)?.let(ConceptMapper::toDomain)

    override suspend fun getAllActive(): List<Concept> =
        dao.getAllActive().map(ConceptMapper::toDomain)

    override suspend fun getAll(): List<Concept> =
        dao.getAll().map(ConceptMapper::toDomain)

    override suspend fun softDelete(id: UUID, now: Instant) {
        dao.softDelete(id, now)
    }
}
