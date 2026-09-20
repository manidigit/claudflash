package com.claudemani.data.mapper

import com.claudemani.database.entity.AchievementEntity
import com.claudemani.database.entity.SettingsEntity
import com.claudemani.domain.model.Achievement
import com.claudemani.domain.model.AchievementType
import com.claudemani.domain.model.AppSetting

object SettingsMapper {
    fun toDomain(e: SettingsEntity): AppSetting = AppSetting(key = e.key, value = e.value, updatedAt = e.updatedAt)
    fun toEntity(d: AppSetting): SettingsEntity = SettingsEntity(key = d.key, value = d.value, updatedAt = d.updatedAt)
}

object AchievementMapper {
    fun toDomain(e: AchievementEntity): Achievement = Achievement(
        type = AchievementType.valueOf(e.type),
        isUnlocked = e.isUnlocked,
        unlockedAt = e.unlockedAt
    )

    fun toEntity(d: Achievement): AchievementEntity = AchievementEntity(
        type = d.type.name,
        isUnlocked = d.isUnlocked,
        unlockedAt = d.unlockedAt
    )
}
