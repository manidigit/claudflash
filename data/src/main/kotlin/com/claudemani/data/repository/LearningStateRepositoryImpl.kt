package com.claudemani.data.repository

import com.claudemani.data.mapper.LearningStateMapper
import com.claudemani.database.dao.LearningStateDao
import com.claudemani.domain.model.LearningState
import com.claudemani.domain.model.Stage
import com.claudemani.domain.repository.LearningStateRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LearningStateRepositoryImpl @Inject constructor(
    private val dao: LearningStateDao
) : LearningStateRepository {

    override suspend fun get(conceptId: UUID): LearningState? =
        dao.getByConceptId(conceptId)?.let(LearningStateMapper::toDomain)

    override suspend fun upsert(state: LearningState) {
        dao.upsert(LearningStateMapper.toEntity(state))
    }

    override suspend fun getAllByStage(stage: Stage): List<LearningState> =
        dao.getAllByStage(stage.name).map(LearningStateMapper::toDomain)

    override suspend fun getDueByStage(stage: Stage, now: Instant): List<LearningState> =
        dao.getDueByStage(stage.name, now).map(LearningStateMapper::toDomain)

    override suspend fun getAllDueNonLearned(now: Instant): List<LearningState> =
        dao.getAllDueNonLearned(now).map(LearningStateMapper::toDomain)

    override suspend fun getAll(): List<LearningState> =
        dao.getAll().map(LearningStateMapper::toDomain)
}
