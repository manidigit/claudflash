package com.claudemani.data.mapper

import com.claudemani.database.entity.ConceptEntity
import com.claudemani.domain.model.Concept
import com.claudemani.domain.model.EntryType

object ConceptMapper {
    fun toDomain(e: ConceptEntity): Concept = Concept(
        id = e.id,
        entryType = EntryType.valueOf(e.entryType),
        categoryId = e.categoryId,
        favorite = e.favorite,
        active = e.active,
        createdAt = e.createdAt,
        updatedAt = e.updatedAt,
        dataVersion = e.dataVersion
    )

    fun toEntity(d: Concept): ConceptEntity = ConceptEntity(
        id = d.id,
        entryType = d.entryType.name,
        categoryId = d.categoryId,
        favorite = d.favorite,
        active = d.active,
        createdAt = d.createdAt,
        updatedAt = d.updatedAt,
        dataVersion = d.dataVersion
    )
}
