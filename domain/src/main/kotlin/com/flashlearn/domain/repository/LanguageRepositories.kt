package com.flashlearn.domain.repository

import com.flashlearn.domain.model.Language
import com.flashlearn.domain.model.LanguagePair
import java.util.UUID

interface LanguageRepository {
    suspend fun insert(language: Language)
    suspend fun update(language: Language)
    suspend fun getById(id: UUID): Language?
    suspend fun getByCode(code: String): Language?
    suspend fun getAll(): List<Language>
}

/** V1 has exactly one active pair at a time (isActive = true). */
interface LanguagePairRepository {
    suspend fun insert(pair: LanguagePair)
    suspend fun update(pair: LanguagePair)
    suspend fun getById(id: UUID): LanguagePair?
    suspend fun getActive(): LanguagePair?
    suspend fun getAll(): List<LanguagePair>
}
