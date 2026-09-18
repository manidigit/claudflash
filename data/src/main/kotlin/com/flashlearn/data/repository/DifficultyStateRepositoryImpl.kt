package com.flashlearn.data.repository

import com.flashlearn.data.mapper.DifficultyStateMapper
import com.flashlearn.database.dao.DifficultyStateDao
import com.flashlearn.domain.model.DifficultyState
import com.flashlearn.domain.repository.DifficultyStateRepository
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
