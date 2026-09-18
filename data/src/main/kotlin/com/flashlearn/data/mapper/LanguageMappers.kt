package com.flashlearn.data.mapper

import com.flashlearn.database.entity.LanguageEntity
import com.flashlearn.database.entity.LanguagePairEntity
import com.flashlearn.domain.model.Language
import com.flashlearn.domain.model.LanguagePair

object LanguageMapper {
    fun toDomain(e: LanguageEntity): Language = Language(id = e.id, code = e.code, name = e.name)
    fun toEntity(d: Language): LanguageEntity = LanguageEntity(id = d.id, code = d.code, name = d.name)
}

object LanguagePairMapper {
    fun toDomain(e: LanguagePairEntity): LanguagePair = LanguagePair(
        id = e.id,
        sourceLanguage = e.sourceLanguage,
        targetLanguage = e.targetLanguage,
        isActive = e.isActive
    )

    fun toEntity(d: LanguagePair): LanguagePairEntity = LanguagePairEntity(
        id = d.id,
        sourceLanguage = d.sourceLanguage,
        targetLanguage = d.targetLanguage,
        isActive = d.isActive
    )
}
