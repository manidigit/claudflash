package com.flashlearn.data.repository

import com.flashlearn.data.mapper.AchievementMapper
import com.flashlearn.database.dao.AchievementDao
import com.flashlearn.domain.model.Achievement
import com.flashlearn.domain.model.AchievementType
import com.flashlearn.domain.repository.AchievementRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepositoryImpl @Inject constructor(
    private val dao: AchievementDao
) : AchievementRepository {

    override suspend fun findByType(type: AchievementType): Achievement? =
        dao.findByType(type.name)?.let(AchievementMapper::toDomain)

    override suspend fun upsert(achievement: Achievement) {
        dao.upsert(AchievementMapper.toEntity(achievement))
    }

    override suspend fun getAll(): List<Achievement> =
        dao.getAll().map(AchievementMapper::toDomain)
}
