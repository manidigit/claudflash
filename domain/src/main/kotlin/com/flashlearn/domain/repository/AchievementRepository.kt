package com.flashlearn.domain.repository

import com.flashlearn.domain.model.Achievement
import com.flashlearn.domain.model.AchievementType

/** Achievements are keyed by [AchievementType] — never by a database row ID (matters for Restore merge). */
interface AchievementRepository {
    suspend fun findByType(type: AchievementType): Achievement?
    suspend fun upsert(achievement: Achievement)
    suspend fun getAll(): List<Achievement>
}
