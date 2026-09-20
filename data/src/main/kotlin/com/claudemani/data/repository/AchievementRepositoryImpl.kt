package com.claudemani.data.repository

import com.claudemani.data.mapper.AchievementMapper
import com.claudemani.database.dao.AchievementDao
import com.claudemani.domain.model.Achievement
import com.claudemani.domain.model.AchievementType
import com.claudemani.domain.repository.AchievementRepository
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
