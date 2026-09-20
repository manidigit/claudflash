package com.claudemani.domain.repository

import com.claudemani.domain.model.Achievement
import com.claudemani.domain.model.AchievementType

/** Achievements are keyed by [AchievementType] — never by a database row ID (matters for Restore merge). */
interface AchievementRepository {
    suspend fun findByType(type: AchievementType): Achievement?
    suspend fun upsert(achievement: Achievement)
    suspend fun getAll(): List<Achievement>
}
