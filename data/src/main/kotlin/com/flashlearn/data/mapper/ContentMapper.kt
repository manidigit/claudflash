package com.flashlearn.data.mapper

import com.flashlearn.database.entity.ContentEntity
import com.flashlearn.domain.model.Content

object ContentMapper {
    fun toDomain(e: ContentEntity): Content = Content(
        id = e.id,
        conceptId = e.conceptId,
        languageCode = e.languageCode,
        text = e.text,
        canonicalKey = e.canonicalKey,
        notes = e.notes,
        pronunciation = e.pronunciation,
        example = e.example,
        dataVersion = e.dataVersion
    )

    fun toEntity(d: Content): ContentEntity = ContentEntity(
        id = d.id,
        conceptId = d.conceptId,
        languageCode = d.languageCode,
        text = d.text,
        canonicalKey = d.canonicalKey,
        notes = d.notes,
        pronunciation = d.pronunciation,
        example = d.example,
        dataVersion = d.dataVersion
    )
}
