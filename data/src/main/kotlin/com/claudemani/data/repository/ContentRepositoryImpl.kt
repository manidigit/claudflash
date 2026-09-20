package com.claudemani.data.repository

import com.claudemani.data.mapper.ContentMapper
import com.claudemani.database.dao.ContentDao
import com.claudemani.domain.model.Content
import com.claudemani.domain.repository.ContentRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val dao: ContentDao
) : ContentRepository {

    override suspend fun upsert(content: Content) {
        dao.upsert(ContentMapper.toEntity(content))
    }

    override suspend fun getById(id: UUID): Content? =
        dao.getById(id)?.let(ContentMapper::toDomain)

    override suspend fun getByConceptIdAndLanguage(conceptId: UUID, languageCode: String): Content? =
        dao.getByConceptIdAndLanguage(conceptId, languageCode)?.let(ContentMapper::toDomain)

    override suspend fun getAllByConceptId(conceptId: UUID): List<Content> =
        dao.getAllByConceptId(conceptId).map(ContentMapper::toDomain)

    override suspend fun findByCanonicalKey(languageCode: String, canonicalKey: String): List<Content> =
        dao.findByCanonicalKey(languageCode, canonicalKey).map(ContentMapper::toDomain)

    override suspend fun getAllByLanguageCode(languageCode: String): List<Content> =
        dao.getAllByLanguageCode(languageCode).map(ContentMapper::toDomain)

    override suspend fun getAll(): List<Content> =
        dao.getAll().map(ContentMapper::toDomain)
}
