package com.flashlearn.data.repository

import com.flashlearn.data.mapper.LanguageMapper
import com.flashlearn.data.mapper.LanguagePairMapper
import com.flashlearn.database.dao.LanguageDao
import com.flashlearn.database.dao.LanguagePairDao
import com.flashlearn.domain.model.Language
import com.flashlearn.domain.model.LanguagePair
import com.flashlearn.domain.repository.LanguagePairRepository
import com.flashlearn.domain.repository.LanguageRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageRepositoryImpl @Inject constructor(
    private val dao: LanguageDao
) : LanguageRepository {
    override suspend fun insert(language: Language) = dao.insert(LanguageMapper.toEntity(language))
    override suspend fun update(language: Language) = dao.update(LanguageMapper.toEntity(language))
    override suspend fun getById(id: UUID): Language? = dao.getById(id)?.let(LanguageMapper::toDomain)
    override suspend fun getByCode(code: String): Language? = dao.getByCode(code)?.let(LanguageMapper::toDomain)
    override suspend fun getAll(): List<Language> = dao.getAll().map(LanguageMapper::toDomain)
}

@Singleton
class LanguagePairRepositoryImpl @Inject constructor(
    private val dao: LanguagePairDao
) : LanguagePairRepository {
    override suspend fun insert(pair: LanguagePair) = dao.insert(LanguagePairMapper.toEntity(pair))
    override suspend fun update(pair: LanguagePair) = dao.update(LanguagePairMapper.toEntity(pair))
    override suspend fun getById(id: UUID): LanguagePair? = dao.getById(id)?.let(LanguagePairMapper::toDomain)
    override suspend fun getActive(): LanguagePair? = dao.getActive()?.let(LanguagePairMapper::toDomain)
    override suspend fun getAll(): List<LanguagePair> = dao.getAll().map(LanguagePairMapper::toDomain)
}
