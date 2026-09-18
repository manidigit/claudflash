package com.flashlearn.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.flashlearn.database.entity.ContentEntity
import java.util.UUID

@Dao
interface ContentDao {
    /** True UPDATE-or-INSERT via Room's native @Upsert (not delete+insert). canonicalKey must already be recomputed by the caller. */
    @Upsert
    suspend fun upsert(entity: ContentEntity)

    @Query("SELECT * FROM contents WHERE id = :id LIMIT 1")
    suspend fun getById(id: UUID): ContentEntity?

    @Query("SELECT * FROM contents WHERE conceptId = :conceptId AND languageCode = :languageCode LIMIT 1")
    suspend fun getByConceptIdAndLanguage(conceptId: UUID, languageCode: String): ContentEntity?

    @Query("SELECT * FROM contents WHERE conceptId = :conceptId")
    suspend fun getAllByConceptId(conceptId: UUID): List<ContentEntity>

    @Query("SELECT * FROM contents WHERE languageCode = :languageCode AND canonicalKey = :canonicalKey")
    suspend fun findByCanonicalKey(languageCode: String, canonicalKey: String): List<ContentEntity>

    @Query("SELECT * FROM contents WHERE languageCode = :languageCode")
    suspend fun getAllByLanguageCode(languageCode: String): List<ContentEntity>

    /** Every Content row — used by CreateBackup (Phase 17). */
    @Query("SELECT * FROM contents")
    suspend fun getAll(): List<ContentEntity>
}
