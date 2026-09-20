package com.claudemani.data.repository

import com.claudemani.data.mapper.DifficultyStateMapper
import com.claudemani.database.dao.DifficultyStateDao
import com.claudemani.domain.model.DifficultyState
import com.claudemani.domain.repository.DifficultyStateRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DifficultyStateRepositoryImpl @Inject constructor(
    private val dao: DifficultyStateDao
) : DifficultyStateRepository {

    override suspend fun get(conceptId: UUID): DifficultyState? =
        dao.getByConceptId(conceptId)?.let(DifficultyStateMapper::toDomain)

    override suspend fun upsert(state: DifficultyState) {
        dao.upsert(DifficultyStateMapper.toEntity(state))
    }

    override suspend fun delete(conceptId: UUID) {
        dao.delete(conceptId)
    }

    override suspend fun getAll(): List<DifficultyState> =
        dao.getAll().map(DifficultyStateMapper::toDomain)
}
