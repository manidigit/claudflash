package com.flashlearn.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.flashlearn.database.entity.AchievementEntity
import com.flashlearn.database.entity.SettingsEntity

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun findByKey(key: String): SettingsEntity?

    @Upsert
    suspend fun put(entity: SettingsEntity)

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun delete(key: String)

    /** Every Setting — used by CreateBackup (Phase 17). */
    @Query("SELECT * FROM settings")
    suspend fun getAll(): List<SettingsEntity>
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE type = :type LIMIT 1")
    suspend fun findByType(type: String): AchievementEntity?

    @Upsert
    suspend fun upsert(entity: AchievementEntity)

    @Query("SELECT * FROM achievements")
    suspend fun getAll(): List<AchievementEntity>
}
