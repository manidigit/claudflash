package com.claudemani.data.repository

import com.claudemani.data.mapper.SettingsMapper
import com.claudemani.database.dao.SettingsDao
import com.claudemani.domain.model.AppSetting
import com.claudemani.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dao: SettingsDao
) : SettingsRepository {

    override suspend fun findByKey(key: String): AppSetting? =
        dao.findByKey(key)?.let(SettingsMapper::toDomain)

    override suspend fun put(setting: AppSetting) {
        dao.put(SettingsMapper.toEntity(setting))
    }

    override suspend fun delete(key: String) {
        dao.delete(key)
    }

    override suspend fun getInt(key: String, default: Int): Int =
        findByKey(key)?.value?.toIntOrNull() ?: default

    override suspend fun getBoolean(key: String, default: Boolean): Boolean =
        findByKey(key)?.value?.toBooleanStrictOrNull() ?: default

    override suspend fun getAll(): List<AppSetting> =
        dao.getAll().map(SettingsMapper::toDomain)
}
